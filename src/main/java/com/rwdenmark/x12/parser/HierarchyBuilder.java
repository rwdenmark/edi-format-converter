package com.rwdenmark.x12.parser;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Walks the segments inside one ST..SE transaction set and builds a nested JSON-shaped
 * map: transaction header → HL tree → claims → service lines.
 *
 * HL hierarchy:
 *   HL01 = current ID, HL02 = parent ID (empty at root), HL03 = level code,
 *   HL04 = "has subordinate children" flag.
 * Common 837 level codes: 20 = billing provider, 22 = subscriber, 23 = patient.
 *
 * Within each HL "loop" we accumulate segments until the next HL or SE. CLM marks a
 * claim boundary; LX marks a service line boundary inside a claim.
 */
public final class HierarchyBuilder {

    private HierarchyBuilder() {}

    public static Map<String, Object> buildTransaction(List<Segment> segments, int start, int end) {
        if (!segments.get(start).id().equals("ST")) {
            throw new X12ParseException("Transaction must start with ST, got: " + segments.get(start).id());
        }
        Segment st = segments.get(start);
        if (!segments.get(end).id().equals("SE")) {
            throw new X12ParseException("Transaction must end with SE, got: " + segments.get(end).id());
        }

        Map<String, Object> tx = new LinkedHashMap<>();
        tx.put("controlNumber", st.element(1));
        tx.put("implementationId", st.element(2));

        List<Map<String, Object>> headerSegments = new ArrayList<>();
        List<HlNode> flatNodes = new ArrayList<>();
        HlNode currentHl = null;

        for (int i = start + 1; i < end; i++) {
            Segment s = segments.get(i);
            if (s.id().equals("HL")) {
                currentHl = new HlNode(s);
                flatNodes.add(currentHl);
            } else if (currentHl == null) {
                headerSegments.add(segmentMap(s));
            } else {
                currentHl.absorb(s);
            }
        }

        tx.put("header", parseHeader(headerSegments));
        tx.put("hierarchy", buildTree(flatNodes));
        return tx;
    }

    private static Map<String, Object> parseHeader(List<Map<String, Object>> headerSegments) {
        Map<String, Object> header = new LinkedHashMap<>();
        List<Map<String, Object>> submitterContacts = new ArrayList<>();
        Map<String, Object> submitter = null;
        Map<String, Object> receiver = null;

        for (Map<String, Object> seg : headerSegments) {
            String id = (String) seg.get("id");
            @SuppressWarnings("unchecked")
            List<String> elements = (List<String>) seg.get("elements");
            switch (id) {
                case "BHT" -> header.put("bht", seg);
                case "NM1" -> {
                    String entityId = elements.isEmpty() ? "" : elements.get(0);
                    if (entityId.equals("41")) {
                        submitter = new LinkedHashMap<>();
                        submitter.put("name", elements.size() > 2 ? elements.get(2) : "");
                        submitter.put("idQualifier", elements.size() > 7 ? elements.get(7) : "");
                        submitter.put("id", elements.size() > 8 ? elements.get(8) : "");
                    } else if (entityId.equals("40")) {
                        receiver = new LinkedHashMap<>();
                        receiver.put("name", elements.size() > 2 ? elements.get(2) : "");
                        receiver.put("idQualifier", elements.size() > 7 ? elements.get(7) : "");
                        receiver.put("id", elements.size() > 8 ? elements.get(8) : "");
                    }
                }
                case "PER" -> submitterContacts.add(seg);
                default -> { /* ignore other header segments for now */ }
            }
        }
        if (submitter != null) {
            submitter.put("contacts", submitterContacts);
            header.put("submitter", submitter);
        }
        if (receiver != null) {
            header.put("receiver", receiver);
        }
        return header;
    }

    private static List<Map<String, Object>> buildTree(List<HlNode> flatNodes) {
        Map<String, Map<String, Object>> idToJson = new LinkedHashMap<>();
        List<Map<String, Object>> roots = new ArrayList<>();

        for (HlNode node : flatNodes) {
            Map<String, Object> json = node.toJson();
            idToJson.put(node.id, json);
        }
        for (HlNode node : flatNodes) {
            Map<String, Object> json = idToJson.get(node.id);
            if (node.parentId == null || node.parentId.isEmpty()) {
                roots.add(json);
            } else {
                Map<String, Object> parent = idToJson.get(node.parentId);
                if (parent == null) {
                    throw new X12ParseException(
                            "HL " + node.id + " references unknown parent " + node.parentId);
                }
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> children = (List<Map<String, Object>>) parent.get("children");
                children.add(json);
            }
        }
        return roots;
    }

    private static Map<String, Object> segmentMap(Segment s) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", s.id());
        map.put("elements", s.elements());
        return map;
    }

    /** Accumulator for one HL and the segments that follow it until the next HL. */
    private static final class HlNode {
        final String id;
        final String parentId;
        final String levelCode;
        final String childCode;
        final List<Map<String, Object>> segments = new ArrayList<>();
        final List<Map<String, Object>> claims = new ArrayList<>();
        Map<String, Object> currentClaim;
        List<Map<String, Object>> currentClaimSegments;
        Map<String, Object> currentServiceLine;
        List<Map<String, Object>> currentServiceLineSegments;

        HlNode(Segment hl) {
            this.id = hl.element(0);
            this.parentId = hl.element(1);
            this.levelCode = hl.element(2);
            this.childCode = hl.element(3);
        }

        void absorb(Segment s) {
            switch (s.id()) {
                case "CLM" -> startClaim(s);
                case "LX" -> {
                    if (currentClaim == null) {
                        // LX outside a CLM — treat as HL-level segment so we don't lose it.
                        segments.add(segmentMap(s));
                    } else {
                        startServiceLine(s);
                    }
                }
                default -> {
                    if (currentServiceLineSegments != null) {
                        currentServiceLineSegments.add(segmentMap(s));
                    } else if (currentClaimSegments != null) {
                        currentClaimSegments.add(segmentMap(s));
                    } else {
                        segments.add(segmentMap(s));
                    }
                }
            }
        }

        private void startClaim(Segment clm) {
            currentClaim = new LinkedHashMap<>();
            currentClaim.put("claimId", clm.element(0));
            currentClaim.put("totalCharges", clm.element(1));
            currentClaim.put("clm", segmentMap(clm));
            currentClaimSegments = new ArrayList<>();
            currentClaim.put("segments", currentClaimSegments);
            currentClaim.put("serviceLines", new ArrayList<Map<String, Object>>());
            currentServiceLine = null;
            currentServiceLineSegments = null;
            claims.add(currentClaim);
        }

        private void startServiceLine(Segment lx) {
            currentServiceLine = new LinkedHashMap<>();
            currentServiceLine.put("lineNumber", lx.element(0));
            currentServiceLineSegments = new ArrayList<>();
            currentServiceLine.put("segments", currentServiceLineSegments);
            @SuppressWarnings("unchecked")
            List<Map<String, Object>> lines =
                    (List<Map<String, Object>>) currentClaim.get("serviceLines");
            lines.add(currentServiceLine);
        }

        Map<String, Object> toJson() {
            Map<String, Object> json = new LinkedHashMap<>();
            json.put("id", id);
            json.put("parentId", parentId.isEmpty() ? null : parentId);
            json.put("levelCode", levelCode);
            json.put("hasChildren", "1".equals(childCode));
            json.put("segments", segments);
            json.put("claims", claims);
            json.put("children", new ArrayList<Map<String, Object>>());
            return json;
        }
    }
}
