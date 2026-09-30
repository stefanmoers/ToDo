package de.ultimatetodo.common;

import java.util.List;
import org.springframework.http.HttpStatus;

/** Erwarteter fachlicher Fehler mit stabilem Code; wird zum einheitlichen JSON-Fehler. */
public class ApiException extends RuntimeException {

	private final HttpStatus status;
	private final String code;
	private final List<ApiError.FieldError> fieldErrors;
	private final Long retryAfterSeconds;

	public ApiException(HttpStatus status, String code, String message) {
		this(status, code, message, List.of(), null);
	}

	public ApiException(HttpStatus status, String code, String message, List<ApiError.FieldError> fieldErrors,
			Long retryAfterSeconds) {
		super(message);
		this.status = status;
		this.code = code;
		this.fieldErrors = fieldErrors;
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public static ApiException validation(List<ApiError.FieldError> fieldErrors) {
		return new ApiException(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "Bitte prüfe deine Eingaben.", fieldErrors,
				null);
	}

	public static ApiException tooManyRequests(long retryAfterSeconds) {
		return new ApiException(HttpStatus.TOO_MANY_REQUESTS, "RATE_LIMITED",
				"Zu viele Versuche. Bitte warte einen Moment und versuche es dann erneut.", List.of(), retryAfterSeconds);
	}

	public HttpStatus status() { return status; }

	public String code() { return code; }

	public List<ApiError.FieldError> fieldErrors() { return fieldErrors; }

	public Long retryAfterSeconds() { return retryAfterSeconds; }

}
