package com.jeannei.app.corpus;

import java.io.File;
import java.io.IOException;
import java.util.*;

public class InvertedIndex {

    public Map<String, List<Token>> create(int nGramSize, double stopWordListThreshold) {
        return  create(nGramSize, stopWordListThreshold, "", CorpusUtils.INVERTED_LIST_DIR);
    }
    public Map<String, List<Token>> create(int nGramSize, double stopWordListThreshold, String corpusDir, String dirName) {
        if (nGramSize <= 0) {
            throw new IllegalArgumentException("N-gram is size must be positive");
        }

        File[] files = corpusDir.isEmpty() ? CorpusUtils.listCorpusFiles() : CorpusUtils.listCorpusFiles(corpusDir);
        Map<String, List<Token>> invertedList = new HashMap<>();
        List<DocumentStat> documentStats = new ArrayList<>();
        for (File file : files) {
            List<String> tokenTerms;

            try {
                tokenTerms = generateNGrams(CorpusUtils.parseFile(file), nGramSize);
            } catch (IOException ioe) {
                System.out.println("Unable to read file " + file.getName());
                continue;
            }

            Map<String, Token> tokens = new HashMap<>();
            String docId = file.getName().split("\\.")[0];
            int position = 0;
            int documentLength = 0;

            for (String tokenTerm : tokenTerms) {
                Token token = tokens.getOrDefault(tokenTerm, new Token(docId, tokenTerm));
                token.addPosition(position);
                if (!tokens.containsKey(tokenTerm)) {
                    tokens.put(tokenTerm, token);
                }
                documentLength++;
                position++;
            }

            for (Token token : tokens.values()) {
                if (invertedList.containsKey(token.getToken())) {
                    List<Token> tokenList = invertedList.get(token.getToken());
                    tokenList.add(token);
                } else {
                    List<Token> tokenList = new ArrayList<>();
                    tokenList.add(token);
                    invertedList.put(token.getToken(), tokenList);
                }
            }

            documentStats.add(new DocumentStat(docId, tokens.keySet().size(), documentLength));
        }

        try {
            Map<String, CorpusTermStat> corpusTermStats = generateCorpusTermFrequency(invertedList);
            List<CorpusTermStat> corpusTermStatList = new ArrayList<>(corpusTermStats.values());
            List<TfIdf> tfIdfWeights = generateTfIdfWeights(corpusTermStatList, files.length);
            CorpusUtils.saveInvertedList(nGramSize, invertedList, dirName);
            CorpusUtils.saveInvertedListWithPositions(nGramSize, invertedList, dirName);
            CorpusUtils.saveDocumentTermCount(nGramSize, documentStats, dirName);
            CorpusUtils.saveCorpusTermStatistics(nGramSize, corpusTermStatList, dirName);
            CorpusUtils.saveCorpusDocumentStatistics(nGramSize, invertedList, dirName);
        } catch (IOException ioe) {
            System.out.println("Unable to save files");
        }

        return invertedList;
    }


    public class DocumentStat {
        private String documentId;
        private int totalTerms;
        private int documentLength;

        public DocumentStat(String documentId, int totalTerms, int documentLength) {
            this.documentId = documentId;
            this.totalTerms = totalTerms;
            this.documentLength = documentLength;
        }

        public String getDocumentId() {
            return documentId;
        }

        public int getTotalTerms() {
            return totalTerms;
        }

        public int getDocumentLength() {
            return documentLength;
        }
    }

    private List<String> generateNGrams(String document, int nGramSize) {
        String[] parts = document.split(" ");

        if (nGramSize == 1) {
            return Arrays.asList(parts);
        }

        List<String> results = new ArrayList<>();
        int index = 0;
        while (index < (parts.length - nGramSize + 1)) {
            StringBuilder stringBuilder = new StringBuilder();
            for (int i = index; i < (nGramSize + index); i++) {
                // if we're at the start don't add a space
                if (i != index) {
                    stringBuilder.append(" ");
                }
                stringBuilder.append(parts[i]);
            }

            index++;
            results.add(stringBuilder.toString());
        }

        return results;
    }

    private Map<String, CorpusTermStat> generateCorpusTermFrequency(Map<String, List<Token>> invertedList) {
        Map<String, CorpusTermStat> terms = new HashMap<>();
        for (String term : invertedList.keySet()) {
            int total = 0;
            List<Token> tokens = invertedList.get(term);
            for (Token token : tokens) {
                total += token.getFrequency();
            }
            terms.put(term, new CorpusTermStat(term, total, tokens.size()));
        }

        return terms;
    }

    private List<TfIdf> generateTfIdfWeights(List<CorpusTermStat> corpusTermStats, int totalDocuments) {
        List<TfIdf> tfIdfs = new ArrayList<>();
        for (CorpusTermStat corpusTermStat : corpusTermStats) {
            double score = Math.log(totalDocuments / corpusTermStat.documentFrequency);
            tfIdfs.add(new TfIdf(corpusTermStat.term, score));
        }

        return tfIdfs;
    }

    public class CorpusTermStat {
        String term;
        int frequency;
        int documentFrequency;

        public CorpusTermStat(String term, int frequency, int documentFrequency) {
            this.term = term;
            this.frequency = frequency;
            this.documentFrequency = documentFrequency;
        }

        public int corpusFrequency() {
            return frequency;
        }
    }

    public class TfIdf {
        String term;
        double score;

        public TfIdf(String term, double score) {
            this.term = term;
            this.score = score;
        }
    }
}
