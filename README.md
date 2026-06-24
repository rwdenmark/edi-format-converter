# EDI Format Converter

Web service that converts between X12 EDI (837 P/I/D), JSON, YAML, and XML. It parses an X12 837 into a structured model and serializes that model to JSON, YAML, or XML, and JSON, YAML, and XML convert freely among themselves. Writing X12 back out is phase 2 (422 for now). Spring Boot backend with a two-pane static frontend.

## Stack

- Spring Boot 3.4.1 on Java 21
- Jackson (databind, YAML, and XML dataformats) for the conversions
- Plain JS + HTML two-pane frontend served from `src/main/resources/static`
- JUnit 5 fixtures use the official X12 TR3 sample claims

## What it does

Accepts the raw text of a 5010 X12 837 file. Detects whether it's 837P (005010X222A1), 837I (005010X223A2), or 837D (005010X224A2). Returns JSON keyed by the X12 structure:

```
{
  "variant": "837P",
  "envelope": { "isa": { ... } },
  "functionalGroups": [
    {
      "gs": { ... },
      "transactions": [
        {
          "implementationId": "005010X222A1",
          "header": { "bht": {...}, "submitter": {...}, "receiver": {...} },
          "hierarchy": [
            {
              "levelCode": "20",
              "hasChildren": true,
              "claims": [ { "claimId": "...", "totalCharges": "...", "serviceLines": [...] } ],
              "children": [ { "levelCode": "22", "children": [...] } ]
            }
          ]
        }
      ]
    }
  ]
}
```

The HL loops are rebuilt into a real `hierarchy` tree by level code (20 billing provider, 22 subscriber, 23 patient), with claims and service lines hanging off the HL they belong to. Raw segment elements are kept under `segments` arrays so nothing is dropped. Friendly field names are a later polish.

## API

| Method | Path | Description |
|--------|------|-------------|
| POST | /api/convert?from={fmt}&to={fmt} | `text/plain` body of the source. Returns the target format. `from`/`to` in {x12, json, yaml, xml}. X12 as `to` returns 422 (phase 2). |
| POST | /api/parse | `text/plain` body of raw 837. Returns the parsed JSON model. |
| GET | /api/samples | Lists the built-in samples (P / I / D). |
| GET | /api/samples/{id} | Returns one built-in sample 837. |
| GET | /api/health | Liveness probe. |
| GET | /actuator/health | Actuator health. |
| GET | / | Static two-pane frontend. |

## File tour

```
edi-format-converter/
├── pom.xml
├── system.properties           # pins Java 21 for Render
├── render.yaml                 # Render blueprint
├── Dockerfile
├── mvnw, mvnw.cmd, .mvn/       # Maven wrapper
└── src/
    ├── main/
    │   ├── java/com/rwdenmark/x12/
    │   │   ├── X12ParserApplication.java
    │   │   ├── common/GlobalExceptionHandler.java
    │   │   ├── config/WebConfig.java
    │   │   ├── convert/
    │   │   │   ├── Format.java
    │   │   │   └── ConversionService.java
    │   │   ├── parser/
    │   │   │   ├── Delimiters.java
    │   │   │   ├── Segment.java
    │   │   │   ├── Tokenizer.java
    │   │   │   ├── EnvelopeParser.java
    │   │   │   ├── HierarchyBuilder.java
    │   │   │   ├── X12Variant.java
    │   │   │   ├── X12ParseException.java
    │   │   │   └── X12ParserService.java
    │   │   ├── samples/SampleClaims.java
    │   │   └── web/
    │   │       ├── ConvertController.java
    │   │       ├── ParseController.java
    │   │       ├── SampleController.java
    │   │       └── HealthController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── static/index.html
    │       └── samples/
    │           ├── 837p_commercial.edi
    │           ├── 837i_institutional.edi
    │           └── 837d_dental.edi
    └── test/
        └── java/com/rwdenmark/x12/
            ├── convert/ConversionServiceTest.java
            ├── parser/
            │   ├── TokenizerTest.java
            │   └── X12ParserServiceTest.java
            └── web/
                ├── ConvertControllerIT.java
                └── ParseControllerIT.java
```

## Sample data

Three sample claims live in `src/main/resources/samples/`:

- `837p_commercial.edi`: TR3 005010X222A1 Example 01 (commercial professional claim, Ben Kildare Service → Key Insurance, two office visits with throat