package de.ultimatetodo.project.application;

import de.ultimatetodo.common.ApiError.FieldError;
import de.ultimatetodo.common.ApiException;
import de.ultimatetodo.project.domain.ProjectInput;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class ProjectDraftValidator {

	public ValidProjectValues validate(ProjectDraft draft) {
		List<FieldError> errors = new ArrayList<>();

		String name = ProjectInput.normalizeName(draft.name());
		if (!ProjectInput.isValidName(name)) {
			errors.add(new FieldError("name", "INVALID", "Der Projektname muss 1 bis 100 Zeichen lang sein."));
		}

		String description = ProjectInput.normalizeDescription(draft.description());
		if (!ProjectInput.isValidDescription(description)) {
			errors.add(
					new FieldError("description", "TOO_LONG", "Die Beschreibung darf höchstens 10.000 Zeichen lang sein."));
		}

		if (!errors.isEmpty()) {
			throw ApiException.validation(errors);
		}
		return new ValidProjectValues(name, description);
	}

}
