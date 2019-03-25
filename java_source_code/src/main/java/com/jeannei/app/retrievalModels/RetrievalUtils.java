package com.jeannei.app.retrievalModels;

import com.jeannei.app.Query;
import com.jeannei.app.corpus.InvertedIndex;
import com.jeannei.app.utils.JSoupClient;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.*;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.*;

public class RetrievalUtils {

    private static final String RESULTS_DIR = "retrieval_results";

    public static Map<String, InvertedIndex.CorpusTermStat> getCorpusTermStats() throws IOException {
        Map<String, InvertedIndex.CorpusTermStat> corpusTermStatMap = new HashMap<>();
        File file = new File("inverted_lists/1_ngram_corpus_term_statistics.txt");
        BufferedReader br = new BufferedReader(new FileReader(file));
        Iterator<String> iterator = br.lines().iterator();

        boolean skippedFirst = false;
        while (iterator.hasNext()) {
            String[] parts = iterator.next().split(" ");

            if (skippedFirst) {
                String term = parts[0];
                int termFrequency = Integer.parseInt(parts[1]);
                corpusTermStatMap.put(term, new InvertedIndex().new CorpusTermStat(term, termFrequency, -1));
            } else {
                skippedFirst = true;
            }
        }

        br.close();
        return corpusTermStatMap;
    }

    public static Map<String, InvertedIndex.DocumentStat> getDocumentStats() throws IOException {
        return getDocumentStats("inverted_lists/document_statistics.txt");
    }

    public static Map<String, InvertedIndex.DocumentStat> getDocumentStats(String path) throws IOException {
        Map<String, InvertedIndex.DocumentStat> documentStatMap = new HashMap<>();
        File file = new File(path);
        BufferedReader br = new BufferedReader(new FileReader(file));
        Iterator<String> iterator = br.lines().iterator();

        boolean skippedFirst = false;
        while (iterator.hasNext()) {
            String[] parts = iterator.next().split(" ");

            if (skippedFirst) {
                String docId = parts[0];
                int tokenCount = Integer.parseInt(parts[1]);
                int totalTerms = Integer.parseInt(parts[2]);
                documentStatMap.put(docId, new InvertedIndex().new DocumentStat(docId, tokenCount, totalTerms));
            } else {
                skippedFirst = true;
            }
        }

        br.close();
        return documentStatMap;
    }

    public static void saveRetrievalResults(List<DocumentRetrievalScore> documentRetrievalScores, String prefix, String queryId, String systemName) throws IOException {
        createResultsDirectory();
        sort(documentRetrievalScores);
        File file = new File(RESULTS_DIR, prefix + ".txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("query_id Q0 doc_id rank Retrieval_Score system_name");
        bufferedWriter.newLine();

        int length = Math.min(documentRetrievalScores.size(), 100);
        for (int i = 0; i < length; i++) {
            DocumentRetrievalScore documentRetrievalScore = documentRetrievalScores.get(i);
            bufferedWriter.write(queryId + " " + "Q0" + " " + documentRetrievalScore.getDocumentId() + " " + (i + 1) + " " + documentRetrievalScore.getScore() + " " + systemName);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static List<Query> parseQueryFile(String queryFile) throws IOException {
        List<Query> results = new ArrayList<>();
        File inputFile = new File(queryFile);
        JSoupClient jSoupClient = new JSoupClient();
        Document document = jSoupClient.parse(inputFile, "");
        Elements elements = document.getElementsByTag("doc");

        for (Element element : elements) {
            Element docNo = element.getElementsByTag("docno").get(0);
            String queryId = docNo.text();
            docNo.remove();
            String query = element.text();
            results.add(new Query(queryId, query));
        }

        return results;
    }

    public static List<Query> parseStemmedQueryFile(String queryFile) throws IOException {
        List<Query> results = new ArrayList<>();
        List<String> queries = Files.readAllLines(Paths.get(queryFile), Charset.defaultCharset());

        for (int i = 0; i < queries.size(); i++) {
            results.add(new Query(Integer.toString(i + 1), queries.get(i).trim()));
        }

        return results;
    }

    public static Set<String> parseStopWordsFile(String stopWordsFile) throws IOException {
        Set<String> results = new TreeSet<>();
        File inputFile = new File(stopWordsFile);
        BufferedReader br = new BufferedReader(new FileReader(inputFile));
        Iterator<String> iterator = br.lines().iterator();

        while (iterator.hasNext()) {
            String word = iterator.next();
            results.add(word);
        }

        br.close();

        return results;
    }

    private static void createResultsDirectory() {
        File directory = new File(RESULTS_DIR);
        if (!directory.exists()){
            directory.mkdir();
        }
    }

    private static void sort(List<DocumentRetrievalScore> documentRetrievalScores) {
        documentRetrievalScores.sort((d1, d2) -> {
            if (d1.getScore() < d2.getScore()) {
                return 1;
            }

            if (d1.getScore() > d2.getScore()) {
                return -1;
            }

            return 0;
        });
    }
}
