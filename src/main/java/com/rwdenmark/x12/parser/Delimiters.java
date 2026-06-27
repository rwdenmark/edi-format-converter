package com.rwdenmark.x12.parser;

/**
 * Element, sub-element, segment, and repetition delimiters for an X12 interchange.
 * Always read from the ISA header, never hardcoded. Different senders use different values.
 */
public record Delimiters(char element, char subElement, char segment, char repetition) {

    private static final int ISA_ELEMENT_COUNT = 16;

    /**
     * Reads the delimiters from the ISA structurally rather than from fixed byte offsets, so a
     * compact ISA (no space padding) parses the same as a fixed-width 106-char one. The element
     * separator is the character right after "ISA". Walking that separator across the 16 ISA
     * elements locates ISA11 (the repetition separator) and ISA16 (the sub-element separator);
     * the character immediately after ISA16 is the segment terminator.
     */
    public static Delimiters fromIsa(String raw) {
        String isa = raw == null ? "" : raw.stripLeading();
        if (isa.length() < 4 || !isa.startsWith("ISA")) {
            throw new X12ParseException("Input does not start with an ISA segment.");
        }
        char element = isa.charAt(3);

        int[] separatorAt = new int[ISA_ELEMENT_COUNT + 1];
        int found = 0;
        for (int i = 3; i < isa.length() && found < ISA_ELEMENT_COUNT; i++) {
            if (isa.charAt(i) == element) {
                separatorAt[++found] = i;
            }
        }
        if (found < ISA_ELEMENT_COUNT) {
            throw new X12ParseException(
                    "ISA segment is incomplete (expected " + ISA_ELEMENT_COUNT + " elements).");
        }

        int subElementIndex = separatorAt[ISA_ELEMENT_COUNT] + 1;
        if (subElementIndex + 1 >= isa.length()) {
            throw new X12ParseException(
                    "ISA segment is missing its sub-element separator or segment terminator.");
        }
        char subElement = isa.charAt(subElementIndex);
        char segment = isa.charAt(subElementIndex + 1);
        char repetition = isa.charAt(separatorAt[11] + 1);

        if (element == segment || element == subElement) {
            throw new X12ParseException("ISA element delimiter conflicts with another delimiter.");
        }
        return new Delimiters(element, subElement, segment, repetition);
    }
}
