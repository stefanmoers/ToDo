package de.ultimatetodo.support;

import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * Simulierter Browser mit echter Sitzung und CSRF-Ablauf für Integrationstests neuer Module (Muster aus
 * {@code user.AuthApiIT.Client}, hier wiederverwendbar gemacht, ohne den M1a-Test selbst zu ändern).
 */
public class ApiTestClient {

	public static final String PASSWORD = "correct horse battery staple";

	private final MockMvc mvc;
	private final JsonMapper json;

	public MockHttpSession session = new MockHttpSession();
	/** Eigene Adresse je Client, damit die Ratenbegrenzung je Adresse Tests nicht gegenseitig stört. */
	public final String address = "10.%d.%d.%d".formatted(rnd(), rnd(), rnd());
	public String header;
	public String token;

	public ApiTestClient(MockMvc mvc, JsonMapper json) throws Exception {
		this.mvc = mvc;
		this.json = json;
		refreshCsrf();
	}

	public void refreshCsrf() throws Exception {
		MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/api/v1/auth/csrf").session(session)).andReturn();
		JsonNode body = json.readTree(r.getResponse().getContentAsString());
		header = body.get("headerName").asString();
		token = body.get("token").asString();
		session = (MockHttpSession) r.getRequest().getSession();
	}

	public MockHttpServletRequestBuilder withCsrf(MockHttpServletRequestBuilder b) {
		return b.with(req -> {
			req.setRemoteAddr(address);
			return req;
		}).session(session).header(header, token).contentType(MediaType.APPLICATION_JSON);
	}

	public MvcResult get(String url) throws Exception {
		return mvc.perform(MockMvcRequestBuilders.get(url).session(session)).andReturn();
	}

	public MvcResult post(String url, String body) throws Exception {
		return mvc.perform(withCsrf(MockMvcRequestBuilders.post(url)).content(body)).andReturn();
	}

	public MvcResult put(String url, String body) throws Exception {
		return mvc.perform(withCsrf(MockMvcRequestBuilders.put(url)).content(body)).andReturn();
	}

	public MvcResult delete(String url) throws Exception {
		return mvc.perform(withCsrf(MockMvcRequestBuilders.delete(url))).andReturn();
	}

	public MvcResult login(String email, String password) throws Exception {
		MvcResult r = post("/api/v1/auth/login", "{\"email\":\"%s\",\"password\":\"%s\"}".formatted(email, password));
		if (r.getResponse().getStatus() == 200) {
			session = (MockHttpSession) r.getRequest().getSession();
			refreshCsrf();
		}
		return r;
	}

	/** Registriert ein neues Konto und meldet sich sofort an; wirft, wenn die Registrierung fehlschlägt. */
	public String registerAndLogin(String email) throws Exception {
		MvcResult r = post("/api/v1/auth/register",
				"{\"email\":\"%s\",\"displayName\":\"Test Person\",\"password\":\"%s\",\"timeZone\":\"Europe/Berlin\"}"
					.formatted(email, PASSWORD));
		if (r.getResponse().getStatus() != 201) {
			throw new IllegalStateException("Registrierung fehlgeschlagen: " + r.getResponse().getContentAsString());
		}
		login(email, PASSWORD);
		return email;
	}

	public static String newEmail() {
		return "user-" + UUID.randomUUID() + "@example.test";
	}

	static int rnd() {
		return ThreadLocalRandom.current().nextInt(1, 255);
	}

}
