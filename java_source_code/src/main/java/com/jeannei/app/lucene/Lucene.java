package com.jeannei.app.lucene;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import com.jeannei.app.corpus.CorpusGenerator;
import com.jeannei.app.retrievalModels.DocumentRetrievalScore;
import com.jeannei.app.retrievalModels.QueryType;
import com.jeannei.app.retrievalModels.RetrievalUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StringField;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

/**
 * To create Apache Lucene index in a folder and add files into this index based
 * on the input of the user.
 */
public class Lucene {
    private static Analyzer analyzer = new StandardAnalyzer();
    private IndexWriter writer;
    private ArrayList<File> queue = new ArrayList<File>();

    public Lucene() {

    }

    /**
     * Constructor
     *
     * @param indexDir
     *            the name of the folder in which the index should be created
     * @throws java.io.IOException
     *             when exception creating index.
     */
    private Lucene(String indexDir) throws IOException {
        FSDirectory dir = FSDirectory.open(Paths.get(indexDir));
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        config.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        writer = new IndexWriter(dir, config);
    }

    public void createIndex(String indexDir, String path, String documentDir) throws IOException {
        File directory = new File(indexDir);
        if (directory.exists()){
            return;
        }

        // remove html tags and random numbers at the end of the document
        new CorpusGenerator().generateLuceneCorpus(documentDir);
        Lucene indexer = new Lucene(indexDir);
        indexer.indexFileOrDirectory(path);
        // ===================================================
        // after adding, we always have to call the
        // closeIndex, otherwise the index is not created
        // ===================================================
        indexer.closeIndex();
    }

    public List<DocumentRetrievalScore> search(String query, String queryId, String indexDir) throws IOException {
        return search(query, queryId, indexDir, QueryType.STANDARD, new TreeSet<String>());
    }

    public List<DocumentRetrievalScore> search(String query, String queryId, String indexDir, QueryType queryType) throws IOException {
        return search(query, queryId, indexDir, queryType, new TreeSet<>());
    }

    public List<DocumentRetrievalScore> search(String query, String queryId, String indexDir, QueryType queryType, Set<String> stopWords) throws IOException {

        // =========================================================
        // Now search
        // =========================================================
        IndexReader reader = DirectoryReader.open(FSDirectory.open(Paths.get(indexDir)));
        IndexSearcher searcher = new IndexSearcher(reader);
        TopScoreDocCollector collector = TopScoreDocCollector.create(100);
        List<DocumentRetrievalScore> documentRetrievalScores = new ArrayList<>();

        try {
            Query q;
            if (stopWords.isEmpty()) {
                q = new QueryParser("contents", analyzer).parse(QueryParser.escape(query));
            } else {
                CharArraySet customStopWords = CharArraySet.unmodifiableSet(CharArraySet.copy(stopWords));
                q = new QueryParser("contents", new StandardAnalyzer(customStopWords)).parse(QueryParser.escape(query));
            }
            // need to escape the query
            searcher.search(q, collector);
            ScoreDoc[] hits = collector.topDocs().scoreDocs;

            // 4. display results
            for (int i = 0; i < hits.length; ++i) {
                int docId = hits[i].doc;
                Document d = searcher.doc(docId);
                String documentId = getDocumentId(d.get("path"));
                documentRetrievalScores.add(new DocumentRetrievalScore(documentId, hits[i].score));
            }

        } catch (Exception e) {
            System.out.println("Error searching " + query + " : " + e.getMessage());
        }

        try {
            String filePrefix;
            String systemName;
            switch (queryType) {
                case STOP_WORDS:
                    filePrefix = "Lucene_Results_For_Query_With_StopWords_For_Query_";
                    systemName = "Lucene_Standard_Analyzer_With_StopWords_CS6200";
                    break;
                case STEMMING:
                    filePrefix = "Lucene_Results_For_Query_With_Stemming_For_Query_";
                    systemName = "Lucene_Standard_Analyzer_With_Stemming_CS6200";
                    break;
                default:
                    filePrefix = "Lucene_Results_For_Query_";
                    systemName = "Lucene_Standard_Analyzer_CS6200";
            }
            RetrievalUtils.saveRetrievalResults(documentRetrievalScores, filePrefix + queryId, queryId, systemName);
        } catch (IOException ioe) {
            System.out.println("Unable to save results");
        }

        return documentRetrievalScores;
    }

    /**
     * Indexes a file or directory
     *
     * @param fileName
     *            the name of a text file or a folder we wish to add to the
     *            index
     * @throws java.io.IOException
     *             when exception
     */
    public void indexFileOrDirectory(String fileName) throws IOException {
        // ===================================================
        // gets the list of files in a folder (if user has submitted
        // the name of a folder) or gets a single file name (is user
        // has submitted only the file name)
        // ===================================================
        addFiles(new File(fileName));

        int originalNumDocs = writer.numDocs();
        for (File f : queue) {
            FileReader fr = null;
            try {
                Document doc = new Document();

                // ===================================================
                // add contents of file
                // ===================================================
                fr = new FileReader(f);
                doc.add(new TextField("contents", fr));
                doc.add(new StringField("path", f.getPath(), Field.Store.YES));
                doc.add(new StringField("filename", f.getName(),
                        Field.Store.YES));

                writer.addDocument(doc);
                System.out.println("Added: " + f);
            } catch (Exception e) {
                System.out.println("Could not add: " + f);
            } finally {
                fr.close();
            }
        }

        int newNumDocs = writer.numDocs();
        System.out.println("");
        System.out.println("************************");
        System.out
                .println((newNumDocs - originalNumDocs) + " documents added.");
        System.out.println("************************");

        queue.clear();
    }

    private void addFiles(File file) {

        if (!file.exists()) {
            System.out.println(file + " does not exist.");
        }
        if (file.isDirectory()) {
            for (File f : file.listFiles()) {
                addFiles(f);
            }
        } else {
            String filename = file.getName().toLowerCase();
            // ===================================================
            // Only index text files
            // ===================================================
            if (filename.endsWith(".htm") || filename.endsWith(".html")
                    || filename.endsWith(".xml") || filename.endsWith(".txt")) {
                queue.add(file);
            } else {
                System.out.println("Skipped " + filename);
            }
        }
    }

    /**
     * Close the index.
     *
     * @throws java.io.IOException
     *             when exception closing
     */
    public void closeIndex() throws IOException {
        writer.close();
    }

    private String getDocumentId(String path) {
        String[] parts = path.split("/");
        return parts[parts.length - 1].replace(".txt", "");
    }
}