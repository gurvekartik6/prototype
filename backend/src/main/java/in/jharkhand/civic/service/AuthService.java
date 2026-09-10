package in.jharkhand.civic.service;

import in.jharkhand.civic.repository.JsonStore;
import in.jharkhand.civic.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

@Service
public class AuthService {

 private final JsonStore store;
 private final PasswordEncoder encoder;
 private final JwtService jwt;
 private final IdService ids;

 public AuthService(
         JsonStore store,
         PasswordEncoder encoder,
         JwtService jwt,
         IdService ids
 ) {
  this.store = store;
  this.encoder = encoder;
  this.jwt = jwt;
  this.ids = ids;
 }

 public Map<String, Object> login(
         String email,
         String password
 ) {

  String normalized =
          normalizeEmail(email);

  Map<String, Object> user =
          store.findBy(
                  "users.json",
                  "email",
                  normalized
          );

  if (user == null) {
   throw new IllegalArgumentException(
           "Invalid email or password"
   );
  }

  String passwordHash =
          String.valueOf(
                  user.getOrDefault(
                          "passwordHash",
                          ""
                  )
          );

  if (passwordHash.isBlank()
          || !encoder.matches(
          password,
          passwordHash
  )) {

   throw new IllegalArgumentException(
           "Invalid email or password"
   );
  }

  return createTokenResponse(user);
 }

 public Map<String, Object> register(
         String name,
         String email,
         String password,
         String language
 ) {

  String normalized =
          normalizeEmail(email);

  if (store.findBy(
          "users.json",
          "email",
          normalized
  ) != null) {

   throw new RuntimeException(
           "Email already registered"
   );
  }

  Map<String, Object> user =
          new LinkedHashMap<>();

  user.put(
          "id",
          ids.id("user")
  );

  user.put(
          "name",
          name
  );

  user.put(
          "email",
          normalized
  );

  user.put(
          "passwordHash",
          encoder.encode(password)
  );

  /*
   * Normal citizen accounts are USER accounts.
   * Admin/Department/Institution/Industry accounts
   * should be provisioned separately.
   */
  user.put(
          "role",
          "USER"
  );

  user.put(
          "language",
          language == null || language.isBlank()
                  ? "en"
                  : language
  );

  user.put(
          "civicPoints",
          0
  );

  store.add(
          "users.json",
          user
  );

  return createTokenResponse(user);
 }

 public Map<String, Object> me(
         String id
 ) {

  Map<String, Object> user =
          store.find(
                  "users.json",
                  id
          );

  if (user == null) {
   throw new RuntimeException(
           "User not found"
   );
  }

  Map<String, Object> safeUser =
          new LinkedHashMap<>(user);

  /*
   * Never expose password hashes.
   */
  safeUser.remove(
          "passwordHash"
  );

  return safeUser;
 }

 private String normalizeEmail(
         String email
 ) {

  return email == null
          ? ""
          : email
          .trim()
          .toLowerCase(Locale.ROOT);
 }

 private Map<String, Object> createTokenResponse(
         Map<String, Object> user
 ) {

  String id =
          String.valueOf(
                  user.get("id")
          );

  String email =
          String.valueOf(
                  user.get("email")
          );

  String role =
          String.valueOf(
                  user.getOrDefault(
                          "role",
                          "USER"
                  )
          );

  Map<String, Object> response =
          new LinkedHashMap<>();

  response.put(
          "token",
          jwt.create(
                  id,
                  email,
                  role
          )
  );

  response.put(
          "user",
          me(id)
  );

  return response;
 }
}

