package de.ultimatetodo.user;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.user.domain.User;
import de.ultimatetodo.user.persistence.UserRepository;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Stream;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-010, 011, 012, 020, 021, 022 (M1-Teil), 141: Konto- und Anmelde-API gegen echtes PostgreSQL, mit echtem CSRF-Ablauf. */
@SpringBootTest(properties = {
		"app.rate-limit.login-attempts=5", "app.rate-limit.register-attempts=100",
		"springdoc.api-docs.enabled=true" })
@AutoConfigureMockMvc
@org.junit.jupiter.api.extension.ExtendWith(org.springframework.boot.test.system.OutputCaptureExtension.class)
@Import(TestcontainersConfiguration.class)
class AuthApiIT {

	static final String PASSWORD = "correct horse battery staple";

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;
	@Autowired UserRepository users;
	@Autowired JdbcTemplate jdbc;

	/** Ein Browser: eine Sitzung mit CSRF-Token wie im echten Ablauf. */
	class Client {
		MockHttpSession session = new MockHttpSession();
		/** Jeder simulierte Browser hat eine eigene Adresse, damit die Ratenbegrenzung je Adresse Tests nicht stört. */
		final String address = "10.%d.%d.%d".formatted(rnd(), rnd(), rnd());
		String header;
		String token;

		Client() throws Exception {
			refreshCsrf();
		}

		void refreshCsrf() throws Exception {
			MvcResult r = mvc.perform(get("/api/v1/auth/csrf").session(session)).andExpect(status().isOk()).andReturn();
			JsonNode body = json.readTree(r.getResponse().getContentAsString());
			header = body.get("headerName").asString();
			token = body.get("token").asString();
			session = (MockHttpSession) r.getRequest().getSession();
		}

		MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder b) {
			return b.with(req -> {
				req.setRemoteAddr(address);
				return req;
			}).session(session).header(header, token).contentType(MediaType.APPLICATION_JSON);
		}

		MvcResult post(String url, String body) throws Exception {
			return mvc.perform(withCsrf(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post(url)).content(body)).andReturn();
		}

		MvcResult login(String email, String password) throws Exception {
			MvcResult r = post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
			if (r.getResponse().getStatus() == 200) {
				session = (MockHttpSession) r.getRequest().getSession();
				refreshCsrf();
			}
			return r;
		}

		int meStatus() throws Exception {
			return mvc.perform(get("/api/v1/auth/me").session(session)).andReturn().getResponse().getStatus();
		}
	}

	static int rnd() {
		return java.util.concurrent.ThreadLocalRandom.current().nextInt(1, 255);
	}

	String newEmail() {
		return "user-" + UUID.randomUUID() + "@example.test";
	}

	String registerBody(String email, String password) {
		return "{\"email\":\"%s\",\"displayName\":\"Test Person\",\"password\":\"%s\",\"timeZone\":\"Europe/Berlin\"}"
			.formatted(email, password);
	}

	String register(Client c, String email) throws Exception {
		MvcResult r = c.post("/api/v1/auth/register", registerBody(email, PASSWORD));
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
		return email;
	}

	@Test
	void registrierungLegtAktivesKontoMitArgon2idAnUndGibtKeineGeheimnisseAus() throws Exception {
		Client c = new Client();
		String email = newEmail().toUpperCase();
		MvcResult r = c.post("/api/v1/auth/register", registerBody("  " + email + " ", PASSWORD));
		assertThat(r.getResponse().getStatus()).isEqualTo(201);
		String body = r.getResponse().getContentAsString();
		assertThat(body).doesNotContain("password").doesNotContain("argon2").doesNotContain(PASSWORD);
		assertThat(body).contains("\"role\":\"USER\"").contains(email.toLowerCase());
		User u = users.findByEmail(email.toLowerCase()).orElseThrow();
		assertThat(u.getPasswordHash()).startsWith("$argon2id$").doesNotContain(PASSWORD);
		assertThat(u.isActive()).isTrue();
	}

	@Test
	void selbstregistrierungKannKeineAdminRolleErzeugen() throws Exception {
		Client c = new Client();
		String email = newEmail();
		MvcResult r = c.post("/api/v1/auth/register",
				"{\"email\":\"%s\",\"displayName\":\"X\",\"password\":\"%s\",\"timeZone\":\"Europe/Berlin\",\"role\":\"ADMIN\",\"status\":\"INACTIVE\"}"
					.formatted(email, PASSWORD));
		assertThat(r.getResponse().getStatus()).isEqualTo(201);
		User u = users.findByEmail(email).orElseThrow();
		assertThat(u.getRole()).isEqualTo(User.Role.USER);
		assertThat(u.isActive()).isTrue();
	}

	@Test
	void ungueltigeEingabenLiefern400MitFeldfehlern() throws Exception {
		Client c = new Client();
		MvcResult r = c.post("/api/v1/auth/register",
				"{\"email\":\"kein-email\",\"displayName\":\"  \",\"password\":\"zu kurz\",\"timeZone\":\"Mars/Base\"}");
		assertThat(r.getResponse().getStatus()).isEqualTo(400);
		JsonNode body = json.readTree(r.getResponse().getContentAsString());
		assertThat(body.get("code").asString()).isEqualTo("VALIDATION_FAILED");
		assertThat(body.get("requestId").asString()).isNotBlank();
		assertThat(body.get("fieldErrors").toString()).contains("email", "displayName", "password", "timeZone");
	}

	@Test
	void passwortGenau14ZeichenIstZuKurz15SindOk() throws Exception {
		Client c = new Client();
		assertThat(c.post("/api/v1/auth/register", registerBody(newEmail(), "a".repeat(14))).getResponse().getStatus())
			.isEqualTo(400);
		assertThat(c.post("/api/v1/auth/register", registerBody(newEmail(), "a".repeat(15))).getResponse().getStatus())
			.isEqualTo(201);
		// Zählung in Unicode-Codepoints: 15 Emoji sind 30 UTF-16-Einheiten, aber erlaubt.
		assertThat(c.post("/api/v1/auth/register", registerBody(newEmail(), "😀".repeat(15))).getResponse().getStatus())
			.isEqualTo(201);
	}

	@Test
	void doppelteEmailWirdAbgewiesenAuchOhneBeachtungDerGrossschreibung() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		MvcResult r = c.post("/api/v1/auth/register", registerBody(email.toUpperCase(), PASSWORD));
		assertThat(r.getResponse().getStatus()).isEqualTo(409);
		assertThat(r.getResponse().getContentAsString()).contains("EMAIL_ALREADY_REGISTERED");
	}

	@Test
	void parallelDoppelteRegistrierungErzeugtGenauEinKonto() throws Exception {
		String email = newEmail();
		int threads = 6;
		ExecutorService pool = Executors.newFixedThreadPool(threads);
		CountDownLatch start = new CountDownLatch(1);
		try {
			java.util.List<Callable<Integer>> tasks = Stream.generate(() -> (Callable<Integer>) () -> {
				Client c = new Client();
				start.await();
				return c.post("/api/v1/auth/register", registerBody(email, PASSWORD)).getResponse().getStatus();
			}).limit(threads).toList();
			java.util.List<Future<Integer>> futures = tasks.stream().map(pool::submit).toList();
			start.countDown();
			int created = 0;
			for (Future<Integer> f : futures) {
				int s = f.get();
				assertThat(s).isIn(201, 409);
				if (s == 201) created++;
			}
			assertThat(created).isEqualTo(1);
			assertThat(jdbc.queryForObject("select count(*) from app_user where email = ?", Integer.class, email)).isEqualTo(1);
		}
		finally {
			pool.shutdownNow();
		}
	}

	@Test
	void schreibendeAnfragenOhneOderMitFalschemCsrfTokenLiefern403() throws Exception {
		Client c = new Client();
		mvc.perform(post("/api/v1/auth/register").session(c.session).contentType(MediaType.APPLICATION_JSON)
			.content(registerBody(newEmail(), PASSWORD)))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		mvc.perform(post("/api/v1/auth/login").session(c.session).header(c.header, "falsch")
			.contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"a@b.de\",\"password\":\"x\"}"))
			.andExpect(status().isForbidden()).andExpect(jsonPath("$.code").value("CSRF_INVALID"));
		mvc.perform(post("/api/v1/auth/logout").session(c.session)).andExpect(status().isForbidden());
	}

	@Test
	void loginErzeugtSitzungMeLiefertEigenesKontoUndOhneAnmeldung401() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		assertThat(c.meStatus()).isEqualTo(401);
		mvc.perform(get("/api/v1/auth/me")).andExpect(status().isUnauthorized())
			.andExpect(jsonPath("$.code").value("UNAUTHENTICATED"));

		String sessionBefore = c.session.getId();
		assertThat(c.login(email, PASSWORD).getResponse().getStatus()).isEqualTo(200);
		assertThat(c.session.getId()).as("Sitzungs-ID wechselt bei Anmeldung").isNotEqualTo(sessionBefore);
		mvc.perform(get("/api/v1/auth/me").session(c.session)).andExpect(status().isOk())
			.andExpect(jsonPath("$.email").value(email)).andExpect(jsonPath("$.passwordHash").doesNotExist());
	}

	@Test
	void fehlerhafteAnmeldungenSindNachAussenNichtUnterscheidbar() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		String inactive = register(c, newEmail());
		jdbc.update("update app_user set status = 'INACTIVE' where email = ?", inactive);

		String[] bodies = {
				c.login(email, "falsches passwort xyz").getResponse().getContentAsString(),
				c.login(newEmail(), PASSWORD).getResponse().getContentAsString(),
				c.login(inactive, PASSWORD).getResponse().getContentAsString() };
		for (String b : bodies) {
			JsonNode n = json.readTree(b);
			assertThat(n.get("code").asString()).isEqualTo("INVALID_CREDENTIALS");
			assertThat(n.get("message").asString()).isEqualTo(json.readTree(bodies[0]).get("message").asString());
		}
	}

	@Test
	void logoutEntwertetSitzungAufDemServerUndEntferntCookie() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		c.login(email, PASSWORD);
		MockHttpSession old = c.session;
		MvcResult r = mvc.perform(c.withCsrf(post("/api/v1/auth/logout"))).andExpect(status().isNoContent()).andReturn();
		Cookie cookie = r.getResponse().getCookie("ULTIMATETODO_SESSION");
		assertThat(cookie).isNotNull();
		assertThat(cookie.getMaxAge()).isZero();
		// Wiederverwendung der alten Sitzung gewährt keinen Zugriff.
		mvc.perform(get("/api/v1/auth/me").session(old)).andExpect(status().isUnauthorized());
	}

	@Test
	void profilNurEigenesUndNurAnzeigenameUndZeitzoneAenderbar() throws Exception {
		Client a = new Client();
		Client b = new Client();
		String emailA = register(a, newEmail());
		String emailB = register(b, newEmail());
		a.login(emailA, PASSWORD);
		b.login(emailB, PASSWORD);

		mvc.perform(a.withCsrf(put("/api/v1/users/me"))
			.content("{\"displayName\":\"  Neuer Name \",\"timeZone\":\"Asia/Tokyo\",\"role\":\"ADMIN\",\"email\":\"x@y.de\"}"))
			.andExpect(status().isOk()).andExpect(jsonPath("$.displayName").value("Neuer Name"))
			.andExpect(jsonPath("$.timeZone").value("Asia/Tokyo")).andExpect(jsonPath("$.email").value(emailA))
			.andExpect(jsonPath("$.role").value("USER"));
		mvc.perform(get("/api/v1/users/me").session(b.session)).andExpect(jsonPath("$.email").value(emailB))
			.andExpect(jsonPath("$.displayName").value("Test Person"));
		mvc.perform(a.withCsrf(put("/api/v1/users/me")).content("{\"displayName\":\"\",\"timeZone\":\"Foo\"}"))
			.andExpect(status().isBadRequest());
		// Ohne Anmeldung: mit gültigem CSRF-Token 401, ohne Token schon 403.
		mvc.perform(new Client().withCsrf(put("/api/v1/users/me")).content("{}")).andExpect(status().isUnauthorized());
		mvc.perform(put("/api/v1/users/me").contentType(MediaType.APPLICATION_JSON).content("{}"))
			.andExpect(status().isForbidden());
	}

	@Test
	void passwortAenderungEntwertetAlleSitzungenUndAltesPasswort() throws Exception {
		Client one = new Client();
		Client two = new Client();
		String email = register(one, newEmail());
		one.login(email, PASSWORD);
		two.login(email, PASSWORD);
		String newPw = "ein ganz neues passwort 42";

		// Falsches bisheriges Passwort ändert nichts.
		mvc.perform(one.withCsrf(post("/api/v1/users/me/password"))
			.content("{\"currentPassword\":\"falsch falsch falsch\",\"newPassword\":\"%s\"}".formatted(newPw)))
			.andExpect(status().isBadRequest()).andExpect(jsonPath("$.fieldErrors[0].field").value("currentPassword"));
		assertThat(one.meStatus()).isEqualTo(200);

		mvc.perform(one.withCsrf(post("/api/v1/users/me/password"))
			.content("{\"currentPassword\":\"%s\",\"newPassword\":\"%s\"}".formatted(PASSWORD, newPw)))
			.andExpect(status().isNoContent());

		assertThat(one.meStatus()).isEqualTo(401);
		assertThat(two.meStatus()).as("auch parallele Sitzung ist entwertet").isEqualTo(401);
		Client fresh = new Client();
		assertThat(fresh.login(email, PASSWORD).getResponse().getStatus()).isEqualTo(401);
		assertThat(fresh.login(email, newPw).getResponse().getStatus()).isEqualTo(200);
	}

	@Test
	void deaktiviertesKontoVerliertBestehendeSitzungBeiNaechsterAnfrage() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		c.login(email, PASSWORD);
		assertThat(c.meStatus()).isEqualTo(200);
		jdbc.update("update app_user set status = 'INACTIVE' where email = ?", email);
		assertThat(c.meStatus()).isEqualTo(401);
		jdbc.update("update app_user set status = 'ACTIVE' where email = ?", email);
		assertThat(c.meStatus()).as("Reaktivierung stellt alte Sitzung nicht wieder her").isEqualTo(401);
	}

	@Test
	void ratenbegrenzungLiefert429NachZuVielenVersuchenAberKeineDauersperre() throws Exception {
		Client c = new Client();
		String email = register(c, newEmail());
		int last = 0;
		for (int i = 0; i < 8; i++) {
			last = c.login(email, "falsch falsch falsch").getResponse().getStatus();
		}
		assertThat(last).isEqualTo(429);
		MvcResult r = c.login(email, PASSWORD);
		assertThat(r.getResponse().getStatus()).isEqualTo(429);
		assertThat(r.getResponse().getHeader("Retry-After")).isNotBlank();
	}

	@Test
	void passwoerterUndCookiesTauchenNichtInDenLogsAuf(org.springframework.boot.test.system.CapturedOutput output)
			throws Exception {
		Client c = new Client();
		String secret = "geheimes-log-testpasswort-0815";
		String email = newEmail();
		assertThat(c.post("/api/v1/auth/register", registerBody(email, secret)).getResponse().getStatus()).isEqualTo(201);
		c.login(email, secret);
		c.login(email, "falsches-log-testpasswort-4711");
		c.post("/api/v1/auth/register", "{kaputt");
		assertThat(output.getAll()).doesNotContain(secret).doesNotContain("falsches-log-testpasswort-4711")
			.doesNotContain(c.token);
	}

	@Test
	void csrfAntwortWirdNichtGecacht() throws Exception {
		mvc.perform(get("/api/v1/auth/csrf")).andExpect(status().isOk())
			.andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Cache-Control",
					org.hamcrest.Matchers.containsString("no-store")));
	}

	@Test
	void openApiBeschreibtDieAuthEndpunkte() throws Exception {
		MvcResult r = mvc.perform(get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
		assertThat(r.getResponse().getContentAsString()).contains("/api/v1/auth/login", "/api/v1/auth/register",
				"/api/v1/users/me/password").doesNotContain("passwordHash");
	}

}
