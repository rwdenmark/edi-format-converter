package com.rwdenmark.x12.convert;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** Supported interchange formats for the converter. */
public enum Format {
    X12, JSON, YAML, XML;

    public static Format fromString(String s) {
        if (s == null) throw bad(null);
        try {
            return Format.valueOf(s.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw bad(s);
        }
    }

    private static ResponseStatusException bad(String s) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST,
                "Unknown format: " + s + " (use x12, json, yaml, xml)");
    }
}
