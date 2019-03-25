package com.jeannei.app.corpus;

import com.jeannei.app.utils.JSoupClient;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class CorpusGenerator {

    private static final String URL_BASE = "";
    private static final String DOCUMENT_ID_SUFFIX = ".html";
    private static final String REGEX = "(?<![a-z])[-.]?[\\d]+([,.][\\d]+)*|[a-zà-γ\\d]+([-][a-zà-γ\\d]+)*";
    private static final String NUMBERS_TABLE_REGEX = "^\\d+\\h\\d\\h\\d+$";
    private static final Pattern NUMBERS_PATTERN = Pattern.compile(NUMBERS_TABLE_REGEX, Pattern.MULTILINE);
    private final Pattern PATTERN = Pattern.compile(REGEX, Pattern.CASE_INSENSITIVE | Pattern.UNICODE_CASE);

    public void generate(ParseOptions parseOptions, String documentDir) {
        File directory = new File(CorpusUtils.CORPUS_DIR);
        if (directory.exists()){
            return;
        }
        final File folder = new File(documentDir);
        JSoupClient jSoupClient = new JSoupClient();

        for (File file : folder.listFiles()) {
            String docId = getDocumentId(file.getPath());
            Document document;
            try {
                document = jSoupClient.parse(file, URL_BASE);
            } catch (IOException ioe) {
                System.out.println("Unable to parse document");
                continue;
            }

            final Matcher matcher = NUMBERS_PATTERN.matcher(document.text());
            String[] parts = matcher.replaceAll("").split(" ");
            StringBuilder builder = new StringBuilder();

            for (String part : parts) {
                if (part.trim().isEmpty()) {
                    continue;
                }
                switch (parseOptions) {
                    case PUNCTUATION:
                        builder.append(punctuationParser(part));
                        break;
                    case LOWERCASE:
                        builder.append(lowerCaseParser(part));
                        break;
                    default:
                        builder.append(defaultParser(part));
                }
                builder.append(" ");
            }

            try {
                CorpusUtils.saveParsedDocument(docId, builder.toString().replaceAll("\\s{2,}", " "));
            } catch (IOException ioe) {
                System.out.println("Unable to save document");
            }
        }
    }

    public void generateLuceneCorpus(String documentDir) {
        final File folder = new File(documentDir);
        JSoupClient jSoupClient = new JSoupClient();

        for (File file : folder.listFiles()) {
            String docId = getDocumentId(file.getPath());
            Document document;
            try {
                document = jSoupClient.parse(file, URL_BASE);
            } catch (IOException ioe) {
                System.out.println("Unable to parse document");
                continue;
            }

            final Matcher matcher = NUMBERS_PATTERN.matcher(document.text());
            String lightlyProcessedDocument = matcher.replaceAll("");

            try {
                CorpusUtils.saveLuceneParsedDocument(docId, lightlyProcessedDocument);
            } catch (IOException ioe) {
                System.out.println("Unable to save document");
            }
        }
    }

    public String[] parseQueryWithPunctuationParser(String queryTerm) {
        String[] parts = queryTerm.trim().replaceAll("\\s{2,}", " ").split(" ");
        for (int i = 0; i < parts.length; i++) {
            parts[i] = punctuationParser(parts[i]).trim().toLowerCase();
        }

        return parts;
    }

    private String getDocumentId(String url) {
        String[] parts = url.split("/");
        return parts[parts.length - 1].replace(DOCUMENT_ID_SUFFIX, "");
    }

    private String defaultParser(String part) {
        return punctuationParser(part).toLowerCase();
    }

    private String lowerCaseParser(String part) {
        return part.toLowerCase();
    }

    private String punctuationParser(String part) {
        StringBuilder stringBuilder = new StringBuilder();
        Matcher matcher = PATTERN.matcher(part);

        while (matcher.find()) {
            stringBuilder.append(matcher.group(0));
            stringBuilder.append(" ");
        }

        return stringBuilder.toString();
    }
}
