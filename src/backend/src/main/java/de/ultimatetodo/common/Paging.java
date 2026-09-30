package de.ultimatetodo.common;

import de.ultimatetodo.common.ApiError.FieldError;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;

/**
 * Paginierung nach ADR-005: Seite ab 0, Standardgröße 25, Höchstgröße 100. Ungültige Werte werden abgewiesen
 * (400), nicht stillschweigend gekappt.
 */
public final class Paging {

	public static final int DEFAULT_SIZE = 25;
	public static final int MAX_SIZE = 100;

	private Paging() {
	}

	public static PageRequest of(Integer page, Integer size, Sort sort) {
		int p = page == null ? 0 : page;
		int s = size == null ? DEFAULT_SIZE : size;
		List<FieldError> errors = new ArrayList<>();
		if (p < 0) {
			errors.add(new FieldError("page", "INVALID", "Die Seite muss 0 oder größer sein."));
		}
		if (s < 1 || s > MAX_SIZE) {
			errors.add(new FieldError("size", "INVALID", "Die Seitengröße muss zwischen 1 und " + MAX_SIZE + " liegen."));
		}
		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		return PageRequest.of(p, s, sort);
	}

}
