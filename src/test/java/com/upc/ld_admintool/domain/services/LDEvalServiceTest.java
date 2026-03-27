package com.upc.ld_admintool.domain.services;

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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * Tests unitarios para LDEvalService
 * Valida la comunicación con el servicio de evaluación del Learning Dashboard
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("LDEvalService - Tests Unitarios")
class LDEvalServiceTest {

    @Mock
    private RestTemplate restTemplate;

    @InjectMocks
    private LDEvalService ldEvalService;

    private static final String LD_EVAL_URL = "http://learning-dashboard:5000/api";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(ldEvalService, "ldEvalUrl", LD_EVAL_URL);
        ReflectionTestUtils.setField(ldEvalService, "restTemplate", restTemplate);
    }

    @Test
    @DisplayName("triggerRefresh debe retornar true cuando la llamada es exitosa")
    void testTriggerRefresh_Success() {
        // Arrange
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
        verify(restTemplate, times(1)).postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    @DisplayName("triggerRefresh debe retornar false cuando hay HttpClientErrorException")
    void testTriggerRefresh_HttpClientErrorException() {
        // Arrange
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "Bad Request"));

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertFalse(result);
        verify(restTemplate, times(1)).postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    @DisplayName("triggerRefresh debe enviar headers correctos")
    void testTriggerRefresh_CorrectHeaders() {
        // Arrange
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        
        when(restTemplate.postForEntity(
            anyString(),
            argThat(entity -> {
                HttpHeaders headers = ((HttpEntity<?>) entity).getHeaders();
                return headers.getContentType() != null &&
                       headers.getContentType().equals(MediaType.APPLICATION_JSON);
            }),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("triggerRefresh debe manejar diferentes códigos de error HTTP")
    void testTriggerRefresh_DifferentErrorCodes() {
        // Test con 404 Not Found
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.NOT_FOUND));

        assertFalse(ldEvalService.triggerRefresh());

        // Test con 500 Internal Server Error
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.INTERNAL_SERVER_ERROR));

        assertFalse(ldEvalService.triggerRefresh());

        // Test con 401 Unauthorized
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED));

        assertFalse(ldEvalService.triggerRefresh());
    }

    @Test
    @DisplayName("triggerRefresh debe usar la URL configurada correctamente")
    void testTriggerRefresh_UsesConfiguredUrl() {
        // Arrange
        String customUrl = "http://custom-ld:8080/api";
        ReflectionTestUtils.setField(ldEvalService, "ldEvalUrl", customUrl);
        
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
        verify(restTemplate).postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        );
    }

    @Test
    @DisplayName("triggerRefresh debe crear HttpEntity con cuerpo vacío")
    void testTriggerRefresh_EmptyBody() {
        // Arrange
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
        
        when(restTemplate.postForEntity(
            anyString(),
            argThat(entity -> ((HttpEntity<?>) entity).getBody() == null),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("triggerRefresh debe retornar true con status 201 CREATED")
    void testTriggerRefresh_Created() {
        // Arrange
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.CREATED);
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
    }

    @Test
    @DisplayName("triggerRefresh debe retornar true con status 202 ACCEPTED")
    void testTriggerRefresh_Accepted() {
        // Arrange
        ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.ACCEPTED);
        when(restTemplate.postForEntity(
            anyString(),
            any(HttpEntity.class),
            eq(Void.class)
        )).thenReturn(responseEntity);

        // Act
        boolean result = ldEvalService.triggerRefresh();

        // Assert
        assertTrue(result);
    }
}
