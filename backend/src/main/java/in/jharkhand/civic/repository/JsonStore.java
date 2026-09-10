package in.jharkhand.civic.repository;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class JsonStore {
  private final ObjectMapper mapper; private final Path dir; private final Map<String,Object> locks=new ConcurrentHashMap<>();
  public JsonStore(ObjectMapper mapper,@Value("${app.data-dir:./data}") String dataDir){this.mapper=mapper;this.dir=Paths.get(dataDir);try{Files.createDirectories(dir);}catch(IOException e){throw new IllegalStateException(e);}}
  private Object lock(String f){return locks.computeIfAbsent(f,k->new Object());}
  public List<Map<String,Object>> read(String file){synchronized(lock(file)){return readUnlocked(file);}}
  public void write(String file,List<Map<String,Object>> rows){synchronized(lock(file)){writeUnlocked(file,rows);}}
  public Map<String,Object> find(String file,String id){return read(file).stream().filter(x->id.equals(String.valueOf(x.get("id")))).findFirst().orElse(null);}
  public Map<String,Object> findBy(String file,String key,String value){return read(file).stream().filter(x->value.equals(String.valueOf(x.get(key)))).findFirst().orElse(null);}
  public Path dataDir(){return dir;}
  public Map<String,Object> add(String file,Map<String,Object> row){synchronized(lock(file)){List<Map<String,Object>> rows=readUnlocked(file);rows.add(row);writeUnlocked(file,rows);return row;}}
  public Map<String,Object> update(String file,String id,Map<String,Object> patch){synchronized(lock(file)){List<Map<String,Object>> rows=readUnlocked(file); for(int i=0;i<rows.size();i++){if(id.equals(String.valueOf(rows.get(i).get("id")))){Map<String,Object> m=new LinkedHashMap<>(rows.get(i));m.putAll(patch);rows.set(i,m);writeUnlocked(file,rows);return m;}} return null;}}
  private List<Map<String,Object>> readUnlocked(String file){Path p=dir.resolve(file);if(!Files.exists(p))return new ArrayList<>();try{return mapper.readValue(Files.readString(p),new TypeReference<>(){});}catch(Exception e){throw new IllegalStateException("Invalid JSON: "+file,e);}}
  private void writeUnlocked(String file,List<Map<String,Object>> rows){Path p=dir.resolve(file);try{Files.createDirectories(p.getParent());Path tmp=Files.createTempFile(dir,".tmp-",".json");Files.writeString(tmp,mapper.writerWithDefaultPrettyPrinter().writeValueAsString(rows));try{Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING,StandardCopyOption.ATOMIC_MOVE);}catch(AtomicMoveNotSupportedException e){Files.move(tmp,p,StandardCopyOption.REPLACE_EXISTING);}}catch(Exception e){throw new IllegalStateException("Unable to write "+file,e);}}
}
