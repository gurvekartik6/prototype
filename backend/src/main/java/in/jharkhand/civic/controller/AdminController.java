package in.jharkhand.civic.controller;

import in.jharkhand.civic.repository.JsonStore;
import in.jharkhand.civic.service.AuditService;
import in.jharkhand.civic.service.ProblemService;
import in.jharkhand.civic.service.WorkflowService;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.time.Instant;
import java.util.*;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {
  private final JsonStore s; private final ProblemService p; private final WorkflowService w; private final AuditService audit;
  public AdminController(JsonStore s, ProblemService p, WorkflowService w, AuditService audit){this.s=s;this.p=p;this.w=w;this.audit=audit;}

  @GetMapping("/dashboard") public Map<String,Object> dash(){
    var ps=s.read("problems.json"); var projects=s.read("projects.json"); var impacts=s.read("impact-metrics.json");
    Map<String,Object>x=new LinkedHashMap<>(); x.put("totalProblems",ps.size());
    x.put("pendingValidation",ps.stream().filter(q->"PENDING_VALIDATION".equals(q.get("status"))).count());
    x.put("highPriority",ps.stream().filter(q->"HIGH".equals(q.get("priority"))||"CRITICAL".equals(q.get("priority"))).count());
    x.put("routedProblems",ps.stream().filter(q->"ROUTED".equals(q.get("status"))).count());
    x.put("activeProjects",projects.stream().filter(q->!Set.of("COMPLETED","CANCELLED").contains(q.get("status"))).count());
    x.put("fieldTests",s.read("field-tests.json").size()); x.put("deployedSolutions",projects.stream().filter(q->"DEPLOYED".equals(q.get("status"))).count());
    x.put("verifiedImpact",impacts.stream().filter(q->Boolean.TRUE.equals(q.get("verified"))).count());
    x.put("civicPoints",s.read("civic-points.json").stream().mapToInt(q->((Number)q.getOrDefault("points",0)).intValue()).sum());
    x.put("departmentDistribution",ps.stream().collect(java.util.stream.Collectors.groupingBy(q->String.valueOf(q.getOrDefault("assignedDepartmentId","UNASSIGNED")),java.util.stream.Collectors.counting())));
    x.put("institutionDistribution",ps.stream().collect(java.util.stream.Collectors.groupingBy(q->String.valueOf(q.getOrDefault("assignedInstitutionId","UNASSIGNED")),java.util.stream.Collectors.counting())));
    List<Map<String,Object>> allInstitutions=new ArrayList<>(); for(int tier=1;tier<=4;tier++) allInstitutions.addAll(s.read("institutions-tier"+tier+".json")); x.put("tierDistribution",allInstitutions.stream().collect(java.util.stream.Collectors.groupingBy(q->String.valueOf(q.get("tier")),java.util.stream.Collectors.counting())));
    return x;
  }
  @GetMapping("/problems") List<Map<String,Object>> problems(){return s.read("problems.json");}
  @GetMapping("/projects") List<Map<String,Object>> projects(){return w.projects();}
  @GetMapping("/users") List<Map<String,Object>> users(){return s.read("users.json").stream().map(u->{Map<String,Object>x=new LinkedHashMap<>(u);x.remove("passwordHash");return x;}).toList();}
  @GetMapping("/audit-logs") List<Map<String,Object>> logs(){return s.read("audit-logs.json");}
  @GetMapping("/industry") List<Map<String,Object>> industry(){return s.read("industry-partners.json");}
  @PostMapping("/industry") Map<String,Object> addIndustry(@RequestBody Map<String,Object> req,Authentication a){Map<String,Object>x=new LinkedHashMap<>(req);x.put("id",UUID.randomUUID().toString());x.putIfAbsent("active",true);s.add("industry-partners.json",x);audit.log(a.getName(),"ADMIN","INDUSTRY_PARTNER_CREATED","INDUSTRY",String.valueOf(x.get("id")),null,x);return x;}
  @PatchMapping("/industry/{id}") Map<String,Object> updateIndustry(@PathVariable String id,@RequestBody Map<String,Object> patch,Authentication a){return updateReference("industry-partners.json",id,patch,a,"INDUSTRY");}
  @GetMapping("/field-tests") List<Map<String,Object>> fieldTests(){return s.read("field-tests.json");}
  @GetMapping("/impact") List<Map<String,Object>> impact(){return s.read("impact-metrics.json");}
  @GetMapping("/milestones") List<Map<String,Object>> milestones(){return s.read("milestones.json");}
  @GetMapping("/civic-points") List<Map<String,Object>> civicPoints(){return s.read("civic-points.json");}
  @GetMapping("/config") List<Map<String,Object>> config(){return s.read("admin-config.json");}
  @GetMapping("/translation-overrides") List<Map<String,Object>> translations(){return s.read("translation-overrides.json");}

  @PostMapping("/problems/{id}/ai-review") Map<String,Object> review(@PathVariable String id,@RequestBody Map<String,Object> r,Authentication a){
    String d=String.valueOf(r.getOrDefault("decision","APPROVE"));
    return p.approveAnalysis(id,a.getName(),"ADMIN",d,String.valueOf(r.getOrDefault("comments","")),r.get("changes"));
  }
  @PostMapping("/routing/{routeId}/reject") Map<String,Object> rejectRouting(@PathVariable String routeId,Authentication a){return p.rejectRouting(routeId,a.getName());}

  @PatchMapping("/departments/{id}") Map<String,Object> updateDepartment(@PathVariable String id,@RequestBody Map<String,Object> patch,Authentication a){return updateReference("departments.json",id,patch,a,"DEPARTMENT");}
  @PatchMapping("/institutions/{id}") Map<String,Object> updateInstitution(@PathVariable String id,@RequestBody Map<String,Object> patch,Authentication a){for(int tier=1;tier<=4;tier++){Map<String,Object>x=s.find("institutions-tier"+tier+".json",id);if(x!=null)return updateReference("institutions-tier"+tier+".json",id,patch,a,"INSTITUTION");}throw new RuntimeException("Institution not found");}
  @PatchMapping("/users/{id}") Map<String,Object> updateUser(@PathVariable String id,@RequestBody Map<String,Object> patch,Authentication a){patch.remove("passwordHash");return updateReference("users.json",id,patch,a,"USER");}
  @PostMapping("/civic-points") Map<String,Object> awardPoints(@RequestBody Map<String,Object> req,Authentication a){Map<String,Object>x=new LinkedHashMap<>(req);x.put("id",UUID.randomUUID().toString());x.put("verified",true);x.put("verifiedBy",a.getName());x.put("createdAt",Instant.now().toString());s.add("civic-points.json",x);audit.log(a.getName(),"ADMIN","CIVIC_POINTS_APPROVED","CIVIC_POINT",String.valueOf(x.get("id")),null,x);return x;}
  @PutMapping("/config/{id}") Map<String,Object> updateConfig(@PathVariable String id,@RequestBody Map<String,Object> patch,Authentication a){return updateReference("admin-config.json",id,patch,a,"CONFIG");}
  @PostMapping("/translation-overrides") Map<String,Object> translation(@RequestBody Map<String,Object> req,Authentication a){Map<String,Object>x=new LinkedHashMap<>(req);x.put("id",UUID.randomUUID().toString());x.put("updatedBy",a.getName());x.put("updatedAt",Instant.now().toString());s.add("translation-overrides.json",x);audit.log(a.getName(),"ADMIN","TRANSLATION_UPDATED","TRANSLATION",String.valueOf(x.get("id")),null,x);return x;}
  @GetMapping("/stats/reference") Map<String,Object> referenceStats(){return Map.of("departments",s.read("departments.json").size(),"tier1",s.read("institutions-tier1.json").size(),"tier2",s.read("institutions-tier2.json").size(),"tier3",s.read("institutions-tier3.json").size(),"tier4",s.read("institutions-tier4.json").size());}
  private Map<String,Object> updateReference(String file,String id,Map<String,Object> patch,Authentication a,String type){Map<String,Object> before=s.find(file,id);if(before==null)throw new RuntimeException("Record not found");Map<String,Object>x=s.update(file,id,patch);audit.log(a.getName(),"ADMIN","ADMIN_UPDATED_"+type,type,id,before,x);return x;}
}
