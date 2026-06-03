package com.rwdenmark.x12.parser;

/**
 * Element, sub-element, segment, and repetition delimiters for an X12 interchange.
 * Always read from the ISA header — never hardcoded. Different senders use different values.
 */
public record Delimiters(char element, char subElement, char segment, char repetition) {

    /** Pulls delimiters from the literal characters at fixed positions in the ISA. */
    public static Delimiters fromIsa(String raw) {
        if (raw == null || raw.length() < 106) {
            throw new X12ParseException("Input is shorter than a valid ISA segment (need 106+ chars).");
        }
        if (!raw.startsWith("ISA")) {
            throw new X12ParseException("Input does not start with ISA.");
        }
        char element = raw.charAt(3);
        char repetition = raw.charAt(82);
        char subElement = raw.charAt(104);
        char segment = raw.charAt(105);
        if (element == segment || element == subElement) {
            throw new X12ParseException("ISA element delimiter conflicts with another delimiter.");
        }
        return new Delimiters(element, subElement, segment, repetition);
    }
}
