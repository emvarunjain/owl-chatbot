package com.owl.util;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class HtmlNormalizer {
    private HtmlNormalizer() {}

    public static String extractMainText(String html, String baseUrl) {
        Document doc = Jsoup.parse(html, baseUrl);
        // strip scripts/styles/nav/aside/footer
        doc.select("script,style,noscript,iframe").remove();
        doc.select("nav,header,footer,aside").remove();
        // heuristic: prefer <main> or the largest <article>/<section>
        Element main = doc.selectFirst("main");
        if (main == null) {
            Elements cands = doc.select("article,section,div");
            Element best = null; int bestLen = -1;
            for (Element e : cands) {
                int len = e.text().length();
                if (len > bestLen) { best = e; bestLen = len; }
            }
            main = best != null ? best : doc.body();
        }
        String text = main.text();
        return text.replaceAll("\n{3,}", "\n\n").trim();
    }
}

