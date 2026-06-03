package com.rwdenmark.x12.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;

/**
 * Entry point for the parser. Takes raw 837 text, returns a JSON-shaped map:
 * envelope → functional groups → transactions → HL hierarchy → claims → service lines.
 */
@Service
public class X12ParserService {

    public Map<String, Object> parse(String raw) {
        Delimiters delimiters = Delimiters.fromIsa(raw);
        List<Segment> segments = Tokenizer.tokenize(raw, delimiters);
        EnvelopeParser.Envelope envelope = EnvelopeParser.parse(segments);

        List<Map<String, Object>> groups = new ArrayList<>();
        X12Variant variant = null;

        for (EnvelopeParser.FunctionalGroup g : envelope.groups()) {
            Map<String, Object> groupOut = new LinkedHashMap<>();
            groupOut.put("gs", g.gs());
            List<Map<String, Object>> transactions = new ArrayList<>();
            int i = g.firstSegmentIndex();
            while (i <= g.lastSegmentIndex()) {
                if (!segments.get(i).id().equals("ST")) {
                    i++;
                    continue;
                }
                int seIndex = findClosingSe(segments, i, g.lastSegmentIndex());
                Map<String, Object> tx = HierarchyBuilder.buildTransaction(segments, i, seIndex);
                if (variant == null) {
                    variant = X12Variant.fromImplementationId(String.valueOf(tx.get("implementationId")));
                }
                transactions.add(tx);
                i = seIndex + 1;
            }
            groupOut.put("transactions", transactions);
            groups.add(groupOut);
        }

        if (variant == null) {
            // Fall back to GS08 when no transaction yielded one.
            variant = X12Variant.fromImplementationId(
                    String.valueOf(envelope.groups().get(0).gs().get("implementationId")));
        }

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("variant", variant.label());
        Map<String, Object> envelopeOut = new LinkedHashMap<>();
        envelopeOut.put("isa", envelope.isa());
        out.put("envelope", envelopeOut);
        out.put("functionalGroups", groups);
        return out;
    }

    private int findClosingSe(List<Segment> segments, int stIndex, int boundary) {
        for (int j = stIndex + 1; j <= boundary; j++) {
            if (segments.get(j).id().equals("SE")) return j;
        }
        throw new X12ParseException("ST at index " + stIndex + " has no matching SE.");
    }
}
