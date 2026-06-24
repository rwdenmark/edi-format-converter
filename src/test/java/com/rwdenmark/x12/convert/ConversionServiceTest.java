package com.rwdenmark.x12.convert;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rwdenmark.x12.parser.X12ParserService;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/** Covers the conversion matrix over the real TR3 837P sample. No Spring context. */
class ConversionServiceTest {

    private final ConversionService service = new ConversionService(new X12ParserService());
    private final ObjectMapper json = new ObjectMapper();

    @Test
    void x12ToJsonExposesTheVariant() {
        String out = service.convert(Format.X12, Format.JSON, sample());
        assertThat(out).contains("\"variant\"").contains("837P");
    }

    @Test
    void x12ToYamlAndXmlBothCarryTheVariant() {
        assertThat(service.convert(Format.X12, Format.YAML, sample())).contains("variant").contains("837P");
        assertThat(service.convert(Format.X12, Format.XML, sample())).contains("837P");
    }

    @Test
    void jsonRoundTripsThroughYaml() throws Exception {
        String asJson = service.convert(Format.X12, Format.JSON, sample());
        String asYaml = service.convert(Format.JSON, Format.YAML, asJson);
        String backToJson = service.convert(Format.YAML, Format.JSON, asYaml);
        assertThat(json.readTree(backToJson)).isEqualTo(json.readTree(asJson));
    }

    @Test
    void sameFormatReturnsInputUnchanged() {
        assertThat(service.convert(Format.JSON, Format.JSON, "{\"a\":1}")).isEqualTo("{\"a\":1}");
    }

    @Test
    void writingX12IsUnsupported() {
        String asJson = service.convert(Format.X12, Format.JSON, sample());
        assertThatThrownBy(() -> service.convert(Format.JSON, Format.X12, asJson))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY));
    }

    @Test
    void unreadableInputIsBadRequest() {
        assertThatThrownBy(() -> service.convert(Format.JSON, Format.YAML, "}{ not json"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(e -> assertThat(((ResponseStatusException) e).getStatusCode())
                        .isEqualTo(HttpStatus.BAD_REQUEST));
    }

    private static String sample() {
        try (var in = new ClassPathResource("samples/837p_commercial.edi").getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
