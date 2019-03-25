package com.jeannei.app;


import com.jeannei.app.corpus.*;
import com.jeannei.app.lucene.Lucene;
import com.jeannei.app.retrievalModels.BM25;
import com.jeannei.app.retrievalModels.QueryType;
import com.jeannei.app.retrievalModels.RetrievalUtils;
import org.apache.commons.cli.*;

import java.io.IOException;
import java.util.*;

// Entry into application, main class
public class IRProject {

    private static final String RAW_DOCUMENTS_DIR = "cacm";
    private static String STANDARD_QUERIES_FILE = "cacm.query.txt";
    private static String STEMMED_QUERIES_FILE = "cacm_stem.query.txt";
    private static String STOP_WORDS_FILE = "common_words";
    private static String STEMMED_CORPUS_FILE = "cacm_stem.txt";
    private static String LUCENE_INDEX = "lucene_index";
    private static String LUCENE_INDEX_WITH_STEMMING = "lucene_index_with_stemming";

    public static void main(String[] args) {
        if (args.length > 0) {
            CommandLine cmd = null;
            CommandLineParser parser = new DefaultParser();
            Options options = new Options();
            Option queryCli = new Option("q", "query", true, "query");
            Option queryIdCli = new Option("i", "id", true, "query id");
            Option typeCli = new Option("t", "type", true, "Type (bestmatch | exact| proximity)");
            Option distanceCli = new Option("p", "proximity", true, "Proximity value N");

            queryCli.setRequired(true);
            queryIdCli.setRequired(true);
            typeCli.setRequired(true);
            distanceCli.setRequired(false);
            options.addOption(queryCli);
            options.addOption(queryIdCli);
            options.addOption(typeCli);
            options.addOption(distanceCli);

            try {
                cmd = parser.parse(options, args);
            } catch (ParseException e) {
                System.out.println("Incorrect arguments passed in please see example in README.txt");
                return;
            }

            String queryId = cmd.getOptionValue("id");
            String query = cmd.getOptionValue("query");
            String type = cmd.getOptionValue("type");
            int maxDistance = cmd.hasOption("proximity") ? Integer.parseInt(cmd.getOptionValue("proximity")) : -1;

            if (queryId.isEmpty() || query.isEmpty() || type.isEmpty()) {
                System.out.println("Incorrect arguments passed in please see example in README.txt");
                return;
            }

            if (type.equalsIgnoreCase("proximity") && maxDistance == -1) {
                System.out.println("Invalid value for type proximity, value must be greater than -1. Or use exact");
                return;
            }
            extraCreditRetrieval(queryId, query, type, maxDistance);
        } else if (args.length > 4) {
            System.out.println("Too many arguments passed. Please see example in README.txt!");
        } else {
            System.out.println("No command line arguments found, doing standard retrievals");
            phase1Retrieval();
            phase1RetrievalWithStemming();
        }
    }

    private static void phase1Retrieval() {
        new CorpusGenerator().generate(ParseOptions.DEFAULT, RAW_DOCUMENTS_DIR);
        InvertedIndex invertedIndex = new InvertedIndex();
        Map<String, List<Token>> index = invertedIndex.create(1, 0.1);
        Lucene lucene = new Lucene();
        BM25 bm25 = new BM25();

        try {
            lucene.createIndex(LUCENE_INDEX, CorpusUtils.LUCENE_DIR, RAW_DOCUMENTS_DIR);
        } catch (IOException e) {
            System.out.println("Unable to create index");
        }

        List<Query> queries = new ArrayList<>();
        Set<String> stopWords = new TreeSet<>();
        try {
            queries = RetrievalUtils.parseQueryFile(STANDARD_QUERIES_FILE);
            stopWords = RetrievalUtils.parseStopWordsFile(STOP_WORDS_FILE);
        } catch (IOException e) {
            System.out.println("Unable to process queries.");
        }

        for (Query query : queries) {
            try {
                bm25.query(query.getQuery(), query.getId(), index);
                bm25.query(query.getQuery(), query.getId(), index, QueryType.STOP_WORDS, stopWords);
                lucene.search(query.getQuery(), query.getId(), LUCENE_INDEX);
                lucene.search(query.getQuery(), query.getId(), LUCENE_INDEX, QueryType.STOP_WORDS, stopWords);
                System.out.println("Completed query " + query.getId());
            } catch (IOException ioe) {
                System.out.println("Unable to fetch results");
            }
        }

        System.out.println("Complete!");
    }

    private static void phase1RetrievalWithStemming() {
        try {
            CorpusUtils.generateStemmedCorpusDirectoryWithFiles(STEMMED_CORPUS_FILE);
        } catch (IOException e) {
            e.printStackTrace();
        }
        InvertedIndex invertedIndex = new InvertedIndex();
        Map<String, List<Token>> index = invertedIndex.create(1, 0.1, CorpusUtils.STEMMED_CORPUS, CorpusUtils.STEMMED_INVERTED_LIST_DIR);
        Lucene lucene = new Lucene();
        BM25 bm25 = new BM25();

        try {
            lucene.createIndex(LUCENE_INDEX_WITH_STEMMING, CorpusUtils.STEMMED_CORPUS, RAW_DOCUMENTS_DIR);
        } catch (IOException e) {
            System.out.println("Unable to create index");
        }

        List<Query> queries = new ArrayList<>();
        try {
            queries = RetrievalUtils.parseStemmedQueryFile(STEMMED_QUERIES_FILE);
        } catch (IOException e) {
            System.out.println("Unable to process queries.");
        }

        for (Query query : queries) {
            try {
                bm25.query(query.getQuery(), query.getId(), index, QueryType.STEMMING);
                lucene.search(query.getQuery(), query.getId(), LUCENE_INDEX_WITH_STEMMING, QueryType.STEMMING);
                System.out.println("Completed query " + query.getId());
            } catch (IOException ioe) {
                System.out.println("Unable to fetch results");
            }
        }
    }

    private static void extraCreditRetrieval(String queryId, String query, String type, int maxDistance) {
        System.out.println("Doing interactive retrieval for" + type + ", output files can be found in retrieval_results dir");
        InvertedIndex invertedIndex = new InvertedIndex();
        Map<String, List<Token>> index = invertedIndex.create(1, 0.1);
        BM25 bm25 = new BM25();
        try {
            if ("exact".equalsIgnoreCase(type)) {
                bm25.exactSearch(query, queryId, index);
            } else if ("bestmatch".equalsIgnoreCase(type)) {
                bm25.query(query, queryId, index);
            } else if ("proximity".equalsIgnoreCase(type)) {
                bm25.proximitySearch(query, queryId, index, maxDistance, false);
            } else {
                System.out.println("Invalid type");
            }
        System.out.println("Complete!");
        } catch (IOException e) {
            System.out.println("Unable to perform extra credit retrieval");
        }
    }
}
