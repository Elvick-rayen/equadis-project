package com.equadis.portal.modules.persona.infrastructure;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.equadis.portal.modules.persona.domain.Persona;

/** Read side of the persona module: the persona types, in code order. */
@Component
@Transactional(readOnly = true)
public class PersonaReadModel {

	private final SpringDataPersonaRepository repository;

	PersonaReadModel(SpringDataPersonaRepository repository) {
		this.repository = repository;
	}

	public List<Persona> findAll() {
		return repository.findAll(Sort.by("code"));
	}

}
