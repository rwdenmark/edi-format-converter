package com.rwdenmark.x12.convert;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLMapper;
import com.rwdenmark.x12.parser.X12ParserService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

/**
 * Canonical-model converter. Every format is read into a plain Object graph
 * (Map / List / scalar) and written back out of it. X12 is read by the existing
 * parser; writing X12 back out is phase 2.
 */
@Service
public class ConversionService {

    private final X12ParserService x12;
    private final ObjectMapper json = new ObjectMapper();
    private final YAMLMapper yaml = new YAMLMapper();
    private final XmlMapper xml = new XmlMapper();

    public ConversionService(X12ParserService x12) {
        this.x12 = x12;
    }

    public String convert(Format from, Format to, String payload) {
        if (from == to) {
            return payload;
        }
        return write(to, read(from, payload));
    }

    private Object read(Format from, String payload) {
        try {
            return switch (from) {
                case X12 -> x12.parse(payload);
                case JSON -> json.readValue(payload, Object.class);
                case YAML -> yaml.readValue(payload, Object.class);
                case XML -> xml.readValue(payload, Object.class);
            };
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "Could not read input as " + from + ": " + e.getMessage());
        }
    }

    private String write(Format to, Object model) {
        try {
            return switch (to) {
                case JSON -> json.writerWithDefaultPrettyPrinter().writeValueAsString(model);
                case YAML -> yaml.writeValueAsString(model);
                case XML -> xml.writer().withRootName("x12").writeValueAsString(model);
                case X12 -> throw new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY,
                        "Writing X12 output is not supported yet (phase 2).");
            };
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR,
                    "Could not write output as " + to + ": " + e.getMessage());
        }
    }
}
