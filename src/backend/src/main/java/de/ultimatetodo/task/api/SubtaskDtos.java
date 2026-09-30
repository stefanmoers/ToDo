package de.ultimatetodo.task.api;

import de.ultimatetodo.task.domain.Subtask;
import io.swagger.v3.oas.annotations.media.Schema;
import java.util.UUID;

/** Ein-/Ausgabemodelle der Unteraufgaben-API (US-070/071). */
public final class SubtaskDtos {

	private SubtaskDtos() {
	}

	public record CreateSubtaskRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200) String title) {
	}

	public record RenameSubtaskRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, minLength = 1, maxLength = 200) String title) {
	}

	@Schema(description = "Erledigt-Zustand explizit setzen (nicht nur umschalten), rückgängig änderbar (US-071).")
	public record SetSubtaskDoneRequest(@Schema(requiredMode = Schema.RequiredMode.REQUIRED) boolean done) {
	}

	@Schema(description = "Vertauscht die Position mit dem Vorgänger (UP) oder Nachfolger (DOWN); "
			+ "am jeweiligen Rand wirkungslos (US-070, ohne Ziehbewegung bedienbar).")
	public record MoveSubtaskRequest(
			@Schema(requiredMode = Schema.RequiredMode.REQUIRED, allowableValues = { "UP", "DOWN" }) String direction) {
	}

	public record SubtaskResponse(UUID id, String title, boolean done, int sortOrder) {

		public static SubtaskResponse of(Subtask s) {
			return new SubtaskResponse(s.getId(), s.getTitle(), s.isDone(), s.getSortOrder());
		}

	}

	@Schema(description = "Fortschritt der Unteraufgaben einer Aufgabe (US-071), z. B. für „3 von 5 erledigt“.")
	public record SubtaskProgress(int done, int total) {

		public static final SubtaskProgress NONE = new SubtaskProgress(0, 0);

	}

}
