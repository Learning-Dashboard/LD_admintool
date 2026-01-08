package com.upc.ld_admintool.domain.services.validation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.*;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para GitHubValidationService
 * Valida la lógica de validación de organizaciones y usuarios en GitHub
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("GitHubValidationService - Tests Unitarios")
class GitHubValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private GitHubValidationService githubValidationService;

    private static final String GITHUB_API_URL = "https://api.github.com";
    private static final String GITHUB_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(githubValidationService, "githubApiUrl", GITHUB_API_URL);
        ReflectionTestUtils.setField(githubValidationService, "githubToken", GITHUB_TOKEN);
        ReflectionTestUtils.setField(githubValidationService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("validateOrganization debe validar organización existente correctamente")
    void testValidateOrganization_Success() {
        // Arrange
        String org = "test-org";
        Object[] members = new Object[]{new Object(), new Object()};
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(members, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("validateOrganization debe retornar error si organización no existe")
    void testValidateOrganization_NotFound() {
        // Arrange
        String org = "non-existent-org";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateOrganization debe retornar error si nombre está vacío")
    void testValidateOrganization_EmptyName() {
        // Act
        ValidationResult result = githubValidationService.validateOrganization("", null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("buit"));
    }

    @Test
    @DisplayName("validateOrganization debe retornar error si nombre es null")
    void testValidateOrganization_NullName() {
        // Act
        ValidationResult result = githubValidationService.validateOrganization(null, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateOrganization debe retornar warning si organización tiene miembros privados")
    void testValidateOrganization_PrivateMembers() {
        // Arrange
        String org = "private-org";
        Object[] emptyMembers = new Object[0];
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(emptyMembers, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertTrue(result.hasWarnings());
        assertTrue(result.getWarnings().get(0).contains("membres privats"));
    }

    @Test
    @DisplayName("validateOrganization debe usar token del proyecto si está disponible")
    void testValidateOrganization_WithProjectToken() {
        // Arrange
        String org = "test-org";
        String projectToken = "project-specific-token";
        Object[] members = new Object[]{new Object()};
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(members, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, projectToken);

        // Assert
        assertTrue(result.isValid());
        verify(restTemplate, times(1)).exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class));
    }

    @Test
    @DisplayName("validateOrganization debe manejar error de autorización")
    void testValidateOrganization_Unauthorized() {
        // Arrange
        String org = "private-org";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe validar usuarios correctamente")
    void testValidateUsersInOrganization_Success() {
        // Arrange
        String org = "test-org";
        List<String> usernames = Arrays.asList("user1", "user2");
        
        // Mock members response
        Object[] members = new Object[]{
            createMockMember("user1"),
            createMockMember("user2"),
            createMockMember("user3")
        };
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(members, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization(org, usernames, null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe retornar error si usuario no es miembro")
    void testValidateUsersInOrganization_UserNotMember() {
        // Arrange
        String org = "test-org";
        List<String> usernames = Arrays.asList("user1", "nonmember");
        
        Object[] members = new Object[]{createMockMember("user1")};
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(members, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization(org, usernames, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe retornar válido si lista de usuarios está vacía")
    void testValidateUsersInOrganization_EmptyList() {
        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization("test-org", Arrays.asList(), null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe retornar válido si lista es null")
    void testValidateUsersInOrganization_NullList() {
        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization("test-org", null, null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe retornar error si miembros son privados")
    void testValidateUsersInOrganization_PrivateMembers() {
        // Arrange
        String org = "private-org";
        List<String> usernames = Arrays.asList("user1");
        
        Object[] emptyMembers = new Object[0];
        ResponseEntity<Object[]> responseEntity = new ResponseEntity<>(emptyMembers, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization(org, usernames, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInOrganization debe manejar error de organización no encontrada")
    void testValidateUsersInOrganization_OrgNotFound() {
        // Arrange
        String org = "non-existent-org";
        List<String> usernames = Arrays.asList("user1");
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        // Act
        ValidationResult result = githubValidationService.validateUsersInOrganization(org, usernames, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }


    @Test
    @DisplayName("validateOrganization debe capturar Exception genérica")
    void testValidateOrganization_GenericException() {
        // Arrange
        String org = "test-org";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(Object[].class)))
                .thenThrow(new RuntimeException("Connection timeout"));

        // Act
        ValidationResult result = githubValidationService.validateOrganization(org, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors(), "Should have errors: " + result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Error validant") && e.contains("Connection timeout")),
                "Error message should contain 'Error validant' and 'Connection timeout': " + result.getErrors());
    }

    @Test
    @DisplayName("validateUser debe validar usuario existente correctamente")
    void testValidateUser_Success() {
        // Arrange
        String username = "testuser";
        ResponseEntity<java.util.Map> responseEntity = new ResponseEntity<>(new java.util.HashMap<>(), HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(java.util.Map.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUser(username, null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        verify(restTemplate).exchange(
                eq(GITHUB_API_URL + "/users/" + username),
                eq(HttpMethod.GET),
                any(HttpEntity.class),
                eq(java.util.Map.class)
        );
    }

    @Test
    @DisplayName("validateUser debe usar token del proyecto si se proporciona")
    void testValidateUser_WithProjectToken() {
        // Arrange
        String username = "testuser";
        String projectToken = "project-specific-token";
        ResponseEntity<java.util.Map> responseEntity = new ResponseEntity<>(new java.util.HashMap<>(), HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(java.util.Map.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUser(username, projectToken);

        // Assert
        assertTrue(result.isValid());
        verify(restTemplate).exchange(
                anyString(),
                eq(HttpMethod.GET),
                argThat(entity -> {
                    HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                    return headers.getFirst("Authorization") != null &&
                           headers.getFirst("Authorization").equals("token " + projectToken);
                }),
                eq(java.util.Map.class)
        );
    }

    @Test
    @DisplayName("validateUser debe retornar error si usuario no existe (NotFound)")
    void testValidateUser_NotFound() {
        // Arrange
        String username = "nonexistentuser";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(java.util.Map.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        ValidationResult result = githubValidationService.validateUser(username, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors(), "Should have errors: " + result.getErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains(username) && e.contains("no existeix")),
                "Error message should contain username and 'no existeix': " + result.getErrors());
    }

    @Test
    @DisplayName("validateUser debe retornar error si respuesta no es OK")
    void testValidateUser_NonOkStatus() {
        // Arrange
        String username = "testuser";
        ResponseEntity<java.util.Map> responseEntity = new ResponseEntity<>(new java.util.HashMap<>(), HttpStatus.FORBIDDEN);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(java.util.Map.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = githubValidationService.validateUser(username, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains(username) && e.contains("no existeix")));
    }

    @Test
    @DisplayName("validateUser debe capturar Exception genérica")
    void testValidateUser_GenericException() {
        // Arrange
        String username = "testuser";
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(java.util.Map.class)))
                .thenThrow(new RuntimeException("Network error"));

        // Act
        ValidationResult result = githubValidationService.validateUser(username, null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().stream()
                .anyMatch(e -> e.contains("Error validant") && e.contains(username) && e.contains("Network error")));
    }

    private Object createMockMember(String login) {
        return new java.util.HashMap<String, Object>() {{
            put("login", login);
        }};
    }
}
