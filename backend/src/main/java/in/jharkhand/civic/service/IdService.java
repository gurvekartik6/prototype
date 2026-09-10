package in.jharkhand.civic.service;
import org.springframework.stereotype.Component; import java.util.UUID;
@Component public class IdService { public String id(String prefix){return prefix+"-"+UUID.randomUUID().toString().substring(0,8);} }
