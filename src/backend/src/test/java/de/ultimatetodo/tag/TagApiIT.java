package de.ultimatetodo.tag;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import de.ultimatetodo.TestcontainersConfiguration;
import de.ultimatetodo.support.ApiTestClient;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/** US-043, US-181: Tag-API gegen echtes PostgreSQL. */
@SpringBootTest(properties = { "springdoc.api-docs.enabled=true" })
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class TagApiIT {

	@Autowired MockMvc mvc;
	@Autowired JsonMapper json;

	ApiTestClient client() throws Exception {
		return new ApiTestClient(mvc, json);
	}

	ApiTestClient loggedIn() throws Exception {
		ApiTestClient c = client();
		c.registerAndLogin(ApiTestClient.newEmail());
		return c;
	}

	JsonNode tree(MvcResult r) throws Exception {
		return json.readTree(r.getResponse().getContentAsString());
	}

	JsonNode me(ApiTestClient c) throws Exception {
		return tree(c.get("/api/v1/auth/me"));
	}

	String createProject(ApiTestClient owner, String name) throws Exception {
		return tree(owner.post("/api/v1/projects", "{\"name\":\"%s\"}".formatted(name))).get("id").asString();
	}

	void addMember(ApiTestClient owner, String projectId, ApiTestClient member) throws Exception {
		MvcResult r = owner.post("/api/v1/projects/" + projectId + "/members",
				"{\"email\":\"%s\"}".formatted(me(member).get("email").asString()));
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
	}

	List<String> ids(JsonNode list) {
		List<String> ids = new ArrayList<>();
		list.forEach(n -> ids.add(n.get("id").asString()));
		return ids;
	}

	@Test
	void legtPersoenlichenTagAn() throws Exception {
		ApiTestClient a = loggedIn();
		MvcResult r = a.post("/api/v1/tags", "{\"name\":\"Zuhause\"}");
		assertThat(r.getResponse().getStatus()).as(r.getResponse().getContentAsString()).isEqualTo(201);
		JsonNode body = tree(r);
		assertThat(body.get("scope").asString()).isEqualTo("PERSONAL");
		assertThat(body.has("projectId")).isFalse();
	}

	@Test
	void legtProjektTagAn() throws Exception {
		ApiTestClient a = loggedIn();
		String projectId = tree(a.post("/api/v1/projects", "{\"name\":\"Umzug\"}")).get("id").asString();
		MvcResult r = a.post("/api/v1/tags", "{\"name\":\"Eilig\",\"projectId\":\"%s\"}".formatted(projectId));
		assertThat(r.getResponse().getStatus()).isEqualTo(201);
		JsonNode body = tree(r);
		assertThat(body.get("scope").asString()).isEqualTo("PROJECT");
		assertThat(body.get("projectId").asString()).isEqualTo(projectId);
	}

	@Test
	void tagFuerFremdesOderNichtExistierendesProjektWirdAbgewiesen() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String othersProjectId = tree(b.post("/api/v1/projects", "{\"name\":\"Projekt von B\"}")).get("id").asString();

		MvcResult r1 = a.post("/api/v1/tags", "{\"name\":\"X\",\"projectId\":\"%s\"}".formatted(othersProjectId));
		assertThat(r1.getResponse().getStatus()).isEqualTo(400);
		assertThat(tree(r1).get("fieldErrors").toString()).contains("projectId");
	}

	@Test
	void validierungLiefertFeldfehler() throws Exception {
		ApiTestClient a = loggedIn();
		assertThat(a.post("/api/v1/tags", "{\"name\":\"\"}").getResponse().getStatus()).isEqualTo(400);
		assertThat(a.post("/api/v1/tags", "{\"name\":\"%s\"}".formatted("x".repeat(51))).getResponse().getStatus())
			.isEqualTo(400);
	}

	@Test
	void gleicherNameImSelbenGeltungsbereichIstEinKonfliktOhneBeachtungDerGrossschreibung() throws Exception {
		ApiTestClient a = loggedIn();
		assertThat(a.post("/api/v1/tags", "{\"name\":\"Wichtig\"}").getResponse().getStatus()).isEqualTo(201);
		MvcResult conflict = a.post("/api/v1/tags", "{\"name\":\"WICHTIG\"}");
		assertThat(conflict.getResponse().getStatus()).isEqualTo(409);
		assertThat(tree(conflict).get("code").asString()).isEqualTo("TAG_ALREADY_EXISTS");
	}

	@Test
	void gleicherNameInUnterschiedlichenGeltungsbereichenIstKeinKonflikt() throws Exception {
		ApiTestClient a = loggedIn();
		String projectId = tree(a.post("/api/v1/projects", "{\"name\":\"P\"}")).get("id").asString();
		assertThat(a.post("/api/v1/tags", "{\"name\":\"Dringend\"}").getResponse().getStatus()).isEqualTo(201);
		assertThat(a.post("/api/v1/tags", "{\"name\":\"Dringend\",\"projectId\":\"%s\"}".formatted(projectId))
			.getResponse().getStatus()).isEqualTo(201);
	}

	@Test
	void listeZeigtNurEigeneTags() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		a.post("/api/v1/tags", "{\"name\":\"A1\"}");
		a.post("/api/v1/tags", "{\"name\":\"A2\"}");
		b.post("/api/v1/tags", "{\"name\":\"B1\"}");

		assertThat(tree(a.get("/api/v1/tags")).size()).isEqualTo(2);
		assertThat(tree(b.get("/api/v1/tags")).size()).isEqualTo(1);
	}

	@Test
	void ownershipMitZweiBenutzernFremderTagIstUeberallNichtGefunden() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String id = tree(a.post("/api/v1/tags", "{\"name\":\"Geheim\"}")).get("id").asString();

		assertThat(b.put("/api/v1/tags/" + id, "{\"name\":\"X\"}").getResponse().getStatus()).isEqualTo(404);
		assertThat(b.delete("/api/v1/tags/" + id).getResponse().getStatus()).isEqualTo(404);
	}

	@Test
	void umbenennenUndLoeschen() throws Exception {
		ApiTestClient a = loggedIn();
		String id = tree(a.post("/api/v1/tags", "{\"name\":\"Alt\"}")).get("id").asString();

		MvcResult renamed = a.put("/api/v1/tags/" + id, "{\"name\":\"Neu\"}");
		assertThat(renamed.getResponse().getStatus()).isEqualTo(200);
		assertThat(tree(renamed).get("name").asString()).isEqualTo("Neu");

		assertThat(a.delete("/api/v1/tags/" + id).getResponse().getStatus()).isEqualTo(204);
		assertThat(tree(a.get("/api/v1/tags")).size()).isZero();
	}

	@Test
	void umbenennenAufBestehendenNamenIstEinKonflikt() throws Exception {
		ApiTestClient a = loggedIn();
		a.post("/api/v1/tags", "{\"name\":\"Eins\"}");
		String id = tree(a.post("/api/v1/tags", "{\"name\":\"Zwei\"}")).get("id").asString();

		assertThat(a.put("/api/v1/tags/" + id, "{\"name\":\"Eins\"}").getResponse().getStatus()).isEqualTo(409);
	}

	@Test
	void ohneAnmeldungIstDieApiNichtErreichbar() throws Exception {
		mvc.perform(MockMvcRequestBuilders.get("/api/v1/tags")).andExpect(status().isUnauthorized());
	}

	@Test
	void openApiBeschreibtDieTagEndpunkte() throws Exception {
		MvcResult r = mvc.perform(MockMvcRequestBuilders.get("/v3/api-docs")).andExpect(status().isOk()).andReturn();
		assertThat(r.getResponse().getContentAsString()).contains("/api/v1/tags");
	}

	@Test
	void gleichnamigeTagsAusVerschiedenenGeltungsbereichenTragIhreHerkunft() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String projectA = createProject(a, "Operation Kaffee");
		String projectB = createProject(b, "Operation Kaffee");
		String tagA = tree(a.post("/api/v1/tags", "{\"name\":\"Backen\",\"projectId\":\"%s\"}".formatted(projectA)))
			.get("id").asString();
		String tagB = tree(b.post("/api/v1/tags", "{\"name\":\"Backen\",\"projectId\":\"%s\"}".formatted(projectB)))
			.get("id").asString();
		String personalB = tree(b.post("/api/v1/tags", "{\"name\":\"Backen\"}")).get("id").asString();
		addMember(a, projectA, b);

		JsonNode list = tree(b.get("/api/v1/tags"));

		assertThat(list.size()).isEqualTo(3);
		assertThat(list.get(0).get("id").asString()).as("persönlich steht vor Projekt-Tags").isEqualTo(personalB);
		assertThat(list.get(0).get("scope").asString()).isEqualTo("PERSONAL");
		assertThat(list.get(0).has("projectName")).isFalse();
		assertThat(list.get(0).has("projectOwnerId")).isFalse();
		assertThat(list.get(0).has("projectOwnerDisplayName")).isFalse();
		for (int i = 1; i <= 2; i++) {
			JsonNode t = list.get(i);
			assertThat(t.get("scope").asString()).isEqualTo("PROJECT");
			assertThat(t.get("projectName").asString()).isEqualTo("Operation Kaffee");
			assertThat(t.get("projectOwnerDisplayName").asString()).isEqualTo("Test Person");
			String expectedOwner = t.get("id").asString().equals(tagA) ? me(a).get("id").asString()
					: me(b).get("id").asString();
			assertThat(t.get("id").asString()).isIn(tagA, tagB);
			assertThat(t.get("projectOwnerId").asString()).as("Eigentümer des jeweiligen Projekts").isEqualTo(expectedOwner);
		}
		assertThat(list.get(1).get("projectOwnerId").asString()).isNotEqualTo(list.get(2).get("projectOwnerId").asString());
	}

	@Test
	void reihenfolgeIstNachNameUndUnabhaengigVonGrossschreibungStabil() throws Exception {
		ApiTestClient a = loggedIn();
		for (String name : List.of("banane", "Apfel", "Zitrone", "Ärmel")) {
			assertThat(a.post("/api/v1/tags", "{\"name\":\"%s\"}".formatted(name)).getResponse().getStatus())
				.isEqualTo(201);
		}

		JsonNode first = tree(a.get("/api/v1/tags"));
		List<String> names = new ArrayList<>();
		first.forEach(n -> names.add(n.get("name").asString()));

		assertThat(names).containsExactly("Apfel", "Ärmel", "banane", "Zitrone");
		assertThat(ids(tree(a.get("/api/v1/tags")))).as("wiederholte Abfrage liefert dieselbe Reihenfolge")
			.isEqualTo(ids(first));
	}

	@Test
	void anlegenUndUmbenennenLiefernDieHerkunftDesProjektTags() throws Exception {
		ApiTestClient a = loggedIn();
		String projectId = createProject(a, "Umzug");

		MvcResult created = a.post("/api/v1/tags", "{\"name\":\"Eilig\",\"projectId\":\"%s\"}".formatted(projectId));
		JsonNode body = tree(created);
		assertThat(body.get("projectName").asString()).isEqualTo("Umzug");
		assertThat(body.get("projectOwnerId").asString()).isEqualTo(me(a).get("id").asString());
		assertThat(body.get("projectOwnerDisplayName").asString()).isEqualTo("Test Person");

		JsonNode renamed = tree(a.put("/api/v1/tags/" + body.get("id").asString(), "{\"name\":\"Sehr eilig\"}"));
		assertThat(renamed.get("name").asString()).isEqualTo("Sehr eilig");
		assertThat(renamed.get("projectName").asString()).isEqualTo("Umzug");
	}

	@Test
	void nachEntfernenAusDemProjektVerschwindenDessenTagsAusDerListe() throws Exception {
		ApiTestClient a = loggedIn();
		ApiTestClient b = loggedIn();
		String projectA = createProject(a, "Geteilt");
		a.post("/api/v1/tags", "{\"name\":\"Nur im Projekt\",\"projectId\":\"%s\"}".formatted(projectA));
		addMember(a, projectA, b);
		assertThat(tree(b.get("/api/v1/tags")).size()).isEqualTo(1);

		String memberId = me(b).get("id").asString();
		assertThat(a.delete("/api/v1/projects/" + projectA + "/members/" + memberId).getResponse().getStatus())
			.isEqualTo(204);

		assertThat(tree(b.get("/api/v1/tags")).size()).as("kein Hinweis auf fremde Projekte ohne Zugriff").isZero();
	}

}
