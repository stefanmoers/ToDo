package de.ultimatetodo.user.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/** Benutzerkonto. Passwort-Hash und Rolle verlassen das Backend nie über die API. */
@Entity
@Table(name = "app_user")
public class User {

	public enum Status { ACTIVE, INACTIVE }

	public enum Role { USER, ADMIN }

	@Id
	private UUID id;

	@Column(nullable = false, updatable = false)
	private String email;

	@Column(name = "display_name", nullable = false)
	private String displayName;

	@Column(name = "password_hash", nullable = false)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false)
	private Status status;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, updatable = false)
	private Role role;

	@Column(name = "time_zone", nullable = false)
	private String timeZone;

	@Column(name = "auth_epoch", nullable = false)
	private int authEpoch;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	@Version
	private long version;

	protected User() {
	}

	public static User register(String email, String displayName, String passwordHash, String timeZone, Instant now) {
		User u = new User();
		u.id = UUID.randomUUID();
		u.email = email;
		u.displayName = displayName;
		u.passwordHash = passwordHash;
		u.status = Status.ACTIVE;
		// Selbstregistrierung erzeugt ausschließlich die Rolle USER (US-010).
		u.role = Role.USER;
		u.timeZone = timeZone;
		u.createdAt = now;
		u.updatedAt = now;
		return u;
	}

	public void updateProfile(String displayName, String timeZone, Instant now) {
		this.displayName = displayName;
		this.timeZone = timeZone;
		this.updatedAt = now;
	}

	/** Setzt ein neues Passwort und entwertet damit alle bestehenden Sitzungen. */
	public void changePassword(String newHash, Instant now) {
		this.passwordHash = newHash;
		this.authEpoch++;
		this.updatedAt = now;
	}

	public UUID getId() { return id; }

	public String getEmail() { return email; }

	public String getDisplayName() { return displayName; }

	public String getPasswordHash() { return passwordHash; }

	public Status getStatus() { return status; }

	public Role getRole() { return role; }

	public String getTimeZone() { return timeZone; }

	public int getAuthEpoch() { return authEpoch; }

	public Instant getCreatedAt() { return createdAt; }

	public boolean isActive() { return status == Status.ACTIVE; }

}
