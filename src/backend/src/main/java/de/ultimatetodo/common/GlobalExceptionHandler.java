package de.ultimatetodo.common;

import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/** Übersetzt Ausnahmen in das einheitliche Fehlerformat. Keine Stacktraces oder Interna in Antworten. */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(ApiException.class)
	ResponseEntity<ApiError> expected(ApiException ex) {
		ResponseEntity.BodyBuilder builder = ResponseEntity.status(ex.status());
		if (ex.retryAfterSeconds() != null) {
			builder.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()));
		}
		return builder.body(ApiError.of(ex.status().value(), ex.code(), ex.getMessage(), ex.fieldErrors()));
	}

	@ExceptionHandler(MethodArgumentNotValidException.class)
	ResponseEntity<ApiError> invalid(MethodArgumentNotValidException ex) {
		List<ApiError.FieldError> fields = ex.getBindingResult().getFieldErrors().stream()
			.map(e -> new ApiError.FieldError(e.getField(), e.getCode(), e.getDefaultMessage()))
			.toList();
		return ResponseEntity.badRequest()
			.body(ApiError.of(400, "VALIDATION_FAILED", "Bitte prüfe deine Eingaben.", fields));
	}

	@ExceptionHandler(HttpMessageNotReadableException.class)
	ResponseEntity<ApiError> unreadable(HttpMessageNotReadableException ex) {
		return ResponseEntity.badRequest()
			.body(ApiError.of(400, "MALFORMED_REQUEST", "Die Anfrage konnte nicht gelesen werden."));
	}

	@ExceptionHandler(HttpRequestMethodNotSupportedException.class)
	ResponseEntity<ApiError> methodNotAllowed(HttpRequestMethodNotSupportedException ex) {
		return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
			.body(ApiError.of(405, "METHOD_NOT_ALLOWED", "Diese Methode ist hier nicht erlaubt."));
	}

	@ExceptionHandler(NoResourceFoundException.class)
	ResponseEntity<ApiError> notFound(NoResourceFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND)
			.body(ApiError.of(404, "NOT_FOUND", "Die Ressource wurde nicht gefunden."));
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		// Unerwartete Systemfehler: nur Typ und Stacktrace ins Log, keine Anfragedaten, nichts in die Antwort.
		log.error("Unerwarteter Fehler ({})", ex.getClass().getName(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiError.of(500, "INTERNAL_ERROR", "Es ist ein unerwarteter Fehler aufgetreten. Bitte später erneut versuchen."));
	}

}
