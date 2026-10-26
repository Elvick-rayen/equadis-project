package com.equadis.portal.persona;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.shared.actor.ActorRole;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/personas")
@Tag(name = "Personas")
class PersonaController {

	record PersonaResponse(ActorRole code, String description) {
	}

	private final PersonaRepository personas;

	PersonaController(PersonaRepository personas) {
		this.personas = personas;
	}

	@GetMapping
	@Operation(summary = "List the persona types a caller can act as")
	List<PersonaResponse> list() {
		return personas.findAll(Sort.by("code"))
			.stream()
			.map(persona -> new PersonaResponse(persona.code(), persona.description()))
			.toList();
	}

}
