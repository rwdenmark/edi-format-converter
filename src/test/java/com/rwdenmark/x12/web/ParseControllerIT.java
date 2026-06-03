package com.rwdenmark.x12.web;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ParseControllerIT {

    @Autowired MockMvc mockMvc;

    @Test
    void parseReturnsJsonForPostedClaim() throws Exception {
        mockMvc.perform(post("/api/parse")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(loadSample("samples/837p_commercial.edi")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.variant").value("837P"))
                .andExpect(jsonPath("$.functionalGroups[0].transactions[0].implementationId")
                        .value("005010X222A1"));
    }

    @Test
    void samplesEndpointListsAllThreeVariants() throws Exception {
        mockMvc.perform(get("/api/samples"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(3))
                .andExpect(jsonPath("$[0].id").value("837p"))
                .andExpect(jsonPath("$[1].id").value("837i"))
                .andExpect(jsonPath("$[2].id").value("837d"));
    }

    @Test
    void parseRejectsNonIsaInput() throws Exception {
        mockMvc.perform(post("/api/parse")
                        .contentType(MediaType.TEXT_PLAIN)
                        .content("not an x12 file"))
                .andExpect(status().isBadRequest());
    }

    private static String loadSample(String path) throws IOException {
        try (var in = new ClassPathResource(path).getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
