package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Cliente;
import com.example.BarberiaLaClasica.repository.ClienteRepository;
import com.example.BarberiaLaClasica.service.ClienteService;
import com.example.BarberiaLaClasica.service.DniService;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/clientes")
public class ClienteRestController {

    @Autowired
    private DniService dniService;

    @Autowired
    private ClienteRepository clienteRepository;

    @Autowired
    private ClienteService clienteService;

    @GetMapping("/consulta-dni/{dni}")
    public ResponseEntity<String> consultarDni(@PathVariable String dni) {
        String resultado = dniService.consultarDni(dni);
        return ResponseEntity.ok(resultado);
    }

    @PostMapping("/guardar-rapido")
    public ResponseEntity<?> guardarRapido(@RequestBody Map<String, String> datos) {
        try {
            String dni = datos.get("dni") != null ? datos.get("dni").trim() : "";
            String nombres = datos.get("nombres") != null ? datos.get("nombres").trim() : "";
            String apellidos = datos.get("apellidos") != null ? datos.get("apellidos").trim() : "";
            String telefono = datos.get("telefono") != null ? datos.get("telefono").trim() : "";
            String correo = datos.get("correo") != null ? datos.get("correo").trim() : "";

            if (!dni.matches("^\\d{8}$")) {
                return ResponseEntity.badRequest().body(Map.of("error", "El DNI debe tener 8 dígitos numéricos."));
            }
            if (nombres.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los nombres son obligatorios."));
            }
            if (apellidos.isEmpty()) {
                return ResponseEntity.badRequest().body(Map.of("error", "Los apellidos son obligatorios."));
            }

            Cliente c = new Cliente();
            c.setDni(dni);
            c.setNombres(nombres);
            c.setApellidos(apellidos);
            c.setTelefono(telefono.isEmpty() ? null : telefono);
            c.setCorreo(correo.isEmpty() ? null : correo);

            // Password predeterminada: B + dni
            String passwordPlana = "B" + dni;
            Cliente guardado = clienteService.crearDesdeAdmin(c, passwordPlana);

            return ResponseEntity.ok(Map.of(
                    "id", guardado.getId(),
                    "dni", guardado.getDni(),
                    "nombres", guardado.getNombres(),
                    "apellidos", guardado.getApellidos()));
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    /**
     * CORREGIDO: Adaptado para usar el nuevo comportamiento paginado del repositorio,
     * limitando la respuesta a los primeros 5 resultados requeridos por el componente.
     */
    @GetMapping("/buscar-dni")
    public ResponseEntity<List<Map<String, Object>>> buscarPorDni(@RequestParam String q) {
        // Creamos un Pageable de tamaño 5 para optimizar la consulta desde la base de datos
        Pageable limiteCinco = PageRequest.of(0, 5);
        
        // Llamamos al repositorio usando la estructura que acabamos de actualizar
        List<Cliente> clientes = clienteRepository
                .findByDniContainingOrNombresContainingIgnoreCaseOrApellidosContainingIgnoreCase(q, q, q, limiteCinco)
                .getContent();

        List<Map<String, Object>> resultado = clientes.stream()
                .filter(c -> c.getEstado() == 1)
                .map(c -> Map.<String, Object>of(
                        "id", c.getId(),
                        "dni", c.getDni(),
                        "nombres", c.getNombres(),
                        "apellidos", c.getApellidos()))
                .toList();

        return ResponseEntity.ok(resultado);
    }
}