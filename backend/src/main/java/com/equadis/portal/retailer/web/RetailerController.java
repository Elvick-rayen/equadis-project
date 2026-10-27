package com.equadis.portal.retailer.web;

import java.util.List;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.equadis.portal.retailer.RetailerQueries;
import com.equadis.portal.retailer.RetailerSummary;
import com.equadis.portal.shared.actor.ActorAccess;
import com.equadis.portal.shared.actor.CurrentActor;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("/api/retailers")
@Tag(name = "Retailers")
class RetailerController {

	private final RetailerQueries queries;

	private final ActorAccess access;

	RetailerController(RetailerQueries queries, ActorAccess access) {
		this.queries = queries;
		this.access = access;
	}

	@GetMapping("/summaries")
	@Operation(summary = "All retailers as id + name, for the administrator's \"Acting as\" selector")
	List<RetailerSummary> summaries(CurrentActor actor) {
		access.requireAdmin(actor);
		return queries.findAllSummaries();
	}

}
