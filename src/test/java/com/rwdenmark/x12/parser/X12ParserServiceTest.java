package com.rwdenmark.x12.parser;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

import static org.assertj.core.api.Assertions.assertThat;

class X12ParserServiceTest {

    private final X12ParserService service = new X12ParserService();

    @Test
    void parses837pCommercialSample() throws Exception {
        Map<String, Object> result = service.parse(loadSample("samples/837p_commercial.edi"));

        assertThat(result.get("variant")).isEqualTo("837P");
        Map<String, Object> tx = firstTransaction(result);
        assertThat(tx.get("implementationId")).isEqualTo("005010X222A1");

        // Root HL is billing provider (level 20).
        Map<String, Object> billing = firstRoot(tx);
        assertThat(billing.get("levelCode")).isEqualTo("20");

        // Subscriber under billing provider.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subs = (List<Map<String, Object>>) billing.get("children");
        assertThat(subs).hasSize(1);
        assertThat(subs.get(0).get("levelCode")).isEqualTo("22");

        // Patient under subscriber.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pats = (List<Map<String, Object>>) subs.get(0).get("children");
        assertThat(pats).hasSize(1);
        assertThat(pats.get(0).get("levelCode")).isEqualTo("23");

        // One claim with four service lines lives on the patient HL.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claims = (List<Map<String, Object>>) pats.get(0).get("claims");
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).get("claimId")).isEqualTo("26463774");
        assertThat(claims.get(0).get("totalCharges")).isEqualTo("100");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) claims.get(0).get("serviceLines");
        assertThat(lines).hasSize(4);
    }

    @Test
    void parses837iInstitutionalSample() throws Exception {
        Map<String, Object> result = service.parse(loadSample("samples/837i_institutional.edi"));

        assertThat(result.get("variant")).isEqualTo("837I");
        Map<String, Object> tx = firstTransaction(result);
        assertThat(tx.get("implementationId")).isEqualTo("005010X223A2");

        Map<String, Object> billing = firstRoot(tx);
        assertThat(billing.get("levelCode")).isEqualTo("20");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subs = (List<Map<String, Object>>) billing.get("children");
        assertThat(subs).hasSize(1);
        Map<String, Object> sub = subs.get(0);
        assertThat(sub.get("levelCode")).isEqualTo("22");

        // 837I example 1a: patient == subscriber, so the claim sits on the subscriber HL.
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claims = (List<Map<String, Object>>) sub.get("claims");
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).get("claimId")).isEqualTo("756048Q");
        assertThat(claims.get(0).get("totalCharges")).isEqualTo("89.93");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) claims.get(0).get("serviceLines");
        assertThat(lines).hasSize(2);
    }

    @Test
    void parses837dDentalSample() throws Exception {
        Map<String, Object> result = service.parse(loadSample("samples/837d_dental.edi"));

        assertThat(result.get("variant")).isEqualTo("837D");
        Map<String, Object> tx = firstTransaction(result);
        assertThat(tx.get("implementationId")).isEqualTo("005010X224A2");

        Map<String, Object> billing = firstRoot(tx);
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> subs = (List<Map<String, Object>>) billing.get("children");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> pats = (List<Map<String, Object>>) subs.get(0).get("children");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> claims = (List<Map<String, Object>>) pats.get(0).get("claims");
        assertThat(claims).hasSize(1);
        assertThat(claims.get(0).get("claimId")).isEqualTo("26403774");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> lines = (List<Map<String, Object>>) claims.get(0).get("serviceLines");
        assertThat(lines).hasSize(2);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstTransaction(Map<String, Object> result) {
        List<Map<String, Object>> groups = (List<Map<String, Object>>) result.get("functionalGroups");
        List<Map<String, Object>> txs = (List<Map<String, Object>>) groups.get(0).get("transactions");
        return txs.get(0);
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> firstRoot(Map<String, Object> tx) {
        List<Map<String, Object>> roots = (List<Map<String, Object>>) tx.get("hierarchy");
        return roots.get(0);
    }

    private static String loadSample(String path) throws IOException {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
