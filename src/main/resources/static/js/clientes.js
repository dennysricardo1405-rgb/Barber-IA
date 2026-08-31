// ============================================================
// clientes.js — con validaciones y cambio de contraseña
// ============================================================

document.addEventListener('DOMContentLoaded', function () {

    // ── Solo números en DNI y teléfonos ──────────────────────
    const dniInput = document.getElementById('dniInput');
    if (dniInput) {
        dniInput.addEventListener('input', function () {
            this.value = this.value.replace(/[^0-9]/g, '');
        });
    }

    const tlfInput = document.getElementById('telefonoInput');
    if (tlfInput) {
        tlfInput.addEventListener('input', function () {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 9) this.value = this.value.slice(0, 9);
        });
    }

    const editTlf = document.getElementById('edit-telefono');
    if (editTlf) {
        editTlf.addEventListener('input', function () {
            this.value = this.value.replace(/[^0-9]/g, '');
            if (this.value.length > 9) this.value = this.value.slice(0, 9);
        });
    }

    // ── Limpiar entrada de usuario en el correo (@gmail.com predeterminado) ──
    const correoInput = document.getElementById('correoInput');
    if (correoInput) {
        correoInput.addEventListener('input', function () {
            this.value = this.value.replace(/@.*$/, '').replace(/\s+/g, '');
        });
    }

    const editCorreoPrefix = document.getElementById('edit-correo-prefix');
    if (editCorreoPrefix) {
        editCorreoPrefix.addEventListener('input', function () {
            this.value = this.value.replace(/@.*$/, '').replace(/\s+/g, '');
        });
    }

    // ── Buscar DNI ───────────────────────────────────────────
    const btnBuscarDni = document.getElementById('btnBuscarDni');
    if (btnBuscarDni) {
        btnBuscarDni.addEventListener('click', buscarDni);
    }

    // ── Guardar nuevo cliente ────────────────────────────────
    const btnGuardarCliente = document.getElementById('btnGuardarCliente');
    if (btnGuardarCliente) {
        btnGuardarCliente.addEventListener('click', guardarCliente);
    }

    const formNuevoCliente = document.getElementById('form-nuevo-cliente');
    if (formNuevoCliente) {
        formNuevoCliente.addEventListener('submit', function (e) {
            e.preventDefault();
            guardarCliente();
        });
    }

    // ── Paginación ───────────────────────────────────────────
    const selectSize = document.getElementById('select-size-pages');
    if (selectSize) {
        selectSize.addEventListener('change', function () {
            const currentSearch = document.getElementById('current-search').value || '';
            const baseUrl = window.location.pathname;
            window.location.href = `${baseUrl}?page=0&size=${this.value}&search=${encodeURIComponent(currentSearch)}`;
        });
    }

    // ── Botones editar ───────────────────────────────────────
    document.querySelectorAll('.edit-client-btn').forEach(button => {
        button.addEventListener('click', function () {
            prepararEdicionCliente(this);
        });
    });

    // ── Switches de estado ───────────────────────────────────
    document.querySelectorAll('.status-switch').forEach(checkbox => {
        checkbox.addEventListener('change', function () {
            const targetUrl = this.getAttribute('data-url');
            if (targetUrl) window.location.href = targetUrl;
        });
    });

    // ── Validar modal editar al submit ───────────────────────
    const formEditar = document.getElementById('form-editar');
    if (formEditar) {
        formEditar.addEventListener('submit', function (e) {
            if (!validarFormularioEditar()) {
                e.preventDefault();
            }
        });
    }

    // ── Limpiar al cerrar modales ────────────────────────────
    document.getElementById('modalNuevo')?.addEventListener('hidden.bs.modal', () => {
        limpiarErrores('form-nuevo-cliente');
        ['dniInput', 'nombresInput', 'apellidosInput', 'telefonoInput', 'correoInput'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.value = '';
        });
        dniInput?.classList.remove('is-valid', 'is-invalid');
    });

    document.getElementById('modalEditar')?.addEventListener('hidden.bs.modal', () => {
        limpiarErrores('form-editar');
        const editPrefix = document.getElementById('edit-correo-prefix');
        if (editPrefix) editPrefix.value = '';
        document.getElementById('edit-correo').value = '';
        document.getElementById('edit-nueva-password').value    = '';
        document.getElementById('edit-confirmar-password').value = '';
    });
});

// ── Helpers ───────────────────────────────────────────────────
function mostrarError(input, mensaje) {
    input.classList.add('is-invalid');
    if (!input.parentNode.querySelector('.invalid-feedback')) {
        const div = document.createElement('div');
        div.className = 'invalid-feedback';
        div.textContent = mensaje;
        input.parentNode.appendChild(div);
    }
}

function limpiarErrores(formId) {
    const form = document.getElementById(formId);
    if (!form) return;
    form.querySelectorAll('.is-invalid').forEach(el => el.classList.remove('is-invalid'));
    form.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
}

function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon  = btn.querySelector('i');
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
    }
}

// ── Validar formulario nuevo ──────────────────────────────────
function validarFormularioNuevo() {
    limpiarErrores('form-nuevo-cliente');
    let valido = true;

    const dni      = document.getElementById('dniInput');
    const nombres  = document.getElementById('nombresInput');
    const apellidos= document.getElementById('apellidosInput');
    const telefono = document.getElementById('telefonoInput');
    const correo   = document.getElementById('correoInput');

    if (!dni.value.trim() || !/^\d{8}$/.test(dni.value.trim())) {
        mostrarError(dni, 'Debes buscar un DNI válido de 8 dígitos.');
        valido = false;
    }
    if (!nombres.value.trim()) {
        mostrarError(nombres, 'Debes buscar el DNI primero para obtener los nombres.');
        valido = false;
    }
    if (!apellidos.value.trim()) {
        mostrarError(apellidos, 'Debes buscar el DNI primero para obtener los apellidos.');
        valido = false;
    }
    if (telefono.value.trim() && !/^\d{9}$/.test(telefono.value.trim())) {
        mostrarError(telefono, 'El teléfono debe tener exactamente 9 dígitos.');
        valido = false;
    }
    const correoPrefix = correo.value.trim().replace(/@.*$/, '');
    if (correoPrefix && !/^[a-zA-Z0-9._%+-]+$/.test(correoPrefix)) {
        mostrarError(correo, 'Ingresa un nombre de usuario de correo válido.');
        valido = false;
    }

    return valido;
}

// ── Validar formulario editar ─────────────────────────────────
function validarFormularioEditar() {
    limpiarErrores('form-editar');
    let valido = true;

    const telefono         = document.getElementById('edit-telefono');
    const correoPrefixEl   = document.getElementById('edit-correo-prefix');
    const correoHidden     = document.getElementById('edit-correo');
    const nuevaPassword    = document.getElementById('edit-nueva-password');
    const confirmarPassword= document.getElementById('edit-confirmar-password');

    // Teléfono: opcional, si se llena 9 dígitos
    if (telefono.value.trim() && !/^\d{9}$/.test(telefono.value.trim())) {
        mostrarError(telefono, 'El teléfono debe tener exactamente 9 dígitos.');
        valido = false;
    }

    // Correo: opcional, si se llena formato válido
    const prefixVal = correoPrefixEl ? correoPrefixEl.value.trim().replace(/@.*$/, '') : '';
    if (prefixVal && !/^[a-zA-Z0-9._%+-]+$/.test(prefixVal)) {
        mostrarError(correoPrefixEl, 'Ingresa un nombre de usuario de correo válido.');
        valido = false;
    }

    if (valido && correoHidden) {
        correoHidden.value = prefixVal ? (prefixVal + '@gmail.com') : '';
    }

    // Contraseña: opcional, si se llena validar longitud y coincidencia
    const pwdVal     = nuevaPassword.value.trim();
    const confirmVal = confirmarPassword.value.trim();

    if (pwdVal) {
        if (pwdVal.length < 6) {
            mostrarError(nuevaPassword, 'La contraseña debe tener al menos 6 caracteres.');
            valido = false;
        } else if (pwdVal.length > 30) {
            mostrarError(nuevaPassword, 'La contraseña no puede superar 30 caracteres.');
            valido = false;
        } else if (pwdVal !== confirmVal) {
            mostrarError(confirmarPassword, 'Las contraseñas no coinciden.');
            valido = false;
        }
    } else if (confirmVal) {
        // Si llenó confirmar pero no nueva
        mostrarError(nuevaPassword, 'Escribe primero la nueva contraseña.');
        valido = false;
    }

    return valido;
}

// ── Buscar DNI ────────────────────────────────────────────────
async function buscarDni() {
    const dniInput = document.getElementById('dniInput');
    const btn      = document.getElementById('btnBuscarDni');
    const dni      = dniInput.value.trim();

    if (!/^\d{8}$/.test(dni)) {
        mostrarError(dniInput, 'El DNI debe tener exactamente 8 dígitos numéricos.');
        return;
    }

    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin"></i>';
    btn.disabled  = true;

    try {
        const response = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data     = await response.json();

        if (data.success && data.datos) {
            document.getElementById('nombresInput').value   = data.datos.nombres || '';
            document.getElementById('apellidosInput').value = ((data.datos.ape_paterno || '') + ' ' + (data.datos.ape_materno || '')).trim();
            dniInput.classList.add('is-valid');
            dniInput.classList.remove('is-invalid');
            dniInput.parentNode.querySelectorAll('.invalid-feedback').forEach(el => el.remove());
        } else {
            mostrarError(dniInput, 'No se encontró información en RENIEC. Puedes ingresar los nombres manualmente.');
        }
    } catch (error) {
        mostrarError(dniInput, 'Problema al consultar RENIEC. Puedes ingresar los nombres manualmente.');
    } finally {
        btn.innerHTML = '<i class="fa-solid fa-magnifying-glass"></i>';
        btn.disabled  = false;
    }
}

// ── Guardar nuevo cliente ─────────────────────────────────────
async function guardarCliente() {
    if (!validarFormularioNuevo()) return;

    const btnGuardar = document.getElementById('btnGuardarCliente');
    btnGuardar.disabled = true;
    btnGuardar.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin me-2"></i>Guardando...';

    const correoRaw = document.getElementById('correoInput').value.trim().replace(/@.*$/, '');
    const correoFinal = correoRaw ? (correoRaw + '@gmail.com') : '';

    const datos = {
        dni:       document.getElementById('dniInput').value.trim(),
        nombres:   document.getElementById('nombresInput').value.trim(),
        apellidos: document.getElementById('apellidosInput').value.trim(),
        telefono:  document.getElementById('telefonoInput').value.trim(),
        correo:    correoFinal
    };

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const res  = await fetch('/api/clientes/guardar-rapido', {
            method:  'POST',
            headers: headers,
            body:    JSON.stringify(datos)
        });
        const data = await res.json();

        if (res.ok) {
            const modalEl       = document.getElementById('modalNuevo');
            const modalInstance = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
            modalInstance.hide();
            location.reload();
        } else {
            alert('Error: ' + (data.error || 'No se pudo guardar el cliente.'));
        }
    } catch (e) {
        alert('Error de conexión.');
    } finally {
        btnGuardar.disabled = false;
        btnGuardar.innerHTML = '<i class="fa-solid fa-user-plus me-1"></i> Registrar Cliente';
    }
}

// ── Preparar modal editar ─────────────────────────────────────
function prepararEdicionCliente(btn) {
    const id        = btn.getAttribute('data-id');
    const dni       = btn.getAttribute('data-dni');
    const nombres   = btn.getAttribute('data-nombres');
    const apellidos = btn.getAttribute('data-apellidos');
    const telefono  = btn.getAttribute('data-telefono');
    const correo    = btn.getAttribute('data-correo');

    document.getElementById('edit-dni').value       = dni || '';
    document.getElementById('edit-nombres').value   = nombres || '';
    document.getElementById('edit-apellidos').value = apellidos || '';
    document.getElementById('edit-telefono').value  = (telefono === '—' || !telefono) ? '' : telefono;
    
    let correoPrefix = '';
    if (correo && correo !== '—') {
        correoPrefix = correo.replace(/@.*$/, '');
    }
    const editCorreoPrefixEl = document.getElementById('edit-correo-prefix');
    if (editCorreoPrefixEl) editCorreoPrefixEl.value = correoPrefix;
    document.getElementById('edit-correo').value = (correo === '—' || !correo) ? '' : correo;

    // Limpiar campos de contraseña al abrir
    document.getElementById('edit-nueva-password').value     = '';
    document.getElementById('edit-confirmar-password').value = '';

    const baseUrl = window.location.pathname.startsWith('/secretario') ? '/secretario/cliente' : '/admin/cliente';
    document.getElementById('form-editar').action = `${baseUrl}/actualizar/${id}`;

    limpiarErrores('form-editar');
    const modalEl = document.getElementById('modalEditar');
    if (modalEl) {
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    }
}

function abrirModalNuevo() {
    const modalEl = document.getElementById('modalNuevo');
    if (modalEl) {
        const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
        modal.show();
    }
}

window.prepararEdicionCliente = prepararEdicionCliente;
window.abrirModalNuevo = abrirModalNuevo;