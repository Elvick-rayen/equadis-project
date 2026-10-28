package com.equadis.portal.modules.persona.infrastructure;

import org.springframework.data.jpa.repository.JpaRepository;

import com.equadis.portal.modules.persona.domain.Persona;
import com.equadis.portal.shared.actor.ActorRole;

/** Spring Data adapter for the persona reference table. */
interface SpringDataPersonaRepository extends JpaRepository<Persona, ActorRole> {

}
