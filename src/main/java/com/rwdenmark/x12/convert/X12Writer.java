package com.rwdenmark.x12.convert;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/**
 * Serializes a parsed X12 model (the Map produced by X12ParserService) back into a valid 837
 * interchange. Output is well-formed and re-parseable, but not byte-identical to a source file:
 * the fixed-width ISA padding, the control counts (SE/GE/IEA), and a few elements the parser
 * does not retain are regenerated rather than echoed. Segments are written one per line.
 */
final class X12Writer {

    private static final char ELEMENT = '*';
    private static final char SUBELEMENT = ':';
    private static final char SEGMENT = '~';
    private static final char REPETITION = '^';

    private X12Writer() {}

    static String write(Object model) {
        Map<String, Object> root = asMap(model, "root object");
        Map<String, Object> envelope = asMap(root.get("envelope"), "envelope");
        Map<String, Object> isa = asMap(envelope.get("isa"), "envelope.isa");
        List<Object> groups = asList(root.get("functionalGroups"), "functionalGroups");

        StringBuilder out = new StringBuilder();
        String isaControl = lpad(str(isa.get("controlNumber")), 9, '0');
        out.append(isaSegment(isa, isaControl));
        for (Object g : groups) {
            Map<String, Object> group = asMap(g, "functionalGroup");
            Map<String, Object> gs = asMap(group.get("gs"), "gs");
            String gsControl = str(gs.get("controlNumber"));
            out.append(gsSegment(gs));
            List<Object> txs = asList(group.get("transactions"), "transactions");
            for (Object t : txs) {
                out.append(transaction(asMap(t, "transaction")));
            }
            out.append(seg("GE", String.valueOf(txs.size()), gsControl));
        }
        out.append(seg("IEA", String.valueOf(groups.size()), isaControl));
        return out.toString();
    }

    private static String transaction(Map<String, Object> tx) {
        List<String> segs = new ArrayList<>();
        String control = str(tx.get("controlNumber"));
        segs.add(seg("ST", "837", control, str(tx.get("implementationId"))));

        Map<String, Object> header = asMap(tx.get("header"), "transaction.header");
        if (header.get("bht") instanceof Map) {
            segs.add(rawSeg(asMap(header.get("bht"), "bht")));
        }
        if (header.get("submitter") instanceof Map) {
            Map<String, Object> s = asMap(header.get("submitter"), "submitter");
            segs.add(nm1("41", s));
            for (Object c : asListOrEmpty(s.get("contacts"))) {
                segs.add(rawSeg(asMap(c, "contact")));
            }
        }
        if (header.get("receiver") instanceof Map) {
            segs.add(nm1("40", asMap(header.get("receiver"), "receiver")));
        }
        for (Object hl : asListOrEmpty(tx.get("hierarchy"))) {
            writeHl(asMap(hl, "hl"), segs);
        }
        segs.add(seg("SE", String.valueOf(segs.size() + 1), control));
        return String.join("", segs);
    }

    private static void writeHl(Map<String, Object> hl, List<String> segs) {
        String parentId = hl.get("parentId") == null ? "" : str(hl.get("parentId"));
        segs.add(seg("HL", str(hl.get("id")), parentId, str(hl.get("levelCode")), childCode(hl.get("hasChildren"))));
        for (Object s : asListOrEmpty(hl.get("segments"))) {
            segs.add(rawSeg(asMap(s, "segment")));
        }
        for (Object c : asListOrEmpty(hl.get("claims"))) {
            writeClaim(asMap(c, "claim"), segs);
        }
        for (Object child : asListOrEmpty(hl.get("children"))) {
            writeHl(asMap(child, "hl"), segs);
        }
    }

    private static void writeClaim(Map<String, Object> claim, List<String> segs) {
        if (claim.get("clm") instanceof Map) {
            segs.add(rawSeg(asMap(claim.get("clm"), "clm")));
        }
        for (Object s : asListOrEmpty(claim.get("segments"))) {
            segs.add(rawSeg(asMap(s, "segment")));
        }
        for (Object sl : asListOrEmpty(claim.get("serviceLines"))) {
            Map<String, Object> line = asMap(sl, "serviceLine");
            segs.add(seg("LX", str(line.get("lineNumber"))));
            for (Object s : asListOrEmpty(line.get("segments"))) {
                segs.add(rawSeg(asMap(s, "segment")));
            }
        }
    }

    private static String nm1(String entityCode, Map<String, Object> party) {
        // NM102 (entity type) and NM104-07 are not retained by the parser; default to a
        // non-person organization so the segment stays structurally valid.
        return seg("NM1", entityCode, "2", str(party.get("name")), "", "", "", "",
                str(party.get("idQualifier")), str(party.get("id")));
    }

    private static String isaSegment(Map<String, Object> isa, String isaControl) {
        StringBuilder b = new StringBuilder("ISA");
        appendEl(b, fix(str(isa.get("authQualifier")), 2));
        appendEl(b, fix("", 10));
        appendEl(b, fix(str(isa.get("securityQualifier")), 2));
        appendEl(b, fix("", 10));
        appendEl(b, fix(str(isa.get("senderIdQualifier")), 2));
        appendEl(b, fix(str(isa.get("senderId")), 15));
        appendEl(b, fix(str(isa.get("receiverIdQualifier")), 2));
        appendEl(b, fix(str(isa.get("receiverId")), 15));
        appendEl(b, fix(str(isa.get("date")), 6));
        appendEl(b, fix(str(isa.get("time")), 4));
        appendEl(b, String.valueOf(REPETITION));
        appendEl(b, fix(str(isa.get("versionNumber")), 5));
        appendEl(b, isaControl);
        appendEl(b, fix(str(isa.get("acknowledgmentRequested")), 1));
        appendEl(b, fix(str(isa.get("usageIndicator")), 1));
        appendEl(b, String.valueOf(SUBELEMENT));
        b.append(SEGMENT).append('\n');
        return b.toString();
    }

    private static String gsSegment(Map<String, Object> gs) {
        return seg("GS", str(gs.get("functionalIdCode")), str(gs.get("senderCode")),
                str(gs.get("receiverCode")), str(gs.get("date")), str(gs.get("time")),
                str(gs.get("controlNumber")), str(gs.get("responsibleAgencyCode")),
                str(gs.get("implementationId")));
    }

    private static String rawSeg(Map<String, Object> s) {
        List<Object> els = asListOrEmpty(s.get("elements"));
        String[] arr = new String[els.size()];
        for (int i = 0; i < els.size(); i++) {
            arr[i] = str(els.get(i));
        }
        return seg(str(s.get("id")), arr);
    }

    private static String seg(String id, String... els) {
        StringBuilder b = new StringBuilder(id);
        for (String e : els) {
            b.append(ELEMENT).append(e == null ? "" : e);
        }
        b.append(SEGMENT).append('\n');
        return b.toString();
    }

    private static void appendEl(StringBuilder b, String value) {
        b.append(ELEMENT).append(value);
    }

    private static String childCode(Object hasChildren) {
        boolean has = Boolean.TRUE.equals(hasChildren)
                || "true".equalsIgnoreCase(String.valueOf(hasChildren))
                || "1".equals(String.valueOf(hasChildren));
        return has ? "1" : "0";
    }

    private static String fix(String v, int width) {
        if (v == null) v = "";
        if (v.length() > width) return v.substring(0, width);
        return v + " ".repeat(width - v.length());
    }

    private static String lpad(String v, int width, char pad) {
        if (v == null) v = "";
        if (v.length() >= width) return v.substring(v.length() - width);
        return String.valueOf(pad).repeat(width - v.length()) + v;
    }

    private static String str(Object o) {
        return o == null ? "" : String.valueOf(o);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> asMap(Object o, String what) {
        if (o instanceof Map) return (Map<String, Object>) o;
        throw badModel("expected an object at " + what);
    }

    // XML has no native arrays, so a single repeated element round-trips as one object rather
    // than a one-item list (directly, or after an XML to JSON hop). Coerce a lone object into a
    // singleton list so those sources still write correctly.
    @SuppressWarnings("unchecked")
    private static List<Object> asList(Object o, String what) {
        if (o instanceof List) return (List<Object>) o;
        if (o == null) throw badModel("missing list at " + what);
        return List.of(o);
    }

    @SuppressWarnings("unchecked")
    private static List<Object> asListOrEmpty(Object o) {
        if (o instanceof List) return (List<Object>) o;
        return o == null ? List.of() : List.of(o);
    }

    private static ResponseStatusException badModel(String why) {
        return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                "Cannot write X12: input is not a parsed X12 model (" + why + ").");
    }
}
