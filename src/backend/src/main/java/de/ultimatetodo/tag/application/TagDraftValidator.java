package de.ultimatetodo.tag.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.tag.domain.TagInput;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class TagDraftValidator {

	public ValidTagValues validate(TagDraft draft) {
		List<FieldError> errors = new ArrayList<>();

		String name = TagInput.normalizeName(draft.name());
		if (!TagInput.isValidName(name)) {
			errors.add(new FieldError("name", "INVALID", "Der Tag-Name muss 1 bis 50 Zeichen lang sein."));
		}

		UUID projectId = null;
		if (draft.projectId() != null && !draft.projectId().isBlank()) {
			try {
				projectId = UUID.fromString(draft.projectId());
			}
			catch (IllegalArgumentException e) {
				errors.add(new FieldError("projectId", "INVALID", "Dieses Projekt existiert nicht oder gehört dir nicht."));
			}
		}

		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		return new ValidTagValues(name, projectId);
	}

}
