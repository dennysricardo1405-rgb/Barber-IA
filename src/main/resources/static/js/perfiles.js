 
function abrirModalPermisos(boton) {
    const id     = boton.getAttribute('data-id');
    const nombre = boton.getAttribute('data-nombre');
 
    // 1. Seteamos el id y nombre en el modal
    document.getElementById('perfilIdInput').value  = id;
    document.getElementById('nombrePerfilModal').innerText = nombre;
 
    // 2. Limpiamos todos los checkboxes primero
    document.querySelectorAll('.form-check-input').forEach(cb => cb.checked = false);
 
    // 3. ✅ Marcamos los permisos que YA tiene el perfil
    //    Los obtenemos del atributo data-permisos del botón
    const permisosActivos = boton.getAttribute('data-permisos');
    if (permisosActivos) {
        // Viene como "1,2,3" — lo convertimos a array
        const ids = permisosActivos.split(',').map(p => p.trim());
        ids.forEach(permisoId => {
            const checkbox = document.getElementById('perm' + permisoId);
            if (checkbox) checkbox.checked = true;
        });
    }
 
    // 4. Abrimos el modal
    const modalElement = document.getElementById('modalPermisos');
    const myModal = bootstrap.Modal.getOrCreateInstance(modalElement);
    myModal.show();
}