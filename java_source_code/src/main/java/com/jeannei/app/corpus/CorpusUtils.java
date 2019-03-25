package com.jeannei.app.corpus;


import java.io.*;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorpusUtils {

    public static final String LUCENE_DIR = "lucene_corpus";
    public static final String STEMMED_CORPUS = "cacm_stemmed_corpus";
    public static final String INVERTED_LIST_DIR = "inverted_lists";
    public static final String STEMMED_INVERTED_LIST_DIR = "inverted_lists_stemmed";
    public static final String CORPUS_DIR = "corpus";
    private final static String CACM_STEM_NUM_REGEX = "(\\d+)(?!.*[a-z])";
    private final static Pattern PATTERN = Pattern.compile(CACM_STEM_NUM_REGEX, Pattern.DOTALL);

    public static File[] listCorpusFiles() {
        return listCorpusFiles(CORPUS_DIR);
    }

    public static File[] listCorpusFiles(String directory) {
        File folder = new File(directory);
        return folder.listFiles();
    }

    public static String parseFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                line = br.readLine();
            }

            return sb.toString();
        }
    }

    public static String parseCacmFile(File file) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            StringBuilder sb = new StringBuilder();
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }

            return sb.toString();
        }
    }

    public static void generateStemmedCorpusDirectoryWithFiles(String path) throws IOException {
        File directory = new File(STEMMED_CORPUS);
        if (directory.exists()){
            return;
        }
        createRawCacmCorpusDirectory();
        String[] parts = parseCacmFile(new File(path)).split("#");
        for (String document : parts) {
            saveCacmStemmedDocument(document);
        }
    }

    private static void saveCacmStemmedDocument(String document) throws IOException {
        if (document.isEmpty()) {
            return;
        }

        String[] documentParts = document.split(System.lineSeparator(), 2);
        int documentId = Integer.parseInt(documentParts[0].trim());
        String fileName;
        if (documentId < 10) {
            fileName = "000" + documentId;
        } else if (documentId < 100) {
            fileName = "00" + documentId;
        } else if (documentId < 1000) {
            fileName = "0" + documentId;
        } else {
            fileName = Integer.toString(documentId);
        }

        File file = new File(STEMMED_CORPUS,  "CACM-" + fileName + ".txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        // remove extraneous digits
        Matcher matcher = PATTERN.matcher(documentParts[1]);
        bufferedWriter.write(matcher.replaceAll("").replaceAll(System.lineSeparator(), " ").trim());

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveDocumentTermCount(int nGramSize, List<InvertedIndex.DocumentStat> documentStats, String dirName) throws IOException {
        createDirectory(dirName);
        File file = new File(dirName,  "document_statistics.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("DocumentId Total_Terms Document_Length");
        bufferedWriter.newLine();

        for (InvertedIndex.DocumentStat documentStat : documentStats) {
            bufferedWriter.write(documentStat.getDocumentId() + " " + documentStat.getTotalTerms() + " " + documentStat.getDocumentLength());
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveInvertedList(int nGramSize, Map<String, List<Token>> invertedList, String dirName) throws IOException {
        createDirectory(dirName);
        File file = new File(dirName,"inverted_list_term_freqency.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("Term:Inverted_List_Length (DocId,TF)");
        bufferedWriter.newLine();

        for (String tokenString : invertedList.keySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Token> tokens = invertedList.get(tokenString);
            stringBuilder.append(tokenString + ":" + tokens.size());

            boolean isFirst = true;
            for (Token token : tokens) {
                if (isFirst) {
                    stringBuilder.append(" ");
                    isFirst = false;
                } else {
                    stringBuilder.append(", ");
                }

                stringBuilder.append("(" +token.getDocId() + "," + token.getFrequency() + ")");
            }

            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveInvertedListWithPositions(int nGramSize, Map<String, List<Token>> invertedList, String dirName) throws IOException {
        createDirectory(dirName);
        File file = new File(dirName, "inverted_list_with_positions_encoded.txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("Term:Inverted_List_Length (DocId, [Positions])");
        bufferedWriter.newLine();

        for (String tokenString : invertedList.keySet()) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Token> tokens = invertedList.get(tokenString);
            stringBuilder.append(tokenString + ":" + tokens.size());

            boolean isFirst = true;
            for (Token token : tokens) {
                if (isFirst) {
                    stringBuilder.append(" ");
                    isFirst = false;
                } else {
                    stringBuilder.append(", ");
                }

                List<Integer> encodedPositions = token.encodePositions();
                stringBuilder
                    .append("(" +token.getDocId() + ",")
                    .append(Arrays.toString(encodedPositions.toArray()))
                    .append(")");
            }

            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveParsedDocument(String title, String document) throws IOException {
        createCorpusDirectory();
        File file = new File(CORPUS_DIR, title + ".txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write(document);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static  void saveLuceneParsedDocument(String title, String document) throws IOException {
        createLuceneCorpusDirectory();
        File file = new File(LUCENE_DIR, title + ".txt");
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write(document);
        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveSearchResults(int windowSize, String query, List<String> docIds) throws IOException {
        createInvertedListDirectory();
        String filename = query.replaceAll(" ", "_") + "_query_window_size_"+ windowSize + "_bool_search_results.txt";
        File file = new File(INVERTED_LIST_DIR, filename);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("DocumentId");
        bufferedWriter.newLine();

        for (String docId : docIds) {
            bufferedWriter.write(docId);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveCorpusDocumentStatistics(int windowSize, Map<String, List<Token>> invertedList, String dirName) throws IOException {
        createDirectory(dirName);
        String filename = "term_statisitcs.txt";
        File file = new File(dirName, filename);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("Term | Doc_Ids | Document_Frequency");
        bufferedWriter.newLine();
        List<String> termsList = new ArrayList<>(invertedList.keySet());
        termsList.sort(Comparator.naturalOrder());

        for (String tokenString : termsList) {
            StringBuilder stringBuilder = new StringBuilder();
            List<Token> tokens = invertedList.get(tokenString);
            stringBuilder.append(tokenString);
            stringBuilder.append(" | ");

            boolean isFirst = true;
            for (Token token : tokens) {
                if (isFirst) {
                    stringBuilder.append(" ");
                    isFirst = false;
                } else {
                    stringBuilder.append(", ");
                }

                stringBuilder.append(token.getDocId());
            }

            stringBuilder.append(" | ");
            stringBuilder.append(tokens.size());
            bufferedWriter.write(stringBuilder.toString());
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveCorpusTermStatistics(int windowSize, List<InvertedIndex.CorpusTermStat> terms, String dirName) throws IOException {
        createDirectory(dirName);
        String filename =  "corpus_statistics.txt";
        File file = new File(dirName, filename);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("Term Term_Frequency");
        bufferedWriter.newLine();

        terms.sort((t1, t2) -> {
            if (t1.frequency < t2.frequency) {
                return 1;
            }

            if (t1.frequency > t2.frequency) {
                return -1;
            }

            return 0;
        });
        for (InvertedIndex.CorpusTermStat term : terms) {
            bufferedWriter.write(term.term + " " + term.frequency);
            bufferedWriter.newLine();
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    public static void saveStopWordList(int windowSize, List<InvertedIndex.TfIdf> tfIdfWeights, double threshold) throws IOException {
        createInvertedListDirectory();
        String filename =  windowSize + "_ngram_stop_word_list.txt";
        File file = new File(INVERTED_LIST_DIR, filename);
        BufferedWriter bufferedWriter = new BufferedWriter(new FileWriter(file));
        bufferedWriter.write("Term IDF_Score");
        bufferedWriter.newLine();

        tfIdfWeights.sort((t1, t2) -> {
            if (t1.score < t2.score) {
                return -1;
            }

            if (t1.score > t2.score) {
                return 1;
            }

            return 0;
        });

        for (InvertedIndex.TfIdf tfIdf : tfIdfWeights) {
            if (tfIdf.score <= threshold) {
                bufferedWriter.write(tfIdf.term + " " + tfIdf.score);
                bufferedWriter.newLine();
            }
        }

        bufferedWriter.flush();
        bufferedWriter.close();
    }

    private static void createCorpusDirectory() {
        createDirectory(CORPUS_DIR);
    }

    private static void createLuceneCorpusDirectory() {
        createDirectory(LUCENE_DIR);
    }

    private static void createInvertedListDirectory() {
        createDirectory(INVERTED_LIST_DIR);
    }

    private static void createRawCacmCorpusDirectory() {
        createDirectory(STEMMED_CORPUS);
    }

    private static void createDirectory(String dir) {
        File directory = new File(dir);
        if (!directory.exists()){
            directory.mkdir();
        }
    }
}
