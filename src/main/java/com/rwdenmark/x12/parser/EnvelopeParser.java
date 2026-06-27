package com.rwdenmark.x12.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the ISA/GS/IEA/GE outer envelope and produces a JSON-shaped map plus the
 * boundary indices of each GS..GE functional group. Transaction parsing happens
 * separately so each group can be processed in isolation.
 */
public final class EnvelopeParser {

    private EnvelopeParser() {}

    public record Envelope(Map<String, Object> isa, List<FunctionalGroup> groups) {}

    public record FunctionalGroup(Map<String, Object> gs, int firstSegmentIndex, int lastSegmentIndex) {}

    public static Envelope parse(List<Segment> segments) {
        if (segments.isEmpty() || !segments.get(0).id().equals("ISA")) {
            throw new X12ParseException("First segment must be ISA, got: "
                    + (segments.isEmpty() ? "<empty>" : segments.get(0).id()));
        }
        Map<String, Object> isa = mapIsa(segments.get(0));

        List<FunctionalGroup> groups = new ArrayList<>();
        int gsStart = -1;
        Map<String, Object> currentGs = null;
        for (int i = 1; i < segments.size(); i++) {
            Segment s = segments.get(i);
            switch (s.id()) {
                case "GS" -> {
                    if (gsStart != -1) {
                        throw new X12ParseException("Nested GS without GE at index " + i + ".");
                    }
                    currentGs = mapGs(s);
                    gsStart = i + 1;
                }
                case "GE" -> {
                    if (gsStart == -1) {
                        throw new X12ParseException("GE without matching GS at index " + i + ".");
                    }
                    groups.add(new FunctionalGroup(currentGs, gsStart, i - 1));
                    gsStart = -1;
                    currentGs = null;
                }
                case "IEA" -> { /* end of interchange */ }
                default -> { /* segment lives inside a GS..GE; transaction parser handles it */ }
            }
        }
        if (gsStart != -1) {
            throw new X12ParseException("GS opened but no GE found.");
        }
        return new Envelope(isa, groups);
    }

    private static Map<String, Object> mapIsa(Segment s) {
        Map<String, Object> isa = new LinkedHashMap<>();
        isa.put("authQualifier", s.element(0).trim());
        isa.put("securityQualifier", s.element(2).trim());
        isa.put("senderIdQualifier", s.element(4).trim());
        isa.put("senderId", s.element(5).trim());
        isa.put("receiverIdQualifier", s.element(6).trim());
        isa.put("receiverId", s.element(7).trim());
        isa.put("date", s.element(8));
        isa.put("time", s.element(9));
        isa.put("versionNumber", s.element(11));
        isa.put("controlNumber", s.element(12));
        isa.put("acknowledgmentRequested", s.element(13));
        isa.put("usageIndicator", s.element(14));
        return isa;
    }

    private static Map<String, Object> mapGs(Segment s) {
        Map<String, Object> gs = new LinkedHashMap<>();
        gs.put("functionalIdCode", s.element(0));
        gs.put("senderCode", s.element(1));
        gs.put("receiverCode", s.element(2));
        gs.put("date", s.element(3));
        gs.put("time", s.element(4));
        gs.put("controlNumber", s.element(5));
        gs.put("responsibleAgencyCode", s.element(6));
        gs.put("implementationId", s.element(7));
        return gs;
    }
}
