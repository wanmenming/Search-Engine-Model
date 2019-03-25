package com.jeannei.app.retrievalModels;

import com.jeannei.app.corpus.CorpusGenerator;
import com.jeannei.app.corpus.CorpusUtils;
import com.jeannei.app.corpus.InvertedIndex;
import com.jeannei.app.corpus.Token;

import java.io.IOException;
import java.util.*;

public class BM25 {

    private final static double K1 = 1.2;
    private final static double K2 = 500;
    private final static double B = 0.75;
    private final static double BM_25_CONSTANT = 0.5;

    public List<DocumentRetrievalScore> query(String query, String queryId, Map<String, List<Token>> invertedIndex) throws IOException {
        return query(query, queryId, invertedIndex, QueryType.STANDARD, new TreeSet<>());
    }

    public List<DocumentRetrievalScore> query(String query, String queryId, Map<String, List<Token>> invertedIndex, QueryType queryType) throws IOException {
        return query(query, queryId, invertedIndex, queryType, new TreeSet<>());
    }

    public List<DocumentRetrievalScore> query(String query, String queryId, Map<String, List<Token>> invertedIndex, QueryType queryType, Set<String> stopWords) throws IOException {
        // key id document
        Map<String, InvertedIndex.DocumentStat> documentStatMap = RetrievalUtils.getDocumentStats(getDocumentStatisticsPath(queryType));
        Map<String, Double> queryTermFrequencies = new HashMap<>();
        Map<String, List<BM25.QueryTerm>> queryTermsMap = new HashMap<>();
        String[] queryParts = new CorpusGenerator().parseQueryWithPunctuationParser(query);
        double totalDocuments = documentStatMap.size();
        double averageDocumentLength = calculateAverageDocumentLength(documentStatMap.values());

        // calculate term frequencies and de-dupe
        for (String queryTerm : queryParts) {
            if (stopWords.contains(queryTerm)) {
                continue;
            }
            Double frequency = queryTermFrequencies.getOrDefault(queryTerm, 0.0);
            queryTermFrequencies.put(queryTerm, frequency + 1);
        }

        // get documents that contain the query terms
        for (String queryTerm : queryTermFrequencies.keySet()) {
            List<Token> tokens = invertedIndex.getOrDefault(queryTerm, new ArrayList<>());
            for (Token token : tokens) {
                List<QueryTerm> queryTerms = queryTermsMap.getOrDefault(token.getDocId(), new ArrayList<>());
                queryTerms.add(new QueryTerm(queryTerm, token.getFrequency(), tokens.size()));
                queryTermsMap.put(token.getDocId(), queryTerms);
            }
        }

        List<DocumentRetrievalScore> documentRetrievalScores = rankDocuments(queryTermsMap, documentStatMap, queryTermFrequencies, totalDocuments, averageDocumentLength);
        try {
            String filePrefix;
            String systemName;
            switch (queryType) {
                case STOP_WORDS:
                    filePrefix = "BM_25_Results_With_StopWords_For_Query_";
                    systemName = "BM25_Model_With_StopWords_CS6200";
                    break;
                case STEMMING:
                    filePrefix = "BM_25_Results_With_Stemming_For_Query_";
                    systemName = "BM25_Model_With_Stemming_CS6200";
                    break;
                default:
                    filePrefix = "BM_25_Results_For_Query_";
                    systemName = "BM25_Model_CS6200";
            }
            RetrievalUtils.saveRetrievalResults(documentRetrievalScores, filePrefix + queryId, queryId, systemName);
        } catch (IOException ioe) {
            System.out.println("Unable to save results");
        }

        return documentRetrievalScores;
    }

    public List<DocumentRetrievalScore> exactSearch(String query, String queryId, Map<String, List<Token>> invertedIndex) throws IOException {
        return proximitySearch(query, queryId, invertedIndex, 0, true);
    }

    public List<DocumentRetrievalScore> proximitySearch(String query, String queryId, Map<String, List<Token>> invertedIndex, int maxDistance, boolean checkExactMatch) throws IOException {
        List<String> documentIds = proximitySearch(query, invertedIndex, maxDistance, checkExactMatch);
        // key id document
        Map<String, InvertedIndex.DocumentStat> documentStatMap = RetrievalUtils.getDocumentStats();
        Map<String, Double> queryTermFrequencies = new HashMap<>();
        Map<String, List<BM25.QueryTerm>> queryTermsMap = new HashMap<>();
        String[] queryParts = new CorpusGenerator().parseQueryWithPunctuationParser(query);
        double totalDocuments = documentStatMap.size();
        double averageDocumentLength = calculateAverageDocumentLength(documentStatMap.values());

        // calculate term frequencies and de-dupe
        for (String queryTerm : queryParts) {
            Double frequency = queryTermFrequencies.getOrDefault(queryTerm, 0.0);
            queryTermFrequencies.put(queryTerm, frequency + 1);
        }

        // get documents that contain the query terms
        for (String queryTerm : queryTermFrequencies.keySet()) {
            List<Token> tokens = invertedIndex.getOrDefault(queryTerm, new ArrayList<>());
            for (Token token : tokens) {
                if (documentIds.contains(token.getDocId())) {
                    List<QueryTerm> queryTerms = queryTermsMap.getOrDefault(token.getDocId(), new ArrayList<>());
                    queryTerms.add(new QueryTerm(queryTerm, token.getFrequency(), tokens.size()));
                    queryTermsMap.put(token.getDocId(), queryTerms);
                }
            }
        }

        List<DocumentRetrievalScore> documentRetrievalScores = rankDocuments(queryTermsMap, documentStatMap, queryTermFrequencies, totalDocuments, averageDocumentLength);
        try {
            String filePrefix = "BM_25_Best_Match_With_Proximity_" + maxDistance + "_Results_For_Query_";
            String systemName = "BM25_Best_Match_With_Proximity_" + maxDistance + "_CS6200";

            if (checkExactMatch) {
                filePrefix = "BM_25_Exact_Search_Results_For_Query_";
                systemName = "BM25_Exact_Search_CS6200";
            }

            RetrievalUtils.saveRetrievalResults(documentRetrievalScores, filePrefix + queryId, queryId, systemName);
        } catch (IOException ioe) {
            System.out.println("Unable to save results");
        }

        return documentRetrievalScores;
    }

    private List<String> proximitySearch(String query, Map<String, List<Token>> invertedIndex, int maxDistance, boolean checkExactMatch) throws IOException {
        if (maxDistance < 0) {
            throw new IllegalArgumentException("Max distance must be a positive integer");
        }
        String[] queryParts = new CorpusGenerator().parseQueryWithPunctuationParser(query);
        Map<String, List<Token>> index = new HashMap<>();

        // 1. get all documents that contain each of the query terms in
        // the order the query term appears in
        for (String queryTerm : queryParts) {
            if (invertedIndex.containsKey(queryTerm)) {
                for (Token token : invertedIndex.get(queryTerm)) {
                    if (index.containsKey(token.getDocId())) {
                        index.get(token.getDocId()).add(token);
                    } else {
                        index.put(token.getDocId(), new ArrayList<Token>() {{ add(token); }});
                    }
                }
            }
        }

        // 2. use only the documents that contain each of the unique terms
        Map<String, List<Token>> candidateDocuments = new HashMap<>();
        for (String docId : index.keySet()) {
            List<Token> tokens = index.get(docId);
            if (checkExactMatch && tokens.size() >= queryParts.length) {
                candidateDocuments.put(docId, tokens);
            } else if (!checkExactMatch) {
                candidateDocuments.put(docId, tokens);
            }
        }

        if (queryParts.length == 1) {
            return new ArrayList<>(candidateDocuments.keySet());
        }

        List<String> results = new ArrayList<>();
        // 3. let the search begin
        for (String docId : candidateDocuments.keySet()) {
            List<Token> candidateTokens = candidateDocuments.get(docId);
            if (candidateTokens.size() == 1) {
                results.add(docId);
                continue;
            }

            List<Integer> matches = buildInitialMatches(candidateTokens.get(0), candidateTokens.get(1), maxDistance);
            for (int i = 2; i < candidateTokens.size(); i++) {
                candidateTokens.get(i).sortPositions();
                List<Integer> nextPositions = candidateTokens.get(i).getPositions();
                matches = buildMatches(matches, nextPositions, maxDistance);
            }

            if (matches.size() > 0) {
                results.add(docId);
            }
        }

        return results;
    }

    private List<Integer> buildInitialMatches(Token token1, Token token2, int maxDistance) {
        token1.sortPositions();
        token2.sortPositions();
        return buildMatches(token1.getPositions(), token2.getPositions(), maxDistance);
    }

    private List<Integer> buildMatches(List<Integer> positions, List<Integer> nextPositions, int maxDistance) {
        int posIndex = 0;
        int pos2Index = 0;

        List<Integer> matches = new ArrayList<>();
        while (posIndex < positions.size() && pos2Index < nextPositions.size()) {
            if (withinWindow(positions.get(posIndex), nextPositions.get(pos2Index), maxDistance)) {
                matches.add(Math.max(positions.get(posIndex), nextPositions.get(pos2Index)));
            }

            if (positions.get(posIndex) < nextPositions.get(pos2Index)) {
                posIndex++;
            } else if (positions.get(posIndex) > nextPositions.get(pos2Index)) {
                pos2Index++;
            } else {
                posIndex++;
                pos2Index++;
            }
        }

        return matches;
    }

    private double calculateDocumentLengthNormalizationScore(InvertedIndex.DocumentStat documentStat, double averageDocumentLength) {
        return K1 * ((1 - B) + B * documentStat.getDocumentLength() / averageDocumentLength);
    }

    private float calculateAverageDocumentLength(Collection<InvertedIndex.DocumentStat> documentStats) {
        int total = 0;
        for (InvertedIndex.DocumentStat documentStat : documentStats) {
            total += documentStat.getDocumentLength();
        }

        return total / documentStats.size();
    }

    private String getDocumentStatisticsPath(QueryType queryType) {
        switch (queryType) {
            case STEMMING:
                return CorpusUtils.STEMMED_INVERTED_LIST_DIR + "/" + "document_statistics.txt";
            default:
                return CorpusUtils.INVERTED_LIST_DIR + "/" + "document_statistics.txt";
        }
    }

    private List<DocumentRetrievalScore> rankDocuments(Map<String, List<BM25.QueryTerm>> queryTermsMap,
                                                       Map<String, InvertedIndex.DocumentStat> documentStatMap,
                                                       Map<String, Double> queryTermFrequencies,
                                                       double totalDocuments,
                                                       double averageDocumentLength) {
        List<DocumentRetrievalScore> documentRetrievalScores = new ArrayList<>();
        for (String docId : queryTermsMap.keySet()) {
            InvertedIndex.DocumentStat documentStat = documentStatMap.get(docId);
            double score = 0.0f;

            for (BM25.QueryTerm queryTerm : queryTermsMap.get(docId)) {
                double denominator = (queryTerm.documentTotal + BM_25_CONSTANT) / (totalDocuments - queryTerm.documentTotal + BM_25_CONSTANT);
                double termFrequency = queryTerm.frequency;
                double k = calculateDocumentLengthNormalizationScore(documentStat, averageDocumentLength);
                double queryTermFrequency = queryTermFrequencies.get(queryTerm.term);
                double documentTermPartialScore = (K1 + 1) * termFrequency / (termFrequency + k);
                double queryTermPartialScore = (K2 + 1) * queryTermFrequency / (queryTermFrequency + K2);
                score += Math.log(1 / denominator * documentTermPartialScore * queryTermPartialScore);
            }

            documentRetrievalScores.add(new DocumentRetrievalScore(docId,  score));
        }

        return documentRetrievalScores;
    }

    private boolean withinWindow(int pos1, int pos2, int window) {
        int result = Math.abs(pos1 - pos2) - 1;
        return pos1 < pos2 && result <= window;
    }

    public class QueryTerm {
        String term;
        int frequency;
        int documentTotal;

        public QueryTerm(String term, int frequency, int documentTotal) {
            this.term = term;
            this.frequency = frequency;
            this.documentTotal = documentTotal;
        }
    }
}
