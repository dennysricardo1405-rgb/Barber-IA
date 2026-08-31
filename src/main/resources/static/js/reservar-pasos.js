// ── Estado global de la reserva ──────────────────────────────────────────────
const reserva = {
    servicioId: null, servicioNombre: '', servicioPrecio: '',
    barberoId: null, barberoNombre: '',
    fecha: '', hora: ''
};

// ── Navegación entre pasos ────────────────────────────────────────────────────
function irPaso(n) {
    document.querySelectorAll('.step-panel').forEach((p, i) => {
        p.classList.toggle('visible', i + 1 === n);
    });
    actualizarProgress(n);
    if (n === 4) rellenarResumen();
}

function actualizarProgress(paso) {
    for (let i = 1; i <= 4; i++) {
        const dot = document.getElementById('dot' + i);
        if (i < paso) {
            dot.classList.add('done');
            dot.classList.remove('active');
            dot.innerHTML = '<i class="fa-solid fa-check" style="font-size:0.75rem"></i>';
        } else if (i === paso) {
            dot.classList.add('active');
            dot.classList.remove('done');
            dot.textContent = i;
        } else {
            dot.classList.remove('active', 'done');
            dot.textContent = i;
        }
    }
    for (let i = 1; i <= 3; i++) {
        document.getElementById('line' + i).classList.toggle('done', i < paso);
    }
}

// ── Paso 1: Seleccionar servicio ──────────────────────────────────────────────
function seleccionarServicio(el) {
    document.querySelectorAll('.option-card').forEach(c => c.classList.remove('selected'));
    el.classList.add('selected');
    reserva.servicioId = el.dataset.id;
    reserva.servicioNombre = el.dataset.nombre;
    reserva.servicioPrecio = el.dataset.precio;
    document.getElementById('btnSiguiente1').disabled = false;
}

// ── Paso 2: Seleccionar barbero ───────────────────────────────────────────────
function seleccionarBarbero(el) {
    document.querySelectorAll('.barbero-card').forEach(c => {
        c.classList.remove('selected');
        const icon = c.querySelector('.check-icon');
        if (icon) icon.style.display = 'none';
    });
    el.classList.add('selected');
    const icon = el.querySelector('.check-icon');
    if (icon) icon.style.display = 'block';
    reserva.barberoId = el.dataset.id;
    reserva.barberoNombre = el.dataset.nombre;
    document.getElementById('btnSiguiente2').disabled = false;
}

// ── Paso 3: Fecha mínima y carga de horas ────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    const ahora = new Date();
    // Fix zona horaria: evita que tome UTC y muestre "mañana" en Perú
    const hoy = new Date(ahora.getTime() - ahora.getTimezoneOffset() * 60000);
    const hoyStr = hoy.toISOString().split('T')[0];

    document.getElementById('fechaInput').min = hoyStr;

    const mesesPermitidos = 2;
    const fechaMax = new Date(ahora.getFullYear(), ahora.getMonth() + 1 + mesesPermitidos, 0);
    // Usar getFullYear/Month/Date directamente para evitar el timezone offset
    const fechaMaxStr = [
        fechaMax.getFullYear(),
        String(fechaMax.getMonth() + 1).padStart(2, '0'),
        String(fechaMax.getDate()).padStart(2, '0')
    ].join('-');
    document.getElementById('fechaInput').max = fechaMaxStr;
});

async function cargarHoras() {
    const fecha = document.getElementById('fechaInput').value;
    if (!fecha || !reserva.barberoId) return;

    reserva.fecha = fecha;
    reserva.hora = '';
    document.getElementById('btnSiguiente3').disabled = true;
    document.getElementById('loadingHoras').style.display = 'block';
    document.getElementById('horasContainer').style.display = 'none';
    document.getElementById('sinHoras').style.display = 'none';

    const res = await fetch(`/api/citas/horas-disponibles?barberoId=${reserva.barberoId}&fecha=${fecha}`);
    const data = await res.json();
    const slots = data.slots;

    document.getElementById('loadingHoras').style.display = 'none';

    const disponibles = slots.filter(s => s.disponible);
    if (disponibles.length === 0) {
        document.getElementById('sinHoras').style.display = 'block';
        return;
    }

    const grid = document.getElementById('horasGrid');
    grid.innerHTML = '';

    if (slots.length === 0) {
        document.getElementById('sinHoras').style.display = 'block';
        return;
    }

    slots.forEach(s => {
        const btn = document.createElement('button');
        btn.textContent = s.hora;

        // Fix: usar hora local para comparar si es hoy
        const ahoraLocal = new Date();
        const hoyLocal = new Date(ahoraLocal.getTime() - ahoraLocal.getTimezoneOffset() * 60000)
            .toISOString().split('T')[0];
        const esHoy = document.getElementById('fechaInput').value === hoyLocal;

        const [hh, mm] = s.hora.split(':').map(Number);
        const yaFue = esHoy && (hh < ahoraLocal.getHours() ||
            (hh === ahoraLocal.getHours() && mm <= ahoraLocal.getMinutes()));

        if (yaFue) {
            btn.className = 'hora-btn ocupada confirmada';
            btn.disabled = true;
            btn.title = 'Hora ya pasada';
        } else if (s.disponible) {
            btn.className = 'hora-btn';
            btn.onclick = () => {
                document.querySelectorAll('.hora-btn').forEach(b => b.classList.remove('selected'));
                btn.classList.add('selected');
                reserva.hora = s.hora;
                document.getElementById('btnSiguiente3').disabled = false;
            };
        } else if (s.estadoOcupacion === 'pendiente') {
            btn.className = 'hora-btn ocupada pendiente';
            btn.disabled = true;
            btn.title = 'Reserva en revisión';
        } else {
            btn.className = 'hora-btn ocupada confirmada';
            btn.disabled = true;
            btn.title = 'Horario no disponible';
        }

        grid.appendChild(btn);
    });

    const hayLibres = slots.some(s => s.disponible);
    if (!hayLibres) {
        document.getElementById('sinHoras').style.display = 'block';
    }

    document.getElementById('horasContainer').style.display = 'block';
}

// ── Paso 4: Rellenar resumen ──────────────────────────────────────────────────
function rellenarResumen() {
    document.getElementById('resumenServicio').textContent = reserva.servicioNombre;
    document.getElementById('resumenBarbero').textContent = reserva.barberoNombre;
    document.getElementById('resumenFecha').textContent = formatearFecha(reserva.fecha);
    document.getElementById('resumenHora').textContent = reserva.hora;
    document.getElementById('resumenPrecio').textContent = 'S/. ' + reserva.servicioPrecio;
}

function formatearFecha(f) {
    if (!f) return '—';
    const [y, m, d] = f.split('-');
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return `${d} ${meses[parseInt(m) - 1]} ${y}`;
}

// ── Proceder al pago ──────────────────────────────────────────────────────────
async function procederAlPago() {
    const res = await fetch('/api/citas/pre-reserva', {
        method: 'POST',
        headers: {
            'Content-Type': 'application/json',
            'X-CSRF-TOKEN': getCsrfToken()
        },
        body: JSON.stringify({
            servicioId: reserva.servicioId,
            barberoId: reserva.barberoId,
            fecha: reserva.fecha,
            hora: reserva.hora
        })
    });
    const data = await res.json();
    if (data.requiereAutenticacion) {
        new bootstrap.Modal(document.getElementById('modalAuth')).show();
    } else {
        window.location.href = data.redireccion;
    }
}

// ── Modal Auth: cambiar tabs ──────────────────────────────────────────────────
function showTab(tab) {
    document.getElementById('tabLogin').style.display = tab === 'login' ? 'block' : 'none';
    document.getElementById('tabRegistro').style.display = tab === 'registro' ? 'block' : 'none';
    document.querySelectorAll('.tab-btn').forEach((b, i) => {
        b.classList.toggle('active',
            (i === 0 && tab === 'login') || (i === 1 && tab === 'registro')
        );
    });
}

// ── Login desde modal ─────────────────────────────────────────────────────────
function hacerLogin() {
    const email = document.getElementById('loginEmail').value.trim();
    const pass = document.getElementById('loginPassword').value;
    const errEl = document.getElementById('loginError');
    errEl.style.display = 'none';

    // ── Validaciones ──
    if (!email) {
        errEl.textContent = 'El correo es obligatorio.';
        errEl.style.display = 'block';
        return;
    }
    if (email.length > 30) {
        errEl.textContent = 'El correo no puede superar 30 caracteres.';
        errEl.style.display = 'block';
        return;
    }
    if (!pass || pass.length < 6) {
        errEl.textContent = 'La contraseña debe tener al menos 6 caracteres.';
        errEl.style.display = 'block';
        return;
    }
    if (pass.length > 30) {
        errEl.textContent = 'La contraseña no puede superar 30 caracteres.';
        errEl.style.display = 'block';
        return;
    }

    const form = document.createElement('form');
    form.method = 'POST';
    form.action = '/cliente/login';
    form.innerHTML = `
        <input name="username" value="${email}">
        <input name="password" value="${pass}">
        <input name="_csrf"    value="${getCsrfToken()}">
    `;
    document.body.appendChild(form);
    form.submit();
}

// ── Buscar DNI modal ──────────────────────────────────────────────────────────
async function buscarDniModal() {
    const dni = document.getElementById('regDni').value;

    if (dni.length !== 8) {
        alert('El DNI debe tener 8 dígitos');
        return;
    }

    try {
        const response = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data = await response.json();

        if (data.success) {
            document.getElementById('regNombres').value = data.datos.nombres || '';
            document.getElementById('regApellidos').value =
                (data.datos.ape_paterno || '') + ' ' + (data.datos.ape_materno || '');
        } else {
            alert('No se encontró información para ese DNI');
        }
    } catch (error) {
        console.error(error);
        alert('Error al consultar el DNI');
    }
}

// ── Registro desde modal ──────────────────────────────────────────────────────
async function hacerRegistro() {
    const errEl = document.getElementById('registroError');
    const dni = document.getElementById('regDni').value.trim();
    const nombres = document.getElementById('regNombres').value.trim();
    const correo = document.getElementById('regCorreo').value.trim();
    const pass = document.getElementById('regPassword').value;
    errEl.style.display = 'none';

    // ── Validaciones ──
    if (!dni || dni.length !== 8) {
        errEl.textContent = 'Debes buscar un DNI válido de 8 dígitos.';
        errEl.style.display = 'block';
        return;
    }
    if (!nombres) {
        errEl.textContent = 'Debes buscar el DNI primero para obtener los nombres.';
        errEl.style.display = 'block';
        return;
    }
    if (!correo) {
        errEl.textContent = 'El correo es obligatorio.';
        errEl.style.display = 'block';
        return;
    }
    if (correo.length > 30) {
        errEl.textContent = 'El correo no puede superar 30 caracteres.';
        errEl.style.display = 'block';
        return;
    }
    if (!pass || pass.length < 6) {
        errEl.textContent = 'La contraseña debe tener al menos 6 caracteres.';
        errEl.style.display = 'block';
        return;
    }
    if (pass.length > 30) {
        errEl.textContent = 'La contraseña no puede superar 30 caracteres.';
        errEl.style.display = 'block';
        return;
    }

    const body = new URLSearchParams({
        dni: dni,
        nombres: nombres,
        apellidos: document.getElementById('regApellidos').value,
        telefono: document.getElementById('regTelefono').value,
        correo: correo,
        passwordPlana: pass,
        _csrf: getCsrfToken()
    });

    const res = await fetch('/cliente/registro', { method: 'POST', body });
    if (res.redirected && res.url.includes('login')) {
        document.getElementById('loginEmail').value = correo;
        document.getElementById('loginPassword').value = pass;
        showTab('login');
        hacerLogin();
    } else {
        errEl.textContent = 'Error al registrarse. Verifica los datos.';
        errEl.style.display = 'block';
    }
}

// ── Helper CSRF ───────────────────────────────────────────────────────────────
function getCsrfToken() {
    return document.querySelector('meta[name="_csrf"]')?.content || '';
}