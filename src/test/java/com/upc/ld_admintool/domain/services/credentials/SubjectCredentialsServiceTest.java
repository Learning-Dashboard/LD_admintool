package com.upc.ld_admintool.domain.services.credentials;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The resolution rule must match LD Connect (config/credentials_loader.py and
 * tests/test_credentials_loader.py).
 */
@DisplayName("SubjectCredentialsService")
class SubjectCredentialsServiceTest {

    private static final String OWN_API_URL = "https://taiga.course-a.example/api/v1";
    private static final String OWN_AUTH_URL = "https://taiga.course-a.example/api/v1/auth";
    private static final String FALLBACK_API_URL = "https://api.taiga.io/api/v1";
    private static final String FALLBACK_AUTH_URL = "https://api.taiga.io/api/v1/auth";

    @TempDir
    Path tempDir;

    private Path credentialsFile;
    private SubjectCredentialsService service;

    @BeforeEach
    void setUp() {
        credentialsFile = tempDir.resolve("credentials_config.json");
        service = new SubjectCredentialsService();
        ReflectionTestUtils.setField(service, "credentialsFile", credentialsFile.toString());
        ReflectionTestUtils.setField(service, "fallbackApiUrl", FALLBACK_API_URL);
        ReflectionTestUtils.setField(service, "fallbackAuthUrl", FALLBACK_AUTH_URL);
        ReflectionTestUtils.setField(service, "fallbackUsername", "fallback_user");
        ReflectionTestUtils.setField(service, "fallbackPassword", "fallback_pass");
    }

    private void write(String json) throws IOException {
        Files.writeString(credentialsFile, json);
    }

    /** One subject called "AMEP" with team "Team"; extraFields are added to its entry. */
    private void writeSubject(String extraFields) throws IOException {
        String extra = extraFields.isEmpty() ? "" : ", " + extraFields;
        write("{\"AMEP\": {\"github_token\": \"ghp_x\", \"teams\": [\"Team\"]" + extra + "}}");
    }

    private String errorFor(String subject) {
        return assertThrows(CredentialsConfigException.class, () -> service.resolveBySubject(subject)).getMessage();
    }

    // ---- Subject lookup -------------------------------------------------------

    @Test
    @DisplayName("resolves the GitHub token and the teams of the subject")
    void resolvesGithubTokenAndTeams() throws IOException {
        write("""
                {
                  "AMEP": {"github_token": " ghp_amep ", "taiga_token": "tok", "teams": ["A1", "A2"]},
                  "ASW": {"github_token": "ghp_asw", "taiga_token": "tok", "teams": ["B1"]}
                }
                """);

        SubjectCredentials amep = service.resolveBySubject("AMEP");
        assertEquals("AMEP", amep.subject());
        assertEquals("ghp_amep", amep.githubToken());
        assertEquals(List.of("A1", "A2"), amep.teams());
        assertEquals("ghp_asw", service.resolveBySubject("ASW").githubToken());
    }

    @Test
    @DisplayName("the subject name ignores case and surrounding spaces")
    void subjectNameIgnoresCaseAndSpaces() throws IOException {
        writeSubject("");
        assertEquals("AMEP", service.resolveBySubject("  amep ").subject());
    }

    @Test
    @DisplayName("team membership is an exact match")
    void teamMembershipIsExact() throws IOException {
        writeSubject("");
        SubjectCredentials credentials = service.resolveBySubject("AMEP");

        assertTrue(credentials.hasTeam("Team"));
        assertFalse(credentials.hasTeam("team"));
        assertFalse(credentials.hasTeam("Other"));
        assertFalse(credentials.hasTeam(null));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "   "})
    @DisplayName("an empty subject cannot be resolved")
    void emptySubject(String subject) throws IOException {
        writeSubject("");
        assertTrue(errorFor(subject).contains("subject is empty"));
        assertTrue(errorFor(null).contains("subject is empty"));
    }

    @Test
    @DisplayName("an unknown subject cannot be resolved")
    void unknownSubject() throws IOException {
        writeSubject("");
        assertTrue(errorFor("Unknown Subject").contains("'Unknown Subject' is not defined"));
    }

    @Test
    @DisplayName("a subject needs a github_token")
    void missingGithubToken() throws IOException {
        write("{\"AMEP\": {\"github_token\": \"  \", \"teams\": [\"Team\"]}}");
        assertTrue(errorFor("AMEP").contains("has no github_token"));
    }

    @Test
    @DisplayName("a subject needs a teams list")
    void missingTeams() throws IOException {
        write("{\"AMEP\": {\"github_token\": \"ghp_x\"}}");
        assertTrue(errorFor("AMEP").contains("must have a 'teams' list"));
    }

    @Test
    @DisplayName("a subject entry must be an object")
    void subjectMustBeObject() throws IOException {
        write("{\"AMEP\": \"x\"}");
        assertTrue(errorFor("AMEP").contains("must be a JSON object"));
    }

    // ---- Taiga server and credentials -----------------------------------------

    @Test
    @DisplayName("own server with a static token")
    void ownServerWithToken() throws IOException {
        writeSubject("\"taiga_api_url\": \"" + OWN_API_URL + "/ \", \"taiga_auth_url\": \"" + OWN_AUTH_URL
                + "\", \"taiga_token\": \"app:token\"");

        TaigaContext taiga = service.resolveBySubject("AMEP").taiga();
        assertEquals(TaigaContext.withToken(OWN_API_URL, OWN_AUTH_URL, "app:token"), taiga);
        assertTrue(taiga.hasToken());
    }

    @Test
    @DisplayName("own server with user and password")
    void ownServerWithLogin() throws IOException {
        writeSubject("\"taiga_api_url\": \"" + OWN_API_URL + "\", \"taiga_auth_url\": \"" + OWN_AUTH_URL
                + "\", \"taiga_user\": \"user\", \"taiga_password\": \"pass\"");

        TaigaContext taiga = service.resolveBySubject("AMEP").taiga();
        assertEquals(TaigaContext.withLogin(OWN_API_URL, OWN_AUTH_URL, "user", "pass"), taiga);
        assertFalse(taiga.hasToken());
    }

    @Test
    @DisplayName("the token wins over user and password")
    void tokenWinsOverLogin() throws IOException {
        writeSubject("\"taiga_token\": \"tok\", \"taiga_user\": \"user\", \"taiga_password\": \"pass\"");
        assertEquals(TaigaContext.withToken(FALLBACK_API_URL, FALLBACK_AUTH_URL, "tok"),
                service.resolveBySubject("AMEP").taiga());
    }

    @Test
    @DisplayName("own server never uses the fallback user")
    void ownServerWithoutCredentials() throws IOException {
        writeSubject("\"taiga_api_url\": \"" + OWN_API_URL + "\", \"taiga_auth_url\": \"" + OWN_AUTH_URL + "\"");
        assertTrue(errorFor("AMEP").contains("neither taiga_token nor taiga_user and taiga_password"));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "\"taiga_api_url\": \"" + OWN_API_URL + "\", \"taiga_auth_url\": \"\"",
            "\"taiga_api_url\": \"\", \"taiga_auth_url\": \"" + OWN_AUTH_URL + "\"",
            "\"taiga_api_url\": \"" + OWN_API_URL + "\"" })
    @DisplayName("only one of the two Taiga URLs is a config error")
    void onlyOneUrl(String urls) throws IOException {
        writeSubject(urls + ", \"taiga_token\": \"tok\"");
        assertTrue(errorFor("AMEP").contains("must set both taiga_api_url and taiga_auth_url"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", "\"taiga_api_url\": \"\", \"taiga_auth_url\": \"\"", "\"taiga_api_url\": null"})
    @DisplayName("empty URLs use the fallback server and the fallback user")
    void fallbackServerWithFallbackUser(String urls) throws IOException {
        writeSubject(urls);
        assertEquals(TaigaContext.withLogin(FALLBACK_API_URL, FALLBACK_AUTH_URL, "fallback_user", "fallback_pass"),
                service.resolveBySubject("AMEP").taiga());
    }

    @Test
    @DisplayName("fallback server with the subject's user and password")
    void fallbackServerWithSubjectLogin() throws IOException {
        writeSubject("\"taiga_user\": \"user\", \"taiga_password\": \"pass\"");
        assertEquals(TaigaContext.withLogin(FALLBACK_API_URL, FALLBACK_AUTH_URL, "user", "pass"),
                service.resolveBySubject("AMEP").taiga());
    }

    @Test
    @DisplayName("an incomplete subject login uses the fallback user")
    void incompleteSubjectLoginUsesFallbackUser() throws IOException {
        writeSubject("\"taiga_user\": \"user\", \"taiga_password\": \"\"");
        assertEquals("fallback_user", service.resolveBySubject("AMEP").taiga().username());
    }

    @ParameterizedTest
    @ValueSource(strings = {"fallbackApiUrl", "fallbackAuthUrl"})
    @DisplayName("the fallback server must be configured when a subject needs it")
    void fallbackServerNotConfigured(String field) throws IOException {
        writeSubject("\"taiga_token\": \"tok\"");
        ReflectionTestUtils.setField(service, field, "");
        assertTrue(errorFor("AMEP").contains("TAIGA_FALLBACK_API_URL and TAIGA_FALLBACK_AUTH_URL are not both set"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"fallbackUsername", "fallbackPassword"})
    @DisplayName("the fallback user must be configured when a subject needs it")
    void fallbackUserNotConfigured(String field) throws IOException {
        writeSubject("");
        ReflectionTestUtils.setField(service, field, "");
        assertTrue(errorFor("AMEP")
                .contains("TAIGA_FALLBACK_USERNAME and TAIGA_FALLBACK_PASSWORD are not both set"));
    }

    // ---- File handling --------------------------------------------------------

    @Test
    @DisplayName("a missing file is reported with its path")
    void missingFile() {
        String message = errorFor("AMEP");
        assertTrue(message.contains("not found"));
        assertTrue(message.contains(credentialsFile.toString()));
    }

    @Test
    @DisplayName("invalid JSON is reported")
    void invalidJson() throws IOException {
        write("{invalid json");
        assertTrue(errorFor("AMEP").contains("invalid JSON"));
    }

    @Test
    @DisplayName("the top level must be an object")
    void topLevelMustBeObject() throws IOException {
        write("[\"not\", \"an\", \"object\"]");
        assertTrue(errorFor("AMEP").contains("must contain a JSON object"));
    }

    @Test
    @DisplayName("the file is read again when it changes")
    void rereadsFileWhenItChanges() throws IOException {
        write("{\"AMEP\": {\"github_token\": \"ghp_old\", \"teams\": [\"Team\"]}}");
        assertEquals("ghp_old", service.resolveBySubject("AMEP").githubToken());

        FileTime before = Files.getLastModifiedTime(credentialsFile);
        write("{\"AMEP\": {\"github_token\": \"ghp_new\", \"teams\": [\"Team\"]}}");
        // Same-size rewrites within the timestamp resolution must be detected too
        Files.setLastModifiedTime(credentialsFile, FileTime.fromMillis(before.toMillis() + 2_000));

        assertEquals("ghp_new", service.resolveBySubject("AMEP").githubToken());
    }

    @Test
    @DisplayName("an unchanged file is not read again")
    void doesNotRereadUnchangedFile() throws IOException {
        writeSubject("");
        service.resolveBySubject("AMEP");

        // Same size and same timestamp: the cached content must still be served
        FileTime before = Files.getLastModifiedTime(credentialsFile);
        write("{\"AMEP\": {\"github_token\": \"ghp_y\", \"teams\": [\"Team\"]}}");
        Files.setLastModifiedTime(credentialsFile, before);

        assertEquals("ghp_x", service.resolveBySubject("AMEP").githubToken());
    }

    // ---- Secrets --------------------------------------------------------------

    @Test
    @DisplayName("toString never shows secrets")
    void toStringHidesSecrets() {
        TaigaContext taiga = new TaigaContext("api", "auth", "s3cret-token", "user", "s3cret-pass");
        SubjectCredentials credentials = new SubjectCredentials("AMEP", "s3cret-github", taiga, List.of("Team"));

        assertFalse(taiga.toString().contains("s3cret"));
        assertFalse(credentials.toString().contains("s3cret"));
        assertTrue(credentials.toString().contains("AMEP"));
    }
}
