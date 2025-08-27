package com.owl.util;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.ai.document.Document;
import org.springframework.ai.reader.ExtractedTextFormatter;
import org.springframework.core.io.Resource;

import java.io.InputStream;
import java.util.List;
import java.util.Map;

public final class PdfUtil {
    private PdfUtil() {}

    public static List<Document> readPdfAsDocuments(Resource resource,
                                                    ExtractedTextFormatter formatter,
                                                    Map<String, Object> meta) throws Exception {
        String text;
        try (InputStream in = resource.getInputStream();
             PDDocument doc = Loader.loadPDF(in.readAllBytes())) { // PDFBox 3.x API
            PDFTextStripper stripper = new PDFTextStripper();
            text = stripper.getText(doc);
        }
        if (formatter != null) {
            text = formatter.format(text);
        }
        return List.of(new Document(text, meta));
    }
}
