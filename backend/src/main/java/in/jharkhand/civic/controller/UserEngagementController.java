package in.jharkhand.civic.controller;

import in.jharkhand.civic.service.WorkflowService;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api")
public class UserEngagementController {
    private final WorkflowService w;
    public UserEngagementController(WorkflowService w){this.w=w;}
    private String actor(Authentication a){return a==null?"user-demo":a.getName();}
    @GetMapping("/users/{id}/points") List<Map<String,Object>> points(@PathVariable String id){return w.points(id);}
    @GetMapping("/leaderboard") List<Map<String,Object>> leaderboard(){return w.leaderboard();}
    @GetMapping("/notifications") List<Map<String,Object>> notifications(Authentication a){return w.notifications(actor(a));}
    @PostMapping("/problems/{id}/feedback") Map<String,Object> feedback(@PathVariable String id,@RequestBody Map<String,Object> r,Authentication a){return w.addFeedback(actor(a),id,r);}
    @GetMapping("/problems/{id}/feedback") List<Map<String,Object>> feedback(@PathVariable String id){return w.feedback(id);}
    @GetMapping("/problems/{id}/engagement") List<Map<String,Object>> engagement(@PathVariable String id){return w.engagement(id);}
    @GetMapping("/problems/{id}/engagement/summary") Map<String,Object> engagementSummary(@PathVariable String id){return w.engagementSummary(id);}
    @PostMapping("/problems/{id}/engagement") Map<String,Object> engage(@PathVariable String id,@RequestBody Map<String,Object> r,Authentication a){return w.engage(actor(a),id,String.valueOf(r.getOrDefault("type","LIKE")),String.valueOf(r.getOrDefault("text","")));}
}
