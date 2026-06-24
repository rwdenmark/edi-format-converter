package com.rwdenmark.x12.parser;

import java.util.List;

/**
 * One X12 segment. {@code id} is the segment tag (NM1, CLM, HL...). {@code elements}
 * holds elements 1..N as raw strings, empty positions stay as empty strings so element
 * numbers in TR3 references line up with list indices (element 01 = elements.get(0)).
 */
public record Segment(String id, List<String> elements) {

    public String element(int index) {
        return index < elements.size() ? elements.get(index) : "";
    }
}
