package com.equadis.portal.modules.persona.domain;

import org.hibernate.annotations.Immutable;

import com.equadis.portal.shared.actor.ActorRole;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Reference data: one row per persona type (ADMIN, SUPPLIER, RETAILER). Managed by Flyway only. */
@Entity
@Immutable
@Table(name = "persona")
public class Persona {

	@Id
	@Enumerated(EnumType.STRING)
	@Column(name = "code", length = 20)
	private ActorRole code;

	@Column(name = "description", nullable = false, length = 200)
	private String description;

	protected Persona() {
	}

	public ActorRole code() {
		return code;
	}

	public String description() {
		return description;
	}

}
