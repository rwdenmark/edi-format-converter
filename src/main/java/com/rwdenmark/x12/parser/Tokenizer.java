package com.rwdenmark.x12.parser;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Splits a raw 837 string into a list of {@link Segment} records, using delimiters
 * pulled from the ISA header. Whitespace between segments (newlines, indentation)
 * is tolerated; segments are trimmed before splitting.
 */
public final class Tokenizer {

    private Tokenizer() {}

    public static List<Segment> tokenize(String raw, Delimiters d) {
        if (raw == null || raw.isBlank()) {
            throw new X12ParseException("Empty input.");
        }
        String[] segmentChunks = raw.split(java.util.regex.Pattern.quote(String.valueOf(d.segment())));
        List<Segment> segments = new ArrayList<>(segmentChunks.length);
        for (String chunk : segmentChunks) {
            String trimmed = chunk.strip();
            if (trimmed.isEmpty()) continue;
            segments.add(parseSegment(trimmed, d));
        }
        return segments;
    }

    private static Segment parseSegment(String raw, Delimiters d) {
        // ISA is special: positions are fixed-width, so element splitting must be tolerant
        // of trailing spaces (we preserve them as-is here — callers can trim).
        String[] parts = raw.split(java.util.regex.Pattern.quote(String.valueOf(d.element())), -1);
        String id = parts[0];
        List<String> elements = parts.length > 1
                ? new ArrayList<>(Arrays.asList(parts).subList(1, parts.length))
                : List.of();
        return new Segment(id, elements);
    }
}
