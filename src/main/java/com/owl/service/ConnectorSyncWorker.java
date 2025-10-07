package com.owl.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.owl.model.ConnectorConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

@Component
public class ConnectorSyncWorker {
    private static final Logger log = LoggerFactory.getLogger(ConnectorSyncWorker.class);

    private final IngestionService ingestion;
    private final HttpClient http = HttpClient.newHttpClient();
    private final ObjectMapper mapper = new ObjectMapper();

    public ConnectorSyncWorker(IngestionService ingestion) { this.ingestion = ingestion; }

    public void sync(ConnectorConfig cc) {
        try {
            switch (cc.getType()) {
                case "gdrive" -> syncGDrive(cc);
                default -> log.info("No worker for connector type {}", cc.getType());
            }
        } catch (Exception e) {
            log.warn("Sync failed for connector {}: {}", cc.getId(), e.getMessage());
        }
    }

    private void syncGDrive(ConnectorConfig cc) throws Exception {
        String token = (String) cc.getConfig().get("accessToken");
        if (token == null || token.isBlank()) {
            log.warn("gdrive connector {} has no accessToken", cc.getId());
            return;
        }
        // List a few files (PDF/TXT/Docs export as text) and ingest their content as text
        String listUrl = "https://www.googleapis.com/drive/v3/files?q=trashed=false&fields=files(id,name,mimeType)&pageSize=5";
        HttpRequest listReq = HttpRequest.newBuilder(URI.create(listUrl))
                .header("Authorization", "Bearer " + token).GET().build();
        HttpResponse<String> listResp = http.send(listReq, HttpResponse.BodyHandlers.ofString());
        if (listResp.statusCode() >= 400) { log.warn("gdrive list failed: {}", listResp.statusCode()); return; }
        JsonNode files = mapper.readTree(listResp.body()).path("files");
        for (JsonNode f : files) {
            String id = f.path("id").asText(); String name = f.path("name").asText(); String mime = f.path("mimeType").asText();
            String downloadUrl;
            if (mime.startsWith("application/vnd.google-apps")) {
                // export as text if Google Doc/Sheet/Slide; default to plain text export
                downloadUrl = "https://www.googleapis.com/drive/v3/files/" + id + "/export?mimeType=text/plain";
            } else {
                downloadUrl = "https://www.googleapis.com/drive/v3/files/" + id + "?alt=media";
            }
            HttpRequest getReq = HttpRequest.newBuilder(URI.create(downloadUrl))
                    .header("Authorization", "Bearer " + token).GET().build();
            HttpResponse<String> getResp = http.send(getReq, HttpResponse.BodyHandlers.ofString());
            if (getResp.statusCode() >= 400) { log.warn("gdrive get failed for {}: {}", id, getResp.statusCode()); continue; }
            String text = getResp.body();
            ingestion.ingestText(cc.getTenantId(), name, text);
        }
    }
}

