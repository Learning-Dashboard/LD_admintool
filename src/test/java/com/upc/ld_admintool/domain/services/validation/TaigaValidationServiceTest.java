package com.upc.ld_admintool.domain.services.validation;

import com.fasterxml.jackson.databind.ObjectMapper;
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
 * Tests unitarios para TaigaValidationService
 * Valida la lógica de validación de proyectos y usuarios en Taiga
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("TaigaValidationService - Tests Unitarios")
class TaigaValidationServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private TaigaValidationService taigaValidationService;

    private static final String TAIGA_API_URL = "https://api.taiga.io/api/v1";
    private static final String TAIGA_TOKEN = "test-token";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(taigaValidationService, "taigaApiUrl", TAIGA_API_URL);
        ReflectionTestUtils.setField(taigaValidationService, "taigaToken", TAIGA_TOKEN);
        ReflectionTestUtils.setField(taigaValidationService, "restTemplate", restTemplate);
        ReflectionTestUtils.setField(taigaValidationService, "objectMapper", new ObjectMapper());
    }

    @Test
    @DisplayName("validateProjectBySlug debe validar proyecto existente correctamente")
    void testValidateProjectBySlug_Success() {
        // Arrange
        String projectSlug = "test-project";
        ResponseEntity<String> responseEntity = new ResponseEntity<>("{\"id\":1,\"name\":\"Test\"}", HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
        assertFalse(result.hasWarnings());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si proyecto no existe")
    void testValidateProjectBySlug_NotFound() {
        // Arrange
        String projectSlug = "non-existent-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.NotFound.create(HttpStatus.NOT_FOUND, "Not Found", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si slug está vacío")
    void testValidateProjectBySlug_EmptySlug() {
        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug("");

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
        assertTrue(result.getErrors().get(0).contains("buit"));
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error si slug es null")
    void testValidateProjectBySlug_NullSlug() {
        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(null);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar error de autorización")
    void testValidateProjectBySlug_Unauthorized() {
        // Arrange
        String projectSlug = "private-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.Unauthorized.create(HttpStatus.UNAUTHORIZED, "Unauthorized", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateProjectBySlug debe retornar warning si proyecto es privado")
    void testValidateProjectBySlug_Forbidden() {
        // Arrange
        String projectSlug = "private-project";
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(HttpClientErrorException.Forbidden.create(HttpStatus.FORBIDDEN, "Forbidden", null, null, null));

        // Act
        ValidationResult result = taigaValidationService.validateProjectBySlug(projectSlug);

        // Assert
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    @DisplayName("validateUsersInProject debe validar usuarios correctamente")
    void testValidateUsersInProject_Success() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1", "user2");
        
        String jsonResponse = "{\"members\":[" +
                "{\"username\":\"user1\"}," +
                "{\"username\":\"user2\"}," +
                "{\"username\":\"user3\"}" +
                "]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar error si usuario no es miembro")
    void testValidateUsersInProject_UserNotMember() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1", "nonmember");
        
        String jsonResponse = "{\"members\":[{\"username\":\"user1\"}]}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar válido si lista de usuarios está vacía")
    void testValidateUsersInProject_EmptyList() {
        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject("test-project", Arrays.asList());

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar válido si lista es null")
    void testValidateUsersInProject_NullList() {
        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject("test-project", null);

        // Assert
        assertTrue(result.isValid());
        assertFalse(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe retornar warning si no hay members disponibles")
    void testValidateUsersInProject_NoMembers() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        String jsonResponse = "{\"id\":1,\"name\":\"Test\"}";
        ResponseEntity<String> responseEntity = new ResponseEntity<>(jsonResponse, HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames);

        // Assert - El código añade warning, no error
        assertTrue(result.isValid());
        assertTrue(result.hasWarnings());
        assertEquals(1, result.getWarnings().size());
    }

    @Test
    @DisplayName("validateUsersInProject debe manejar error HTTP")
    void testValidateUsersInProject_HttpError() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }

    @Test
    @DisplayName("validateUsersInProject debe manejar JSON inválido")
    void testValidateUsersInProject_InvalidJson() {
        // Arrange
        String projectSlug = "test-project";
        List<String> usernames = Arrays.asList("user1");
        
        ResponseEntity<String> responseEntity = new ResponseEntity<>("invalid json", HttpStatus.OK);
        
        when(restTemplate.exchange(anyString(), eq(HttpMethod.GET), any(HttpEntity.class), eq(String.class)))
                .thenReturn(responseEntity);

        // Act
        ValidationResult result = taigaValidationService.validateUsersInProject(projectSlug, usernames);

        // Assert
        assertFalse(result.isValid());
        assertTrue(result.hasErrors());
    }
}
