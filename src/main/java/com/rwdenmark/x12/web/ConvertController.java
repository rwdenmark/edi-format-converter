package com.rwdenmark.x12.web;

import com.rwdenmark.x12.convert.ConversionService;
import com.rwdenmark.x12.convert.Format;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/convert")
public class ConvertController {

    private final ConversionService conversion;

    public ConvertController(ConversionService conversion) {
        this.conversion = conversion;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> convert(@RequestParam String from,
                                          @RequestParam String to,
                                          @RequestBody String body) {
        Format f = Format.fromString(from);
        Format t = Format.fromString(to);
        String result = conversion.convert(f, t, body);
        return ResponseEntity.ok().contentType(contentType(t)).body(result);
    }

    private MediaType contentType(Format t) {
        return switch (t) {
            case JSON -> MediaType.APPLICATION_JSON;
            case XML -> MediaType.APPLICATION_XML;
            case YAML -> MediaType.valueOf("application/x-yaml");
            case X12 -> MediaType.TEXT_PLAIN;
        };
    }
}
