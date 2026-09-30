package de.ultimatetodo.common;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

class RequestIdFilterTest {

	private final RequestIdFilter filter = new RequestIdFilter();

	@Test
	void vergibtKennungWennKeineGesendetWurde() throws Exception {
		MockHttpServletResponse response = run(null);
		assertThat(response.getHeader("X-Request-Id")).isNotBlank().hasSizeGreaterThanOrEqualTo(8);
	}

	@Test
	void uebernimmtHarmloseKennung() throws Exception {
		assertThat(run("abc-12345678").getHeader("X-Request-Id")).isEqualTo("abc-12345678");
	}

	@Test
	void ersetztVerdaechtigeKennungDurchNeue() throws Exception {
		String result = run("x\nFAKE LOG LINE").getHeader("X-Request-Id");
		assertThat(result).doesNotContain("\n").doesNotContain("FAKE");
	}

	private MockHttpServletResponse run(String incoming) throws Exception {
		MockHttpServletRequest request = new MockHttpServletRequest();
		if (incoming != null) {
			request.addHeader("X-Request-Id", incoming);
		}
		MockHttpServletResponse response = new MockHttpServletResponse();
		filter.doFilter(request, response, new MockFilterChain());
		assertThat(org.slf4j.MDC.get("requestId")).isNull();
		return response;
	}

}
