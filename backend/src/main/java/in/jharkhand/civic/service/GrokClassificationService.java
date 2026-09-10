package in.jharkhand.civic.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import in.jharkhand.civic.port.ClassificationService;
import in.jharkhand.civic.repository.JsonStore;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class GrokClassificationService implements ClassificationService {

  private final RestClient client;
  private final String model;
  private final String apiKey;
  private final JsonStore store;
  private final DuplicateDetectionService duplicates;
  private final PrototypeImageClassificationService imageClassifier;

  public GrokClassificationService(
          @Value("${app.xai.base-url:https://api.x.ai/v1}") String base,
          @Value("${app.xai.api-key:}") String key,
          @Value("${app.xai.model:grok-4.6}") String model,
          JsonStore store,
          DuplicateDetectionService duplicates,
          PrototypeImageClassificationService imageClassifier
  ) {
    this.model = model;
    this.apiKey = key;
    this.store = store;
    this.duplicates = duplicates;
    this.imageClassifier = imageClassifier;

    this.client = RestClient.builder()
            .baseUrl(base)
            .defaultHeader(
                    HttpHeaders.AUTHORIZATION,
                    "Bearer " + key
            )
            .defaultHeader(
                    HttpHeaders.CONTENT_TYPE,
                    MediaType.APPLICATION_JSON_VALUE
            )
            .build();
  }

  @Override
  public Map<String, Object> analyze(Map<String, Object> problem) {

    if (apiKey == null || apiKey.isBlank()) {
      throw new IllegalStateException(
              "XAI_API_KEY is not configured"
      );
    }

    String prompt = String.format(
            """
            You are the civic problem analysis layer for a
            Jharkhand government-ready civic innovation platform.

            Analyze the submitted civic problem semantically.

            IMPORTANT:
            - Do NOT make the final routing decision.
            - Do NOT invent facts.
            - Use only the information supplied by the citizen.
            - Return exactly the requested structured fields.
            - Urgency and recommendedPriority must be one of:
              LOW, MEDIUM, HIGH, CRITICAL.

            Problem:

            Title:
            %s

            Description:
            %s

            Language:
            %s

            Display language:
            %s

            Category:
            %s

            Location:
            %s

            Expected impact:
            %s

            Affected people:
            %s

            Image/file metadata:
            %s
            """,
            problem.get("title"),
            problem.get("description"),
            problem.get("language"),
            problem.getOrDefault(
                    "displayLanguage",
                    problem.get("language")
            ),
            problem.get("category"),
            problem.get("location"),
            problem.get("expectedImpact"),
            problem.get("affectedPeople"),
            problem.get("attachments")
    );

    Map<String, Object> body =
            new LinkedHashMap<>();

    body.put("model", model);
    body.put("input", prompt);

    Map<String, Object> text =
            new LinkedHashMap<>();

    Map<String, Object> format =
            new LinkedHashMap<>();

    format.put("type", "json_schema");
    format.put(
            "name",
            "civic_problem_analysis"
    );
    format.put("strict", true);
    format.put("schema", schema());

    text.put("format", format);

    body.put("text", text);

    String raw =
            client.post()
                    .uri("/responses")
                    .body(body)
                    .retrieve()
                    .body(String.class);

    try {

      ObjectMapper mapper =
              new ObjectMapper();

      JsonNode root =
              mapper.readTree(raw);

      String outputText =
              findOutputText(root);

      Map<String, Object> result =
              mapper.readValue(
                      outputText,
                      Map.class
              );

      result.put(
              "aiProvider",
              "xAI Grok"
      );

      result.put(
              "model",
              model
      );

      result.put(
              "generatedAt",
              Instant.now().toString()
      );

      /*
       * Duplicate detection is kept outside the LLM
       * so duplicate signals remain deterministic.
       */
      result.put(
              "duplicateSignals",
              duplicates.detect(problem)
      );

      /*
       * Optional image classification.
       */
      Object attachments =
              problem.get("attachments");

      if (attachments instanceof List<?> files
              && !files.isEmpty()) {

        String firstFile =
                String.valueOf(files.get(0));

        result.put(
                "imageClassification",
                imageClassifier.classify(firstFile)
        );
      }

      return result;

    } catch (Exception e) {

      throw new IllegalStateException(
              "Unable to parse Grok structured response",
              e
      );
    }
  }

  private String findOutputText(
          JsonNode root
  ) {

    /*
     * Direct output_text response.
     */
    if (root.has("output_text")
            && !root.get("output_text").isNull()) {

      return root
              .get("output_text")
              .asText();
    }

    /*
     * Standard Responses API message output.
     */
    JsonNode output =
            root.path("output");

    if (output.isArray()) {

      for (JsonNode item : output) {

        if ("message".equals(
                item.path("type").asText()
        )) {

          JsonNode content =
                  item.path("content");

          if (content.isArray()) {

            for (JsonNode contentItem :
                    content) {

              if ("output_text".equals(
                      contentItem
                              .path("type")
                              .asText()
              )) {

                return contentItem
                        .path("text")
                        .asText();
              }
            }
          }
        }
      }
    }

    throw new IllegalStateException(
            "No structured output returned by Grok"
    );
  }

  /**
   * JSON schema used for structured Grok classification.
   *
   * IMPORTANT:
   * Do not use Map.of() for the outer schema because
   * Java Map.of() supports a maximum of 10 key/value pairs.
   */
  private Map<String, Object> schema() {

    Map<String, Object> schema =
            new LinkedHashMap<>();

    schema.put(
            "type",
            "object"
    );

    Map<String, Object> properties =
            new LinkedHashMap<>();

    properties.put(
            "summary",
            stringSchema()
    );

    properties.put(
            "problemType",
            stringSchema()
    );

    properties.put(
            "domain",
            stringSchema()
    );

    properties.put(
            "subDomain",
            stringSchema()
    );

    properties.put(
            "keywords",
            stringArraySchema()
    );

    properties.put(
            "urgency",
            enumSchema(
                    "LOW",
                    "MEDIUM",
                    "HIGH",
                    "CRITICAL"
            )
    );

    properties.put(
            "severity",
            stringSchema()
    );

    properties.put(
            "affectedPopulation",
            stringSchema()
    );

    properties.put(
            "estimatedImpact",
            stringSchema()
    );

    properties.put(
            "requiredExpertise",
            stringArraySchema()
    );

    properties.put(
            "possibleDepartments",
            stringArraySchema()
    );

    properties.put(
            "possibleInstitutionExpertise",
            stringArraySchema()
    );

    properties.put(
            "duplicateSignals",
            stringArraySchema()
    );

    properties.put(
            "recommendedPriority",
            enumSchema(
                    "LOW",
                    "MEDIUM",
                    "HIGH",
                    "CRITICAL"
            )
    );

    properties.put(
            "reasoning",
            stringSchema()
    );

    schema.put(
            "properties",
            properties
    );

    schema.put(
            "required",
            List.of(
                    "summary",
                    "problemType",
                    "domain",
                    "subDomain",
                    "keywords",
                    "urgency",
                    "severity",
                    "affectedPopulation",
                    "estimatedImpact",
                    "requiredExpertise",
                    "possibleDepartments",
                    "possibleInstitutionExpertise",
                    "duplicateSignals",
                    "recommendedPriority",
                    "reasoning"
            )
    );

    schema.put(
            "additionalProperties",
            false
    );

    return schema;
  }

  private Map<String, Object> stringSchema() {

    Map<String, Object> schema =
            new LinkedHashMap<>();

    schema.put(
            "type",
            "string"
    );

    return schema;
  }

  private Map<String, Object> stringArraySchema() {

    Map<String, Object> schema =
            new LinkedHashMap<>();

    schema.put(
            "type",
            "array"
    );

    schema.put(
            "items",
            stringSchema()
    );

    return schema;
  }

  private Map<String, Object> enumSchema(
          String... values
  ) {

    Map<String, Object> schema =
            new LinkedHashMap<>();

    schema.put(
            "type",
            "string"
    );

    schema.put(
            "enum",
            new ArrayList<>(
                    List.of(values)
            )
    );

    return schema;
  }
}
