# X12 Parser

Web service that parses X12 837 healthcare claim files (Professional, Institutional, Dental) into structured JSON. Spring Boot backend with a single-page static frontend.

## Stack

- Spring Boot 3.4 on Java 21
- Plain JS + HTML frontend served from `src/main/resources/static`
- JUnit 5 fixtures use the official X12 TR3 sample claims

## What it does

Accepts the raw text of a 5010 X12 837 file. Detects whether it's 837P (005010X222A1), 837I (005010X223A2), or 837D (005010X224A2). Returns nested JSON:

```
{
  "envelope": { "isa": {...}, "gs": {...} },
  "variant": "837P",
  "transactions": [
    {
      "header": { "st": {...}, "bht": {...} },
      "submitter": {...},
      "receiver": {...},
      "billingProviders": [
        {
          "subscribers": [
            {
              "patients": [
                {
                  "claims": [ { "clm": {...}, "serviceLines": [...] } ]
                }
              ]
            }
          ]
        }
      ]
    }
  ]
}
```

Segment names are kept as JSON keys in v1 (`NM1`, `CLM`, `SV1`, `SV2`, `SV3`). Friendly field names is a v2 polish — bolt on via a mapping config.

## API

| Method | Path              | Description                              |
|--------|-------------------|------------------------------------------|
| POST   | /api/parse        | `text/plain` body of raw 837. Returns JSON. |
| GET    | /api/samples      | Lists built-in samples (P / I / D).      |
| GET    | /api/samples/{id} | Returns one of the built-in sample 837s. |
| GET    | /                 | Static frontend.                         |
| GET    | /actuator/health  | Health check.                            |

## File tour

```
x12-parser/
├── pom.xml
├── system.properties           # pins Java 21 for Render
├── render.yaml                 # Render blueprint
├── mvnw, mvnw.cmd, .mvn/       # Maven wrapper
└── src/
    ├── main/
    │   ├── java/com/rwdenmark/x12/
    │   │   ├── X12ParserApplication.java
    │   │   ├── common/GlobalExceptionHandler.java
    │   │   ├── config/WebConfig.java
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
    │   │       ├── ParseController.java
    │   │       └── SampleController.java
    │   └── resources/
    │       ├── application.yml
    │       ├── static/index.html
    │       └── samples/
    │           ├── 837p_commercial.edi
    │           ├── 837i_institutional.edi
    │           └── 837d_dental.edi
    └── test/
        └── java/com/rwdenmark/x12/
            ├── parser/
            │   ├── TokenizerTest.java
            │   ├── HierarchyBuilderTest.java
            │   └── X12ParserServiceTest.java
            └── web/
                └── ParseControllerIT.java
```

## Sample data

Three sample claims live in `src/main/resources/samples/`:

- `837p_commercial.edi` — TR3 005010X222A1 Example 01 (commercial professional claim, Ben Kildare Service → Key Insurance, two office visits with throat / mono labs)
- `837i_institutional.edi` — TR3 005010X223A2 Example 1a (institutional outpatient lab claim, Jones Hospital → Medicare B + State Teachers secondary)
- `837d_dental.edi` — 005010X224A2 dental claim (Premier Billing Service → Insurance Company XYZ, two dental procedures)

All three are public reference examples from the X12 implementation guides. No PHI. Do not paste real PHI into this tool.

## Running locally

Requires Java 21.

```bash
./mvnw spring-boot:run
```

App on http://localhost:8080.

```bash
curl -X POST http://localhost:8080/api/parse \
  -H 'Content-Type: text/plain' \
  --data-binary @src/main/resources/samples/837p_commercial.edi
```

## Tests

```bash
./mvnw test
```

The parser tests round-trip all three sample claims and assert the variant detection, hierarchy shape, and claim totals.

## Deploying

Same pattern as `wow-explorer` and `ranger-survivor`. Render web service, Java 21 runtime, `./mvnw -DskipTests package` build, `java -jar target/x12-837-parser-0.0.1-SNAPSHOT.jar` start command.
