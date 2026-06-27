package com.rwdenmark.x12.samples;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * Built-in 837 samples bundled under {@code resources/samples/}. All three are public
 * reference claims from the TR3 implementation guides, no PHI.
 */
@Component
public class SampleClaims {

    private final Map<String, Sample> samples;

    public SampleClaims() {
        this.samples = new LinkedHashMap<>();
        samples.put("837p", new Sample("837p",
                "TR3 005010X222A1 Example 01: commercial professional claim",
                "samples/837p_commercial.edi"));
        samples.put("837i", new Sample("837i",
                "TR3 005010X223A2 Example 1a: institutional outpatient lab claim",
                "samples/837i_institutional.edi"));
        samples.put("837d", new Sample("837d",
                "005010X224A2 dental claim with two procedures",
                "samples/837d_dental.edi"));
    }

    public Map<String, Sample> all() {
        return Collections.unmodifiableMap(samples);
    }

    public String content(String id) {
        Sample s = samples.get(id);
        if (s == null) {
            throw new IllegalArgumentException("Unknown sample id: " + id);
        }
        try (var in = new ClassPathResource(s.resourcePath()).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read sample " + id, e);
        }
    }

    public record Sample(String id, String description, String resourcePath) {}
}
