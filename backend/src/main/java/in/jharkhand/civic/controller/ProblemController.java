package in.jharkhand.civic.controller;
import in.jharkhand.civic.service.ProblemService; import com.fasterxml.jackson.databind.ObjectMapper; import org.springframework.security.core.Authentication; import org.springframework.security.access.prepost.PreAuthorize; import org.springframework.web.bind.annotation.*; import org.springframework.web.multipart.MultipartFile; import java.util.*;
@RestController @RequestMapping("/api/problems") public class ProblemController {private final ProblemService ps;private final ObjectMapper mapper;public ProblemController(ProblemService p,ObjectMapper m){ps=p;mapper=m;} private String role(Authentication a){return a==null?"USER":a.getAuthorities().stream().map(x->x.getAuthority().replace("ROLE_","")).findFirst().orElse("USER");}
 @GetMapping public List<Map<String,Object>> list(Authentication a){return ps.list(a.getName(),role(a));}
 @PostMapping(consumes="multipart/form-data") public Map<String,Object> create(@RequestPart("payload") String payload,@RequestPart(value="files",required=false) List<MultipartFile> files,Authentication a)throws Exception{return ps.create(a.getName(),mapper.readValue(payload,Map.class),files);}
 @GetMapping("/{id}") public Map<String,Object> get(@PathVariable String id,Authentication a){return ps.get(id,a.getName(),role(a));}
 @PostMapping("/{id}/analyze") public Map<String,Object> analyze(@PathVariable String id,Authentication a){return ps.analyze(id,a.getName(),role(a));}
 @GetMapping("/{id}/analysis") public Map<String,Object> analysis(@PathVariable String id){return Optional.ofNullable(ps.analysis(id)).orElse(Map.of("status","PENDING"));}
 @PostMapping("/{id}/analyze/retry") public Map<String,Object> retry(@PathVariable String id,Authentication a){return ps.analyze(id,a.getName(),role(a));}
 @GetMapping("/{id}/routing") public Map<String,Object> routing(@PathVariable String id){return Optional.ofNullable(ps.routing(id)).orElse(Map.of("status","NOT_ROUTED"));}
 @PostMapping("/{id}/route") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> route(@PathVariable String id,Authentication a){return ps.route(id,a.getName(),role(a));}
 @PostMapping("/routing/{routeId}/approve") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> approveRoute(@PathVariable String routeId,Authentication a){return ps.approveRouting(routeId,a.getName(),role(a));}
 @PostMapping("/routing/{routeId}/reject") @PreAuthorize("hasRole('ADMIN')") public Map<String,Object> rejectRoute(@PathVariable String routeId){return Map.of("message","Routing rejection recorded for review","routeId",routeId);}
}
