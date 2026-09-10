package in.jharkhand.civic.service;

import in.jharkhand.civic.repository.JsonStore;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@Service
public class ProblemService {

 private final JsonStore s;
 private final IdService ids;
 private final GrokClassificationService grok;
 private final RoutingEngine routing;
 private final AuditService audit;
 private final DuplicateDetectionService dup;
 private final ReferenceService ref;

 public ProblemService(
         JsonStore s,
         IdService i,
         GrokClassificationService g,
         RoutingEngine r,
         AuditService a,
         DuplicateDetectionService d,
         ReferenceService ref
 ) {
  this.s = s;
  this.ids = i;
  this.grok = g;
  this.routing = r;
  this.audit = a;
  this.dup = d;
  this.ref = ref;
 }

 public List<Map<String, Object>> list(String userId, String role) {

  List<Map<String, Object>> all = s.read("problems.json");

  if ("ADMIN".equals(role)) {
   return all;
  }

  if ("USER".equals(role)) {
   return all.stream()
           .filter(p -> userId.equals(String.valueOf(p.get("userId"))))
           .toList();
  }

  if ("DEPARTMENT".equals(role)) {
   Map<String, Object> u = s.find("users.json", userId);

   String departmentId = String.valueOf(
           u == null ? "" : u.getOrDefault("departmentId", "")
   );

   return all.stream()
           .filter(p ->
                   departmentId.equals(
                           String.valueOf(
                                   p.getOrDefault("assignedDepartmentId", "")
                           )
                   )
           )
           .toList();
  }

  if ("INSTITUTION".equals(role)) {
   Map<String, Object> u = s.find("users.json", userId);

   String institutionId = String.valueOf(
           u == null ? "" : u.getOrDefault("institutionId", "")
   );

   return all.stream()
           .filter(p ->
                   institutionId.equals(
                           String.valueOf(
                                   p.getOrDefault("assignedInstitutionId", "")
                           )
                   )
           )
           .toList();
  }

  /*
   * Industry does not receive raw civic problems.
   * Industry access begins after the collaboration stage.
   */
  return List.of();
 }

 public Map<String, Object> get(
         String id,
         String userId,
         String role
 ) {

  Map<String, Object> p = s.find("problems.json", id);

  if (p == null) {
   throw new RuntimeException("Problem not found");
  }

  if ("ADMIN".equals(role)) {
   return p;
  }

  if ("USER".equals(role)) {

   if (!userId.equals(String.valueOf(p.get("userId")))) {
    throw new RuntimeException("Access denied");
   }

   return p;
  }

  if ("DEPARTMENT".equals(role)) {

   Map<String, Object> u = s.find("users.json", userId);

   if (u == null) {
    throw new RuntimeException("Access denied");
   }

   String userDepartment =
           String.valueOf(u.getOrDefault("departmentId", ""));

   String assignedDepartment =
           String.valueOf(
                   p.getOrDefault("assignedDepartmentId", "")
           );

   if (!userDepartment.equals(assignedDepartment)) {
    throw new RuntimeException("Access denied");
   }

   return p;
  }

  if ("INSTITUTION".equals(role)) {

   Map<String, Object> u = s.find("users.json", userId);

   if (u == null) {
    throw new RuntimeException("Access denied");
   }

   String userInstitution =
           String.valueOf(u.getOrDefault("institutionId", ""));

   String assignedInstitution =
           String.valueOf(
                   p.getOrDefault("assignedInstitutionId", "")
           );

   if (!userInstitution.equals(assignedInstitution)) {
    throw new RuntimeException("Access denied");
   }

   return p;
  }

  if ("INDUSTRY".equals(role)) {
   throw new RuntimeException(
           "Industry access begins at collaboration stage"
   );
  }

  throw new RuntimeException("Access denied");
 }

 public Map<String, Object> create(
         String userId,
         Map<String, Object> req,
         List<MultipartFile> files
 ) throws Exception {

  Map<String, Object> p = new LinkedHashMap<>();

  String problemId = ids.id("problem");

  p.put("id", problemId);
  p.put("userId", userId);

  p.put("title", req.get("title"));
  p.put("description", req.get("description"));

  p.put(
          "category",
          req.getOrDefault("category", "Other")
  );

  p.put(
          "location",
          req.getOrDefault("location", Map.of())
  );

  p.put(
          "language",
          req.getOrDefault("language", "en")
  );

  p.put(
          "originalLanguage",
          req.getOrDefault("language", "en")
  );

  p.put(
          "originalText",
          req.get("description")
  );

  p.put(
          "normalizedText",
          req.get("description")
  );

  p.put(
          "displayLanguage",
          req.getOrDefault("language", "en")
  );

  p.put(
          "expectedImpact",
          req.getOrDefault("expectedImpact", "")
  );

  p.put(
          "affectedPeople",
          req.getOrDefault("affectedPeople", 0)
  );

  p.put(
          "contactDetails",
          req.getOrDefault("contactDetails", "")
  );

  /*
   * Initial civic-problem lifecycle.
   */
  p.put("status", "SUBMITTED");

  p.put("isDemoData", false);

  String now = Instant.now().toString();

  p.put("createdAt", now);
  p.put("updatedAt", now);

  /*
   * File attachments.
   */
  List<String> attachments = new ArrayList<>();

  if (files != null) {

   Path dir = Paths.get(
           s.dataDir().toString(),
           "uploads",
           problemId
   );

   Files.createDirectories(dir);

   for (MultipartFile f : files) {

    if (f == null || f.isEmpty()) {
     continue;
    }

    String originalName =
            Optional.ofNullable(f.getOriginalFilename())
                    .orElse("file");

    String name =
            Path.of(originalName)
                    .getFileName()
                    .toString();

    String lower =
            name.toLowerCase();

    boolean supported =
            lower.endsWith(".jpg")
                    || lower.endsWith(".jpeg")
                    || lower.endsWith(".png")
                    || lower.endsWith(".webp")
                    || lower.endsWith(".mp3")
                    || lower.endsWith(".wav")
                    || lower.endsWith(".m4a")
                    || lower.endsWith(".pdf")
                    || lower.endsWith(".doc")
                    || lower.endsWith(".docx");

    if (!supported) {
     throw new RuntimeException(
             "Unsupported file type"
     );
    }

    if (f.getSize() > 10 * 1024 * 1024) {
     throw new RuntimeException(
             "File too large; maximum 10 MB"
     );
    }

    Path destination =
            dir.resolve(name);

    Files.copy(
            f.getInputStream(),
            destination,
            StandardCopyOption.REPLACE_EXISTING
    );

    attachments.add(name);
   }
  }

  p.put("attachments", attachments);

  /*
   * Detect possible duplicate reports before AI analysis.
   */
  p.put(
          "duplicateSignals",
          dup.detect(p)
  );

  /*
   * IMPORTANT:
   * Save the problem BEFORE calling analyze().
   *
   * analyze() expects the problem ID, actor and role.
   * The previous code incorrectly passed the entire Map.
   */
  s.add("problems.json", p);

  audit.log(
          userId,
          "USER",
          "PROBLEM_SUBMITTED",
          "PROBLEM",
          problemId,
          null,
          p
  );

  /*
   * Start AI classification.
   *
   * The analysis method has the signature:
   *
   * analyze(String id, String actor, String role)
   *
   * Therefore we pass:
   * problemId -> ID of saved problem
   * userId    -> submitting citizen
   * USER      -> submitting role
   */
  try {

   analyze(
           problemId,
           userId,
           "USER"
   );

  } catch (Exception ignored) {

   /*
    * The problem itself is already safely stored.
    * AI failure must not delete or invalidate the citizen's report.
    */
  }

  Map<String, Object> saved =
          s.find("problems.json", problemId);

  return saved == null ? p : saved;
 }

 public Map<String, Object> analyze(
         String id,
         String actor,
         String role
 ) {

  Map<String, Object> p =
          s.find("problems.json", id);

  if (p == null) {
   throw new RuntimeException(
           "Problem not found"
   );
  }

  Map<String, Object> ai;

  try {

   ai = grok.analyze(p);

   ai.put(
           "id",
           ids.id("ai")
   );

   ai.put(
           "problemId",
           id
   );

   ai.put(
           "humanApproved",
           false
   );

   s.add(
           "ai-analysis.json",
           ai
   );

   s.update(
           "problems.json",
           id,
           Map.of(
                   "status",
                   "PENDING_VALIDATION",

                   "aiStatus",
                   "COMPLETED",

                   "updatedAt",
                   Instant.now().toString()
           )
   );

   audit.log(
           actor,
           role,
           "AI_ANALYSIS_GENERATED",
           "PROBLEM",
           id,
           null,
           ai
   );

   return Map.of(
           "problem",
           s.find("problems.json", id),

           "analysis",
           ai
   );

  } catch (Exception e) {

   /*
    * AI failure should never lose the civic report.
    *
    * Admin can perform manual classification later.
    */
   s.update(
           "problems.json",
           id,
           Map.of(
                   "status",
                   "PENDING_VALIDATION",

                   "aiStatus",
                   "AI_ANALYSIS_PENDING",

                   "updatedAt",
                   Instant.now().toString()
           )
   );

   return Map.of(
           "problem",
           s.find("problems.json", id),

           "analysisStatus",
           "AI_ANALYSIS_PENDING",

           "message",
           "AI analysis is temporarily unavailable. "
                   + "Your submission has been saved and will be "
                   + "processed when the service becomes available."
   );
  }
 }

 public Map<String, Object> analysis(
         String id
 ) {

  return s.read("ai-analysis.json")
          .stream()
          .filter(
                  x -> id.equals(
                          String.valueOf(
                                  x.get("problemId")
                          )
                  )
          )
          .reduce(
                  (a, b) -> b
          )
          .orElse(null);
 }

 public Map<String, Object> route(
         String id,
         String actor,
         String role
 ) {

  Map<String, Object> p =
          s.find("problems.json", id);

  if (p == null) {
   throw new RuntimeException(
           "Problem not found"
   );
  }

  Map<String, Object> ai =
          analysis(id);

  if (ai == null) {
   throw new RuntimeException(
           "Analysis is required before routing"
   );
  }

  if (!Boolean.TRUE.equals(
          ai.get("humanApproved")
  )) {

   throw new RuntimeException(
           "Admin validation is required before routing"
   );
  }

  Map<String, Object> r =
          routing.route(p, ai);

  r.put(
          "id",
          ids.id("route")
  );

  r.put(
          "problemId",
          id
  );

  r.put(
          "approved",
          false
  );

  s.add(
          "routing-decisions.json",
          r
  );

  s.update(
          "problems.json",
          id,
          Map.of(
                  "status",
                  "ROUTED",

                  "updatedAt",
                  Instant.now().toString()
          )
  );

  audit.log(
          actor,
          role,
          "ROUTING_GENERATED",
          "PROBLEM",
          id,
          null,
          r
  );

  return r;
 }

 public Map<String, Object> approveAnalysis(
         String id,
         String actor,
         String role,
         String decision,
         String comments,
         Object changes
 ) {

  Map<String, Object> ai =
          analysis(id);

  /*
   * Manual Admin classification when AI was unavailable.
   */
  if (ai == null) {

   Map<String, Object> p =
           s.find("problems.json", id);

   if (p == null) {
    throw new RuntimeException(
            "Problem not found"
    );
   }

   Map<String, Object> manual =
           new LinkedHashMap<>();

   manual.put(
           "id",
           ids.id("ai")
   );

   manual.put(
           "problemId",
           id
   );

   manual.put(
           "summary",
           String.valueOf(
                   p.getOrDefault(
                           "description",
                           ""
                   )
           )
   );

   manual.put(
           "problemType",
           String.valueOf(
                   p.getOrDefault(
                           "category",
                           "Other"
                   )
           )
   );

   manual.put(
           "domain",
           String.valueOf(
                   p.getOrDefault(
                           "category",
                           "Other"
                   )
           )
   );

   manual.put(
           "subDomain",
           String.valueOf(
                   p.getOrDefault(
                           "category",
                           "Other"
                   )
           )
   );

   manual.put(
           "keywords",
           List.of()
   );

   manual.put(
           "urgency",
           "MEDIUM"
   );

   manual.put(
           "severity",
           "MEDIUM"
   );

   manual.put(
           "affectedPopulation",
           String.valueOf(
                   p.getOrDefault(
                           "affectedPeople",
                           0
                   )
           )
   );

   manual.put(
           "estimatedImpact",
           String.valueOf(
                   p.getOrDefault(
                           "expectedImpact",
                           ""
                   )
           )
   );

   manual.put(
           "requiredExpertise",
           List.of()
   );

   manual.put(
           "possibleDepartments",
           List.of()
   );

   manual.put(
           "possibleInstitutionExpertise",
           List.of()
   );

   manual.put(
           "duplicateSignals",
           List.of()
   );

   manual.put(
           "recommendedPriority",
           "MEDIUM"
   );

   manual.put(
           "reasoning",
           "Manual classification entered by Admin "
                   + "because AI analysis was unavailable."
   );

   manual.put(
           "aiProvider",
           "Manual Admin Classification"
   );

   manual.put(
           "humanApproved",
           false
   );

   s.add(
           "ai-analysis.json",
           manual
   );

   ai = manual;
  }

  Map<String, Object> patch =
          new LinkedHashMap<>();

  boolean approved =
          "APPROVE".equalsIgnoreCase(
                  decision
          );

  patch.put(
          "humanApproved",
          approved
  );

  patch.put(
          "humanDecision",
          decision
  );

  patch.put(
          "humanComments",
          comments
  );

  patch.put(
          "reviewedBy",
          actor
  );

  patch.put(
          "reviewedAt",
          Instant.now().toString()
  );

  if (changes instanceof Map<?, ?> cm) {

   cm.forEach(
           (k, v) ->
                   patch.put(
                           String.valueOf(k),
                           v
                   )
   );

   patch.put(
           "humanChanges",
           changes
   );
  }

  s.update(
          "ai-analysis.json",
          String.valueOf(
                  ai.get("id")
          ),
          patch
  );

  s.update(
          "problems.json",
          id,
          Map.of(
                  "status",
                  approved
                          ? "VALIDATED"
                          : "REJECTED",

                  "updatedAt",
                  Instant.now().toString()
          )
  );

  audit.log(
          actor,
          role,
          "ADMIN_APPROVED_AI_ANALYSIS",
          "PROBLEM",
          id,
          ai,
          patch
  );

  return analysis(id);
 }

 public Map<String, Object> routing(
         String id
 ) {

  return s.read("routing-decisions.json")
          .stream()
          .filter(
                  x -> id.equals(
                          String.valueOf(
                                  x.get("problemId")
                          )
                  )
          )
          .reduce(
                  (a, b) -> b
          )
          .orElse(null);
 }

 public Map<String, Object> approveRouting(
         String routeId,
         String actor,
         String role
 ) {

  Map<String, Object> r =
          s.find(
                  "routing-decisions.json",
                  routeId
          );

  if (r == null) {
   throw new RuntimeException(
           "Routing not found"
   );
  }

  s.update(
          "routing-decisions.json",
          routeId,
          Map.of(
                  "approved",
                  true,

                  "approvedBy",
                  actor,

                  "approvedAt",
                  Instant.now().toString()
          )
  );

  String problemId =
          String.valueOf(
                  r.get("problemId")
          );

  List<?> departments =
          r.get("departments") instanceof List<?> list
                  ? list
                  : List.of();

  List<?> institutions =
          r.get("institutions") instanceof List<?> list
                  ? list
                  : List.of();

  /*
   * A valid routing decision must identify at least
   * one eligible department and institution.
   */
  if (departments.isEmpty()
          || institutions.isEmpty()) {

   throw new RuntimeException(
           "Routing has no eligible department "
                   + "or institution candidate"
   );
  }

  Object departmentCandidate =
          departments.get(0);

  Object institutionCandidate =
          institutions.get(0);

  if (!(departmentCandidate instanceof Map<?, ?> department)
          || !(institutionCandidate instanceof Map<?, ?> institution)) {

   throw new RuntimeException(
           "Invalid routing candidate format"
   );
  }

  String departmentId =
          String.valueOf(
                  department.get("departmentId")
          );

  String institutionId =
          String.valueOf(
                  institution.get("institutionId")
          );

  s.update(
          "problems.json",
          problemId,
          Map.of(
                  "status",
                  "ROUTED",

                  "assignedDepartmentId",
                  departmentId,

                  "assignedInstitutionId",
                  institutionId,

                  "updatedAt",
                  Instant.now().toString()
          )
  );

  audit.log(
          actor,
          role,
          "ADMIN_CHANGED_ROUTING",
          "PROBLEM",
          problemId,
          null,
          r
  );

  return routing(problemId);
 }

 public Map<String, Object> rejectRouting(
         String routeId,
         String actor
 ) {

  Map<String, Object> r =
          s.find(
                  "routing-decisions.json",
                  routeId
          );

  if (r == null) {
   throw new RuntimeException(
           "Routing not found"
   );
  }

  Map<String, Object> result =
          s.update(
                  "routing-decisions.json",
                  routeId,
                  Map.of(
                          "approved",
                          false,

                          "rejected",
                          true,

                          "rejectedBy",
                          actor,

                          "rejectedAt",
                          Instant.now().toString()
                  )
          );

  String problemId =
          String.valueOf(
                  r.get("problemId")
          );

  s.update(
          "problems.json",
          problemId,
          Map.of(
                  "status",
                  "NEEDS_CLARIFICATION",

                  "updatedAt",
                  Instant.now().toString()
          )
  );

  audit.log(
          actor,
          "ADMIN",
          "ROUTING_REJECTED",
          "PROBLEM",
          problemId,
          r,
          result
  );

  return result;
 }
}