package com.rwdenmark.x12.web;

import java.util.Map;

import com.rwdenmark.x12.parser.X12ParserService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/parse")
public class ParseController {

    private final X12ParserService parser;

    public ParseController(X12ParserService parser) {
        this.parser = parser;
    }

    @PostMapping(consumes = MediaType.TEXT_PLAIN_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, Object> parse(@RequestBody String raw) {
        return parser.parse(raw);
    }
}
