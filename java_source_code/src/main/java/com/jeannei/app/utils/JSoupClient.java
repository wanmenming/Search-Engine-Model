package com.jeannei.app.utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.select.Elements;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

// Constructor template for JSoupClient:
//     new JSoupClient()
// Interpretation:
//     JSoupClient for interacting with JSoup API
public class JSoupClient {
    private static String DIRECTORY_NAME = "raw_documents";

    public JSoupClient() {

    }

    public Document fetchDocument(String url) throws IOException {
        return Jsoup.connect(url).get();
    }

    public Elements getLinks(Document document) {
        return select(document, "a[href]");
    }

    public Document parse(File file, String baseUri) throws IOException {
        return parse(file, "UTF-8", baseUri);
    }

    public Document parse(File file, String charEncoding, String baseUri) throws IOException {
        return Jsoup.parse(file, charEncoding, baseUri);
    }

    public void saveDocument(Document document, String filename, String href) throws IOException {
        saveDocument(document, DIRECTORY_NAME, filename, href);
    }

    public void saveDocument(Document document, String directoryName, String fileName, String href) throws IOException {
        File directory = new File(directoryName);
        if (!directory.exists()){
            directory.mkdir();
        }

        String newFileName = fileName.endsWith(".txt") ? fileName : fileName + ".txt";
        File file = new File(directoryName, newFileName);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write(href);
        bufferedWriter.newLine();
        bufferedWriter.newLine();
        bufferedWriter.write(document.toString());
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public Elements select(Document document, String cssQuery) {
        return document.select(cssQuery);
    }
}
