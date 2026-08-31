package com.example.BarberiaLaClasica.service;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class DniService {

    private final String API_URL = "https://miapi.cloud/v1/dni/";

    // Spring Boot inyectará automáticamente el token desde tu application.properties
    @Value("${api.dni.token}")
    private String token; 

    public String consultarDni(String dni) {
        RestTemplate restTemplate = new RestTemplate();
        
        // 1. Configurar las cabeceras con el Bearer Token inyectado de forma segura
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token); // Ahora usa la variable minúscula 'token'
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<String> entity = new HttpEntity<>(headers);

        // 2. Hacer la petición GET
        try {
            ResponseEntity<String> response = restTemplate.exchange(
                API_URL + dni, 
                HttpMethod.GET, 
                entity, 
                String.class
            );
            return response.getBody(); 
        } catch (Exception e) {
            return "{\"success\": false, \"message\": \"Error al conectar con la API\"}";
        }
    }
}