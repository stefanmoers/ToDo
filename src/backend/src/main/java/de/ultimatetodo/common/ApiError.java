package de.ultimatetodo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Einheitliches Fehlerformat der API: stabiler Code, verständliche Meldung, Anfragekennung, optionale Feldfehler. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(String code, String message, String requestId, List<FieldError> fieldErrors) {

	public record FieldError(String field, String code, String message) {
	}

	public static ApiError of(String code, String message) {
		return new ApiError(code, message, org.slf4j.MDC.get(RequestIdFilter.MDC_KEY), List.of());
	}

}
