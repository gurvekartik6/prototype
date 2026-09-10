package in.jharkhand.civic.exception;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {
  @ExceptionHandler(MethodArgumentNotValidException.class)
  ResponseEntity<Map<String,Object>> validation(MethodArgumentNotValidException e, HttpServletRequest r) {
    String message=e.getBindingResult().getFieldErrors().stream().map(x->x.getField()+": "+x.getDefaultMessage()).findFirst().orElse("Validation failed");
    return body(400,"ValidationError",message,r);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  ResponseEntity<Map<String,Object>> constraint(ConstraintViolationException e,HttpServletRequest r){return body(400,"ValidationError",e.getMessage(),r);}

  @ExceptionHandler(java.util.NoSuchElementException.class)
  ResponseEntity<Map<String,Object>> notFound(Exception e,HttpServletRequest r){return body(404,"NotFound",e.getMessage()==null?"Resource not found":e.getMessage(),r);}

  @ExceptionHandler(org.springframework.security.access.AccessDeniedException.class)
  ResponseEntity<Map<String,Object>> forbidden(Exception e,HttpServletRequest r){return body(403,"Forbidden","You do not have permission for this action.",r);}

  @ExceptionHandler(IllegalArgumentException.class)
  ResponseEntity<Map<String,Object>> badRequest(Exception e,HttpServletRequest r){return body(400,"BadRequest",e.getMessage(),r);}

  @ExceptionHandler(Exception.class)
  ResponseEntity<Map<String,Object>> server(Exception e,HttpServletRequest r){return body(500,"InternalServerError","The server could not process the request.",r);}

  private ResponseEntity<Map<String,Object>> body(int status,String error,String message,HttpServletRequest r){Map<String,Object>x=new LinkedHashMap<>();x.put("timestamp",Instant.now().toString());x.put("status",status);x.put("error",error);x.put("message",message==null?"Request could not be processed":message);x.put("path",r.getRequestURI());return ResponseEntity.status(HttpStatus.valueOf(status)).body(x);}
}
