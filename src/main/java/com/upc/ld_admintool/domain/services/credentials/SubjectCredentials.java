package com.upc.ld_admintool.domain.services.credentials;

import java.util.List;

/**
 * Credentials of one subject of the credentials config: its GitHub token, its
 * Taiga server and the teams that belong to it.
 */
public record SubjectCredentials(String subject, String githubToken, TaigaContext taiga, List<String> teams) {

    public SubjectCredentials {
        teams = List.copyOf(teams);
    }

    /**
     * LD Connect looks a team up by its exact identifier, so the comparison is
     * exact here too.
     */
    public boolean hasTeam(String teamExternalId) {
        return teamExternalId != null && teams.contains(teamExternalId);
    }

    /** Secrets are left out so that the credentials can be logged. */
    @Override
    public String toString() {
        return "SubjectCredentials[subject=" + subject + ", taiga=" + taiga + ", teams=" + teams + "]";
    }
}
