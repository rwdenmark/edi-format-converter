package com.rwdenmark.x12.web;

import java.util.List;
import java.util.Map;

import com.rwdenmark.x12.samples.SampleClaims;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/samples")
public class SampleController {

    private final SampleClaims samples;

    public SampleController(SampleClaims samples) {
        this.samples = samples;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<Map<String, String>> list() {
        return samples.all().values().stream()
                .map(s -> Map.of("id", s.id(), "description", s.description()))
                .toList();
    }

    @GetMapping(value = "/{id}", produces = MediaType.TEXT_PLAIN_VALUE)
    public String content(@PathVariable String id) {
        return samples.content(id);
    }
}
