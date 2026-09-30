package de.ultimatetodo.common;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Übersetzt Ausnahmen in das einheitliche Fehlerformat. Keine Stacktraces oder Interna in Antworten. */
@RestControllerAdvice
class GlobalExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(Exception.class)
	ResponseEntity<ApiError> unexpected(Exception ex) {
		// Unerwartete Systemfehler: nur Typ protokollieren, keine Anfragedaten.
		log.error("Unerwarteter Fehler ({})", ex.getClass().getName(), ex);
		return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
			.body(ApiError.of("INTERNAL_ERROR", "Es ist ein unerwarteter Fehler aufgetreten. Bitte später erneut versuchen."));
	}

}
