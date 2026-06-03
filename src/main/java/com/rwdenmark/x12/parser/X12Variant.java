package com.rwdenmark.x12.parser;

/**
 * The three 837 flavors. The TR3 implementation guide ID is in ST03 of every transaction
 * set and also in GS08. Either source resolves the variant.
 */
public enum X12Variant {

    PROFESSIONAL("837P", "005010X222"),
    INSTITUTIONAL("837I", "005010X223"),
    DENTAL("837D", "005010X224");

    private final String label;
    private final String tr3Prefix;

    X12Variant(String label, String tr3Prefix) {
        this.label = label;
        this.tr3Prefix = tr3Prefix;
    }

    public String label() {
        return label;
    }

    /** Resolves a variant from the TR3 ID string (ST03 or GS08), e.g. {@code 005010X222A1}. */
    public static X12Variant fromImplementationId(String id) {
        if (id == null) {
            throw new X12ParseException("Missing implementation ID (ST03 / GS08).");
        }
        String normalized = id.trim().toUpperCase();
        for (X12Variant v : values()) {
            if (normalized.startsWith(v.tr3Prefix)) return v;
        }
        throw new X12ParseException("Unrecognized 837 implementation ID: " + id);
    }
}
