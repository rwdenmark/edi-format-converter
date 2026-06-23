# Enhancement - Any-to-Any Format Converter

Added 6/23/2026. Turn the X12 parser into a two-pane converter. Left pane is the source, right pane is the target. Each side has buttons to choose the format: X12, JSON, YAML, XML. Pick a format on each side, paste/load on the left, hit Convert, see the result on the right.

This builds on the existing pipeline (X12 -> nested claim model -> JSON). The claim model becomes the canonical hub, and every format is just a reader into the model or a writer out of it.

## Core idea

```
        READ                         WRITE
 source text --> [ Claim model ] --> target text
 (X12/JSON/         (existing            (X12/JSON/
  YAML/XML)          POJO graph)          YAML/XML)
```

- JSON, YAML, XML are all the SAME object graph, just different serializations. Jackson reads and writes all three off the existing model with almost no new code.
- X12 is the special one. X12 -> model already exists (the parser). model -> X12 must be written and is the hard part (see Phasing).

## What's easy vs hard (be honest in the README)

- Easy, do first: JSON <-> YAML <-> XML in any direction, and X12 -> {JSON, YAML, XML}. These are Jackson plus the parser you already have.
- Hard, phase it: {JSON, YAML, XML} -> X12 (writing valid X12 back out). X12 has strict segment order, delimiters, and envelope control numbers, so regenerating it faithfully is real work. Ship the easy matrix first, mark X12-as-output as "beta / 837P only" until it's solid.

## Architecture

Introduce two small interfaces and a dispatcher so adding a format later (e.g., FHIR) is a drop-in.

```
com.rwdenmark.x12.convert
  Format.java            // enum: X12, JSON, YAML, XML
  FormatReader.java      // String -> Claim model
  FormatWriter.java      // Claim model -> String
  ConversionService.java // picks reader by 'from', writer by 'to'
  readers/  X12Reader (wraps existing X12ParserService), JsonReader, YamlReader, XmlReader
  writers/  JsonWriter (existing behavior), YamlWriter, XmlWriter, X12Writer (phased)
```

- `ConversionService.convert(from, to, payload)`: `model = readers.get(from).read(payload); return writers.get(to).write(model);`
- JSON/YAML/XML readers and writers are thin wrappers over Jackson `ObjectMapper`, `YAMLMapper`, `XmlMapper` against the existing model classes.
- `X12Reader` just calls the parser you already have. `X12Writer` walks the model and emits segments (phase 2).

## Dependencies to add (pom.xml)

- `com.fasterxml.jackson.dataformat:jackson-dataformat-yaml`
- `com.fasterxml.jackson.dataformat:jackson-dataformat-xml`
(jackson-databind for JSON is already present via Spring Web.)

Note: the current model may need a couple of Jackson annotations for clean XML (a root `@JacksonXmlRootElement`, and `@JacksonXmlElementWrapper` on lists) and stable ordering. Add them on the model, not per-format.

## API

Keep `/api/parse` for back-compat. Add:

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/convert?from={fmt}&to={fmt} | Body is the source text. Returns the converted text in the target format. `from`/`to` in {x12,json,yaml,xml}. |

- Set the response `Content-Type` to match the target (application/json, application/x-yaml, application/xml, text/plain for X12).
- Validation: if `from == to`, just pretty-print/normalize. Reject unknown formats with a 400. Reject X12-as-target with 422 + a clear message until phase 2 lands.
- Reuse `GlobalExceptionHandler` so a bad parse returns a clean error, not a stack trace.

## Frontend (single page, src/main/resources/static/index.html)

Two-column layout:

```
[ X12 | JSON | YAML | XML ]            [ X12 | JSON | YAML | XML ]
+-------------------------+   (swap)   +--------------------------+
|  source textarea        |   <-->     |  result textarea (RO)    |
|  (paste or Load sample) |  [Convert] |  [Copy] [Download]       |
+-------------------------+            +--------------------------+
```

- Each side has a row of four toggle buttons (segmented control). Selected = active format.
- Middle: a Convert button and a swap arrow that flips source/target formats (and moves the result into the source box) for round-trip testing.
- Left side keeps the existing "Load sample" (the built-in 837P/I/D) so people can try it in one click.
- Right side gets Copy and Download buttons.
- On Convert: `POST /api/convert?from=LEFT&to=RIGHT` with the left textarea as the body, drop the response into the right textarea. Show errors inline.

## Build order

1. Add the two Jackson dataformat deps. Add `Format` enum.
2. Define `FormatReader`/`FormatWriter`, wire `ConversionService`.
3. Implement JSON/YAML/XML readers + writers over the existing model. Add the XML annotations.
4. Implement `X12Reader` as a wrapper over `X12ParserService`. Now X12 -> {json,yaml,xml} works.
5. Add `/api/convert`. Unit-test `ConversionService` for the round trips that don't need X12 output (json<->yaml<->xml, x12->each).
6. Rebuild the frontend into the two-pane layout. Keep `/api/parse` working.
7. Phase 2: `X12Writer` (start with 837P only), flip X12-as-target on, add round-trip tests (x12 -> json -> x12 should be semantically equal).

## Tests

- `ConversionServiceTest`: for each (from,to) pair in the supported matrix, convert and assert structure. Use the existing TR3 sample claims as fixtures.
- Round-trip: x12 -> json -> yaml -> xml -> json and assert the model is unchanged (parse both ends, compare objects, not strings).
- `ConvertControllerIT`: WebMvc test hitting `/api/convert` with sample payloads and asserting content types and 400/422 on bad input.

## Resume framing

A canonical-model integration hub: read any of four formats into one model, write any of four back out, with a clean reader/writer abstraction that makes new formats (FHIR next) a drop-in. This is exactly the schema-translation and data-mapping work (your 4-5) shown as a usable tool, and the honest "X12 output is the hard part, phased it" note signals real engineering judgment rather than a happy-path demo.
