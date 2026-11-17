package com.upc.ld_admintool.domain.services.credentials;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Reads the per-subject credentials config that LD Connect also uses
 * (credentials_config.json) and resolves the GitHub token and the Taiga server
 * of a subject.
 *
 * The resolution rule is the one of LD Connect (config/credentials_loader.py);
 * both must stay in step:
 * <ol>
 * <li>Taiga server: the subject's taiga_api_url and taiga_auth_url, or the
 * fallback Taiga when both are empty.</li>
 * <li>Taiga credentials: taiga_token, else taiga_user and taiga_password, else
 * the fallback user (only on the fallback Taiga).</li>
 * <li>GitHub token: the subject's github_token, without fallback.</li>
 * </ol>
 */
@Service
public class SubjectCredentialsService {

    @Value("${credentials.file:config_files/credentials_config.json}")
    private String credentialsFile;

    @Value("${taiga.fallback.api-url:}")
    private String fallbackApiUrl;

    @Value("${taiga.fallback.auth-url:}")
    private String fallbackAuthUrl;

    @Value("${taiga.fallback.username:}")
    private String fallbackUsername;

    @Value("${taiga.fallback.password:}")
    private String fallbackPassword;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** Last file read, kept until the file changes on disk. */
    private Snapshot snapshot;

    private record Snapshot(Path path, FileTime lastModified, long size, JsonNode config) {
    }

    /**
     * Returns the credentials of a subject. The subject name is matched ignoring
     * case and surrounding spaces.
     *
     * @throws CredentialsConfigException if the subject is empty or unknown, if
     *                                    its entry is incomplete, or if the file
     *                                    cannot be read
     */
    public SubjectCredentials resolveBySubject(String subject) {
        String wanted = text(subject);
        if (wanted.isEmpty()) {
            throw new CredentialsConfigException("The subject is empty, so its credentials cannot be resolved.");
        }

        for (Map.Entry<String, JsonNode> entry : load().properties()) {
            if (entry.getKey().trim().equalsIgnoreCase(wanted)) {
                return resolve(entry.getKey(), entry.getValue());
            }
        }
        throw new CredentialsConfigException(
                "Subject '" + wanted + "' is not defined in the credentials config (" + credentialsFile + ").");
    }

    private SubjectCredentials resolve(String subject, JsonNode props) {
        if (!props.isObject()) {
            throw new CredentialsConfigException("Subject '" + subject + "' must be a JSON object.");
        }
        return new SubjectCredentials(subject, githubToken(subject, props), taigaContext(subject, props),
                teams(subject, props));
    }

    private String githubToken(String subject, JsonNode props) {
        String token = text(props.get("github_token"));
        if (token.isEmpty()) {
            throw new CredentialsConfigException("Subject '" + subject + "' has no github_token.");
        }
        return token;
    }

    private List<String> teams(String subject, JsonNode props) {
        JsonNode teamsNode = props.get("teams");
        if (teamsNode == null || !teamsNode.isArray()) {
            throw new CredentialsConfigException("Subject '" + subject + "' must have a 'teams' list.");
        }
        List<String> teams = new ArrayList<>();
        for (JsonNode team : teamsNode) {
            if (team.isTextual()) {
                teams.add(team.asText());
            }
        }
        return teams;
    }

    private TaigaContext taigaContext(String subject, JsonNode props) {
        String apiUrl = url(props.get("taiga_api_url"));
        String authUrl = url(props.get("taiga_auth_url"));
        if (apiUrl.isEmpty() != authUrl.isEmpty()) {
            throw new CredentialsConfigException("Subject '" + subject
                    + "' must set both taiga_api_url and taiga_auth_url, or leave both empty to use the fallback Taiga.");
        }

        boolean usesFallback = apiUrl.isEmpty();
        if (usesFallback) {
            apiUrl = url(fallbackApiUrl);
            authUrl = url(fallbackAuthUrl);
            if (apiUrl.isEmpty() || authUrl.isEmpty()) {
                throw new CredentialsConfigException("Subject '" + subject
                        + "' uses the fallback Taiga, but TAIGA_FALLBACK_API_URL and TAIGA_FALLBACK_AUTH_URL are not both set.");
            }
        }

        String token = text(props.get("taiga_token"));
        if (!token.isEmpty()) {
            return TaigaContext.withToken(apiUrl, authUrl, token);
        }

        String username = text(props.get("taiga_user"));
        String password = raw(props.get("taiga_password"));
        if (!username.isEmpty() && !password.isEmpty()) {
            return TaigaContext.withLogin(apiUrl, authUrl, username, password);
        }

        if (!usesFallback) {
            throw new CredentialsConfigException("Subject '" + subject
                    + "' has its own Taiga server but neither taiga_token nor taiga_user and taiga_password.");
        }

        username = text(fallbackUsername);
        password = fallbackPassword == null ? "" : fallbackPassword;
        if (username.isEmpty() || password.isEmpty()) {
            throw new CredentialsConfigException("Subject '" + subject
                    + "' uses the fallback Taiga without credentials of its own, but TAIGA_FALLBACK_USERNAME and TAIGA_FALLBACK_PASSWORD are not both set.");
        }
        return TaigaContext.withLogin(apiUrl, authUrl, username, password);
    }

    /** Returns the parsed config, reading the file again only when it has changed. */
    private synchronized JsonNode load() {
        Path path = Path.of(credentialsFile).toAbsolutePath();
        try {
            FileTime lastModified = Files.getLastModifiedTime(path);
            long size = Files.size(path);
            if (snapshot != null && snapshot.path().equals(path) && snapshot.lastModified().equals(lastModified)
                    && snapshot.size() == size) {
                return snapshot.config();
            }

            JsonNode config = objectMapper.readTree(Files.readAllBytes(path));
            if (config == null || !config.isObject()) {
                throw new CredentialsConfigException("Credentials config file at " + path
                        + " must contain a JSON object with one entry per subject.");
            }
            snapshot = new Snapshot(path, lastModified, size, config);
            return config;
        } catch (NoSuchFileException e) {
            throw new CredentialsConfigException("Credentials config file not found at " + path
                    + ". Set CREDENTIALS_FILE to the credentials_config.json that LD Connect uses.", e);
        } catch (com.fasterxml.jackson.core.JacksonException e) {
            throw new CredentialsConfigException(
                    "Credentials config file at " + path + " contains invalid JSON: " + e.getOriginalMessage(), e);
        } catch (IOException e) {
            throw new CredentialsConfigException(
                    "Credentials config file at " + path + " cannot be read: " + e.getMessage(), e);
        }
    }

    private static String text(JsonNode node) {
        return node != null && node.isTextual() ? node.asText().trim() : "";
    }

    private static String raw(JsonNode node) {
        return node != null && node.isTextual() ? node.asText() : "";
    }

    private static String text(String value) {
        return value == null ? "" : value.trim();
    }

    private static String url(JsonNode node) {
        return url(text(node));
    }

    private static String url(String value) {
        return text(value).replaceAll("/+$", "");
    }
}
