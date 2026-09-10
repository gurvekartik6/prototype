package in.jharkhand.civic.service;
import in.jharkhand.civic.repository.JsonStore; import org.junit.jupiter.api.Test; import org.mockito.Mockito; import java.util.*; import static org.junit.jupiter.api.Assertions.*;
class DuplicateDetectionServiceTest {
 @Test void detectsSimilarProblem(){JsonStore store=Mockito.mock(JsonStore.class);Mockito.when(store.read("problems.json")).thenReturn(List.of(Map.of("id","p2","title","Contaminated water village","description","Water contamination near mine")));var svc=new DuplicateDetectionService(store);var r=svc.detect(Map.of("id","p1","title","Village water contamination","description","Contaminated water near mining area"));assertEquals(Boolean.TRUE,r.get("isPotentialDuplicate"));}
}
