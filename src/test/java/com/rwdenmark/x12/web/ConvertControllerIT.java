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

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.startsWith;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ConvertControllerIT {

    @Autowired MockMvc mockMvc;

    @Test
    void x12ToJsonReturnsJsonBody() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "x12").param("to", "json")
                        .contentType(MediaType.TEXT_PLAIN).content(sample()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.variant").value("837P"));
    }

    @Test
    void x12ToYamlSetsYamlContentType() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "x12").param("to", "yaml")
                        .contentType(MediaType.TEXT_PLAIN).content(sample()))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.valueOf("application/x-yaml")));
    }

    @Test
    void jsonToStringReturnsEscapedPlainText() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "json").param("to", "string")
                        .contentType(MediaType.TEXT_PLAIN).content("{\"a\":1}"))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(startsWith("\"")));
    }

    @Test
    void jsonModelToX12ReturnsInterchange() throws Exception {
        String model = mockMvc.perform(post("/api/convert").param("from", "x12").param("to", "json")
                        .contentType(MediaType.TEXT_PLAIN).content(sample()))
                .andReturn().getResponse().getContentAsString();

        mockMvc.perform(post("/api/convert").param("from", "json").param("to", "x12")
                        .contentType(MediaType.TEXT_PLAIN).content(model))
                .andExpect(status().isOk())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(startsWith("ISA")));
    }

    @Test
    void writingX12FromNonModelReturns422() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "json").param("to", "x12")
                        .contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().string(containsString("not a parsed X12 model")));
    }

    @Test
    void unknownFormatReturns400() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "bogus").param("to", "json")
                        .contentType(MediaType.TEXT_PLAIN).content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void conversionErrorReturnsReadablePlainText() throws Exception {
        mockMvc.perform(post("/api/convert").param("from", "json").param("to", "yaml")
                        .contentType(MediaType.TEXT_PLAIN).content("}{ not json"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.TEXT_PLAIN))
                .andExpect(content().string(containsString("Could not read input as JSON")));
    }

    private static String sample() throws IOException {
        try (var in = new ClassPathResource("samples/837p_commercial.edi").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }
}
