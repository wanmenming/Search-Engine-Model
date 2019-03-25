package com.jeannei.app.retrievalModels;

public class DocumentRetrievalScore {

    private String documentId;
    private double score;

    public DocumentRetrievalScore(String documentId, double score) {
        this.documentId = documentId;
        this.score = score;
    }

    public String getDocumentId() {
        return documentId;
    }

    public double getScore() {
        return score;
    }

    public void incrementScore(double newScore) {
        score += newScore;
    }
}
