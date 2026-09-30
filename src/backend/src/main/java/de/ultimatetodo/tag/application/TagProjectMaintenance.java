package de.ultimatetodo.tag.application;

import de.ultimatetodo.tag.persistence.TagRepository;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementiert {@link ProjectTagSupport} losgelöst von {@link TagService} (das selbst {@code ProjectAccess}
 * benötigt): sonst entstünde ein zyklischer Bean-Zugriff, analog zu {@code task.application.TaskProjectMaintenance}.
 */
@Component
class TagProjectMaintenance implements ProjectTagSupport {

	private final TagRepository tags;

	TagProjectMaintenance(TagRepository tags) {
		this.tags = tags;
	}

	@Override
	@Transactional
	public void deleteAllForProject(UUID projectId) {
		tags.deleteByProjectId(projectId);
	}

}
