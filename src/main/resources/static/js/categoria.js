function prepararEdicionCategoria(boton) {
            // Obtenemos los atributos th:data personalizados del botón presionado
            const id = boton.getAttribute('data-id');
            const nombre = boton.getAttribute('data-nombre');
            const descripcion = boton.getAttribute('data-descripcion');
            const padreId = boton.getAttribute('data-padre');

            // Inyectamos los valores directos en los inputs del modal de edición
            document.getElementById('editId').value = id;
            document.getElementById('editNombre').value = nombre;
            document.getElementById('editDescripcion').value = descripcion;
            
            // Asignamos la categoría padre seleccionada (si es vacía, selecciona la por defecto)
            document.getElementById('editPadre').value = padreId ? padreId : "";

            // Levantamos el modal de Bootstrap programáticamente
            const modal = new bootstrap.Modal(document.getElementById('modalEditarCategoria'));
            modal.show();
}