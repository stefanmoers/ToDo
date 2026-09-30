package de.ultimatetodo.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import java.util.List;

/** Einheitliches Fehlerformat der API: Status, stabiler Code, deutsche Meldung, Anfragekennung, optionale Feldfehler. */
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public record ApiError(int status, String code, String message, String requestId, List<FieldError> fieldErrors) {

	public record FieldError(String field, String code, String message) {
	}

	public static ApiError of(int status, String code, String message) {
		return of(status, code, message, List.of());
	}

	public static ApiError of(int status, String code, String message, List<FieldError> fieldErrors) {
		return new ApiError(status, code, message, org.slf4j.MDC.get(RequestIdFilter.MDC_KEY), fieldErrors);
	}

}
