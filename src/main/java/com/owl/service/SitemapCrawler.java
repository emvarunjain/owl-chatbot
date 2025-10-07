package com.owl.service;

import com.owl.util.HtmlNormalizer;
import org.springframework.stereotype.Service;

import java.io.ByteArrayInputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import javax.xml.parsers.DocumentBuilderFactory;
import org.w3c.dom.NodeList;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
public class SitemapCrawler {

    private final HttpClient http = HttpClient.newHttpClient();

    public List<String> fetchUrls(String sitemapUrl, int max) {
        try {
            var req = HttpRequest.newBuilder(URI.create(sitemapUrl)).GET().build();
            var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
            if (resp.statusCode() >= 400) return List.of();
            var xml = resp.body();
            var dbf = DocumentBuilderFactory.newInstance();
            var db = dbf.newDocumentBuilder();
            var doc = db.parse(new ByteArrayInputStream(xml.getBytes(StandardCharsets.UTF_8)));
            NodeList locs = doc.getElementsByTagName("loc");
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < locs.getLength() && urls.size() < Math.max(1, max); i++) {
                urls.add(locs.item(i).getTextContent().trim());
            }
            return urls;
        } catch (Exception e) {
            return List.of();
        }
    }

    public String fetchHtml(String url) throws Exception {
        var req = HttpRequest.newBuilder(URI.create(url)).GET().build();
        var resp = http.send(req, HttpResponse.BodyHandlers.ofString());
        if (resp.statusCode() >= 400) throw new IllegalStateException("HTTP " + resp.statusCode());
        return resp.body();
    }

    public String normalizeHtml(String html, String url) {
        return HtmlNormalizer.extractMainText(html, url);
    }
}

