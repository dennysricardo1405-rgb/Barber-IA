// ══════════════════════════════════════════════
//  barberos.js — con validaciones
// ══════════════════════════════════════════════

// ── Validaciones compartidas ──────────────────
function soloLetras(input) {
    input.value = input.value.replace(/[^a-zA-ZáéíóúÁÉÍÓÚñÑ\s]/g, '');
}

function soloNumeros(input) {
    input.value = input.value.replace(/[^0-9]/g, '');
}

function validarFormularioBarbero(formId) {
    const form = document.getElementById(formId);
    const nombre = form.querySelector('[name="nombre"]');
    const telefono = form.querySelector('[name="telefono"]');
    const especialidad = form.querySelector('[name="especialidad"]');
    let valido = true;

    // Limpiar errores previos
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());

    // Validar nombre
    const nombreVal = nombre.value.trim();
    if (!nombreVal) {
        mostrarError(nombre, 'El nombre es obligatorio.');
        valido = false;
    } else if (nombreVal.length < 3) {
        mostrarError(nombre, 'El nombre debe tener al menos 3 caracteres.');
        valido = false;
    } else if (nombreVal.length > 80) {
        mostrarError(nombre, 'El nombre no puede superar 80 caracteres.');
        valido = false;
    } else if (!/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(nombreVal)) {
        mostrarError(nombre, 'El nombre solo puede contener letras.');
        valido = false;
    }

    // Validar especialidad (opcional pero si se llena, solo letras)
    const espVal = especialidad.value.trim();
    if (espVal && !/^[a-zA-ZáéíóúÁÉÍÓÚñÑ\s]+$/.test(espVal)) {
        mostrarError(especialidad, 'La especialidad solo puede contener letras.');
        valido = false;
    } else if (espVal && espVal.length > 60) {
        mostrarError(especialidad, 'La especialidad no puede superar 60 caracteres.');
        valido = false;
    }

    // Validar teléfono (opcional pero si se llena, solo 9 dígitos)
    const telVal = telefono.value.trim();
    if (telVal && !/^\d{9}$/.test(telVal)) {
        mostrarError(telefono, 'El teléfono debe tener exactamente 9 dígitos.');
        valido = false;
    }

    return valido;
}

function mostrarError(input, mensaje) {
    input.classList.add('is-invalid');
    const div = document.createElement('div');
    div.className = 'invalid-feedback';
    div.textContent = mensaje;
    input.parentNode.appendChild(div);
}

// ── Modal Nuevo: bloquear caracteres en tiempo real ──
document.addEventListener('DOMContentLoaded', () => {
    // Nombre nuevo
    const inputNombreNuevo = document.querySelector('#modalNuevo [name="nombre"]');
    if (inputNombreNuevo) {
        inputNombreNuevo.addEventListener('input', () => soloLetras(inputNombreNuevo));
    }

    // Especialidad nuevo
    const inputEspNuevo = document.querySelector('#modalNuevo [name="especialidad"]');
    if (inputEspNuevo) {
        inputEspNuevo.addEventListener('input', () => soloLetras(inputEspNuevo));
    }

    // Teléfono nuevo — solo números y máx 9 dígitos
    const inputTelNuevo = document.querySelector('#modalNuevo [name="telefono"]');
    if (inputTelNuevo) {
        inputTelNuevo.addEventListener('input', () => {
            soloNumeros(inputTelNuevo);
            if (inputTelNuevo.value.length > 9) {
                inputTelNuevo.value = inputTelNuevo.value.slice(0, 9);
            }
        });
    }

    // Nombre editar
    const inputNombreEditar = document.getElementById('edit-nombre');
    if (inputNombreEditar) {
        inputNombreEditar.addEventListener('input', () => soloLetras(inputNombreEditar));
    }

    // Especialidad editar
    const inputEspEditar = document.getElementById('edit-especialidad');
    if (inputEspEditar) {
        inputEspEditar.addEventListener('input', () => soloLetras(inputEspEditar));
    }

    // Teléfono editar
    const inputTelEditar = document.getElementById('edit-telefono');
    if (inputTelEditar) {
        inputTelEditar.addEventListener('input', () => {
            soloNumeros(inputTelEditar);
            if (inputTelEditar.value.length > 9) {
                inputTelEditar.value = inputTelEditar.value.slice(0, 9);
            }
        });
    }

    // Submit modal Nuevo
    const formNuevo = document.querySelector('#modalNuevo form');
    if (formNuevo) {
        formNuevo.id = 'form-nuevo';
        formNuevo.addEventListener('submit', (e) => {
            if (!validarFormularioBarbero('form-nuevo')) {
                e.preventDefault();
            }
        });
    }

    // Submit modal Editar
    const formEditar = document.getElementById('form-editar');
    if (formEditar) {
        formEditar.addEventListener('submit', (e) => {
            if (!validarFormularioBarbero('form-editar')) {
                e.preventDefault();
            }
        });
    }

    // Limpiar errores al cerrar modales
    document.getElementById('modalNuevo').addEventListener('hidden.bs.modal', () => {
        const form = document.getElementById('form-nuevo');
        if (form) {
            form.reset();
            form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
            form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
            // Resetear preview foto
            document.getElementById('preview-nueva').classList.add('d-none');
            document.getElementById('placeholder-nueva').classList.remove('d-none');
        }
    });

    document.getElementById('modalEditar').addEventListener('hidden.bs.modal', () => {
        const form = document.getElementById('form-editar');
        if (form) {
            form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
            form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
        }
    });
});

// ── Rellena el modal de editar ──────────────────
function prepararEdicion(btn) {
    const id           = btn.dataset.id;
    const nombre       = btn.dataset.nombre;
    const especialidad = btn.dataset.especialidad || '';
    const telefono     = btn.dataset.telefono     || '';
    const diaLibre     = btn.dataset.dialib       || 'MARTES';
    const imagen       = btn.dataset.imagen       || '';

    document.getElementById('form-editar').action = `/admin/barberos/${id}/actualizar`;

    document.getElementById('edit-nombre').value       = nombre;
    document.getElementById('edit-especialidad').value = especialidad;
    document.getElementById('edit-telefono').value     = telefono;

    const sel = document.getElementById('edit-diaLibre');
    for (const opt of sel.options) {
        opt.selected = opt.value === diaLibre;
    }

    const preview     = document.getElementById('preview-editar');
    const placeholder = document.getElementById('placeholder-editar');
    if (imagen && imagen !== 'null') {
        preview.src = imagen;
        preview.classList.remove('d-none');
        placeholder.classList.add('d-none');
    } else {
        preview.src = '';
        preview.classList.add('d-none');
        placeholder.classList.remove('d-none');
    }

    document.getElementById('foto-editar').value = '';

    new bootstrap.Modal(document.getElementById('modalEditar')).show();
}

// ── Preview foto ────────────────────────────────
function previewFoto(input, previewId, placeholderId) {
    if (!input.files || !input.files[0]) return;

    // Validar que sea imagen
    const archivo = input.files[0];
    const tiposPermitidos = ['image/jpeg', 'image/png', 'image/webp'];
    if (!tiposPermitidos.includes(archivo.type)) {
        alert('Solo se permiten imágenes en formato JPG, PNG o WEBP.');
        input.value = '';
        return;
    }

    // Validar tamaño máximo 2MB
    if (archivo.size > 2 * 1024 * 1024) {
        alert('La imagen no puede superar los 2MB.');
        input.value = '';
        return;
    }

    const reader = new FileReader();
    reader.onload = e => {
        const img = document.getElementById(previewId);
        const ph  = document.getElementById(placeholderId);
        img.src = e.target.result;
        img.classList.remove('d-none');
        if (ph) ph.classList.add('d-none');
    };
    reader.readAsDataURL(archivo);
}