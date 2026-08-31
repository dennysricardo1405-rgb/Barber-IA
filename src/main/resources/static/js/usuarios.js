// ── Contador visual de usuarios ──────────────────────────────
async function actualizarContadorUsuarios() {
    try {
        const response = await fetch('/admin/usuarios/count');
        const data     = await response.json();
        const total    = data.total;
        const maximo   = 5;
        const porcentaje = (total / maximo) * 100;

        const badge = document.getElementById('badgeContador');
        if (badge) badge.textContent = `${total}/${maximo}`;

        const barra = document.getElementById('barraUsuarios');
        const texto = document.getElementById('textoContador');
        if (barra) {
            barra.style.width           = porcentaje + '%';
            barra.style.backgroundColor = total >= maximo ? '#ef4444' : '#f97316';
        }
        if (texto) {
            texto.textContent = `${total} de ${maximo} usuario${total !== 1 ? 's' : ''}`;
        }
    } catch (error) {
        console.error('Error al obtener contador:', error);
    }
}

actualizarContadorUsuarios();

// ── Toggle mostrar/ocultar contraseña ────────────────────────
function togglePassword(inputId, btn) {
    const input = document.getElementById(inputId);
    const icon  = btn.querySelector('i');
    if (!input) return;
    if (input.type === 'password') {
        input.type = 'text';
        icon.classList.replace('fa-eye', 'fa-eye-slash');
    } else {
        input.type = 'password';
        icon.classList.replace('fa-eye-slash', 'fa-eye');
    }
}

// ── Ensamblar email completo antes de enviar ─────────────────
function ensamblarEmail() {
    const prefijo = document.getElementById('nuevoEmailPrefijo').value.trim();
    if (!prefijo) return false;
    document.getElementById('nuevoEmailCompleto').value = prefijo + '@gmail.com';
    return true;
}

function ensamblarEmailEditar() {
    const prefijo = document.getElementById('editEmailPrefijo').value.trim();
    if (!prefijo) return false;
    document.getElementById('editEmailCompleto').value = prefijo + '@gmail.com';
    return true;
}

// ── Botón Nuevo Usuario ──────────────────────────────────────
document.getElementById('btnNuevoUsuario').addEventListener('click', async function () {
    try {
        const response = await fetch('/admin/usuarios/count');
        const data     = await response.json();

        if (data.total >= 5) {
            Swal.fire({
                icon: 'warning',
                title: '¡Límite alcanzado!',
                text: 'No puedes agregar más usuarios. El máximo permitido es 5.',
                confirmButtonColor: '#f97316',
                confirmButtonText: 'Entendido',
                background: '#1e1e2e',
                color: '#ffffff'
            });
            return;
        }

        // Limpiar formulario
        document.getElementById('nuevoEmailPrefijo').value = '';
        document.getElementById('nuevoEmailCompleto').value = '';

        const pass = document.getElementById('nuevoPassword');
        if (pass) {
            pass.value = '';
            pass.type  = 'password';
        }
        const iconNuevo = document.getElementById('iconOjoNuevo');
        if (iconNuevo) {
            iconNuevo.classList.remove('fa-eye-slash');
            iconNuevo.classList.add('fa-eye');
        }

        bootstrap.Modal.getOrCreateInstance(
            document.getElementById('modalUsuario')
        ).show();

    } catch (error) {
        console.error('Error al verificar usuarios:', error);
    }
});

// ── Validar y ensamblar al submit del form nuevo ─────────────
document.addEventListener('DOMContentLoaded', () => {

    // Submit modal nuevo
    const formNuevo = document.querySelector('#modalUsuario form');
    if (formNuevo) {
        formNuevo.addEventListener('submit', function (e) {
            const prefijo = document.getElementById('nuevoEmailPrefijo').value.trim();
            if (!prefijo) {
                e.preventDefault();
                alert('El email es obligatorio.');
                return;
            }
            document.getElementById('nuevoEmailCompleto').value = prefijo + '@gmail.com';
        });
    }

    // Submit modal editar
    const formEditar = document.querySelector('#modalEditar form');
    if (formEditar) {
        formEditar.addEventListener('submit', function (e) {
            const prefijo = document.getElementById('editEmailPrefijo').value.trim();
            if (!prefijo) {
                e.preventDefault();
                alert('El email es obligatorio.');
                return;
            }
            document.getElementById('editEmailCompleto').value = prefijo + '@gmail.com';
        });
    }

    // Limpiar al cerrar modal nuevo
    document.getElementById('modalUsuario')?.addEventListener('hidden.bs.modal', () => {
        document.getElementById('nuevoEmailPrefijo').value  = '';
        document.getElementById('nuevoEmailCompleto').value = '';
        const pass = document.getElementById('nuevoPassword');
        if (pass) pass.type = 'password';
        const icon = document.getElementById('iconOjoNuevo');
        if (icon) { icon.classList.remove('fa-eye-slash'); icon.classList.add('fa-eye'); }
    });

    // Limpiar al cerrar modal editar
    document.getElementById('modalEditar')?.addEventListener('hidden.bs.modal', () => {
        const pass = document.getElementById('editPassword');
        if (pass) { pass.value = ''; pass.type = 'password'; }
        const icon = document.getElementById('iconOjoEditar');
        if (icon) { icon.classList.remove('fa-eye-slash'); icon.classList.add('fa-eye'); }
    });
});

// ── Preparar edición ─────────────────────────────────────────
function prepararEdicion(btn) {
    const id      = btn.dataset.id;
    const nombre  = btn.dataset.nombre;
    const email   = btn.dataset.email;
    const perfil  = btn.dataset.perfil;
    const esAdmin = btn.dataset.esAdmin === 'true';

    document.getElementById('editId').value     = id;
    document.getElementById('editNombre').value = nombre;

    // ← Separar el prefijo del @gmail.com para mostrarlo en el campo
    const prefijo = email.includes('@') ? email.split('@')[0] : email;
    document.getElementById('editEmailPrefijo').value  = prefijo;
    document.getElementById('editEmailCompleto').value = email;

    // Limpiar contraseña y su ojo
    const editPass = document.getElementById('editPassword');
    if (editPass) { editPass.value = ''; editPass.type = 'password'; }
    const iconOjo = document.getElementById('iconOjoEditar');
    if (iconOjo) { iconOjo.classList.remove('fa-eye-slash'); iconOjo.classList.add('fa-eye'); }

    // Rol
    if (esAdmin) {
        document.getElementById('wrapperRolAdmin').style.display  = 'block';
        document.getElementById('wrapperRolSelect').style.display = 'none';
        document.getElementById('editPerfilHidden').value         = perfil;
        document.getElementById('editPerfil').removeAttribute('required');
    } else {
        document.getElementById('wrapperRolAdmin').style.display  = 'none';
        document.getElementById('wrapperRolSelect').style.display = 'block';
        document.getElementById('editPerfil').setAttribute('required', 'required');
        document.getElementById('editPerfil').value = perfil;
    }

    bootstrap.Modal.getOrCreateInstance(document.getElementById('modalEditar')).show();
}