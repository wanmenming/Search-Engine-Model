package com.jeannei.app.corpus;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class Token {
    private String docId;
    private String token;
    private List<Integer> positions;

    public Token(String docId, String token) {
        this.docId = docId;
        this.token = token;
        this.positions = new ArrayList<>();
    }

    public String getDocId() {
        return docId;
    }

    public String getToken() {
        return token;
    }

    public int getFrequency() {
        return positions.size();
    }

    public List<Integer> getPositions() {
        return positions;
    }

    public void addPosition(int position) {
        this.positions.add(position);
    }

    public List<Integer> encodePositions() {
        sortPositions();
        Integer[] dGapEncoding = new Integer[positions.size()];
        dGapEncoding[0] = positions.get(0);
        for (int i = positions.size() - 1; i > 0; i--) {
            dGapEncoding[i] = positions.get(i) - positions.get(i - 1);
        }

        return Arrays.asList(dGapEncoding);
    }

    public List<Integer> decodePositions(List<Integer> encodedPositions) {
        Integer[] dGapDecoding = new Integer[encodedPositions.size()];

        dGapDecoding[0] = encodedPositions.get(0);
        for (int i = 1; i < encodedPositions.size(); i++) {
            dGapDecoding[i] = encodedPositions.get(i) + dGapDecoding[i - 1];
        }

        return Arrays.asList(dGapDecoding);
    }

    public void sortPositions() {
        this.positions.sort((i1, i2) -> {
            if (i1 < i2) {
                return -1;
            }

            if (i1 > i2) {
                return 1;
            }

            return 0;
        });
    }
}
