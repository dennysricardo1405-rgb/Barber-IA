package com.example.BarberiaLaClasica.controller;

import com.example.BarberiaLaClasica.model.Proveedor;
import com.example.BarberiaLaClasica.service.ProveedorService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequestMapping("/admin/proveedores")
public class ProveedorController {

    @Autowired
    private ProveedorService proveedorService;

    // Listar todo en la pantalla principal de proveedores
    @GetMapping
    public String listarProveedores(Model model) {
        model.addAttribute("proveedores", proveedorService.listarTodos());
        model.addAttribute("nuevoProveedor", new Proveedor()); // Objeto vacío para el modal de añadir
        return "proveedores/lista"; // Apunta a templates/proveedores/lista.html
    }

    // Guardar o Editar Proveedor
    @PostMapping("/guardar")
    public String guardarProveedor(@ModelAttribute("nuevoProveedor") Proveedor proveedor) {
        proveedorService.guardar(proveedor);
        return "redirect:/admin/proveedores?exito";
    }

    // Cambiar estado con tu slider Switch
    @GetMapping("/estado/{id}")
    public String cambiarEstado(@PathVariable("id") Long id) {
        proveedorService.cambiarEstado(id);
        return "redirect:/admin/proveedores?estadoCambiado";
    }
}