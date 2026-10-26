package com.equadis.portal.persona;

import org.springframework.data.jpa.repository.JpaRepository;

import com.equadis.portal.shared.actor.ActorRole;

interface PersonaRepository extends JpaRepository<Persona, ActorRole> {

}
