package in.jharkhand.civic.port; import java.util.*; public interface ProblemRepository { List<Map<String,Object>> findAll(); Optional<Map<String,Object>> findById(String id); }
