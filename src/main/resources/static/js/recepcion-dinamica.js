let modalOcupar = null;
let modalGestionar = null;

const HEADERS_JSON = { 'Content-Type': 'application/json' };

// ── CSRF ──────────────────────────────────────────────────────
function getCsrf() {
    return document.querySelector('meta[name="_csrf"]')?.content
        || document.querySelector('input[name="_csrf"]')?.value
        || '';
}

// ── TOAST FLOTANTE ────────────────────────────────────────────
function mostrarToast(tipo, titulo, mensaje) {
    const colores = {
        exito: { bg: '#0f2e1a', border: '#2ecc71', color: '#2ecc71', icon: 'circle-check' },
        error: { bg: '#2e0f0f', border: '#e74c3c', color: '#e74c3c', icon: 'circle-xmark' },
        warning: { bg: '#2e2200', border: '#f39c12', color: '#f39c12', icon: 'triangle-exclamation' },
        info: { bg: '#0f1e2e', border: '#3498db', color: '#3498db', icon: 'circle-info' }
    };
    const c = colores[tipo] || colores.info;

    let contenedor = document.getElementById('toastContenedor');
    if (!contenedor) {
        contenedor = document.createElement('div');
        contenedor.id = 'toastContenedor';
        contenedor.style.cssText = 'position:fixed;bottom:24px;right:24px;z-index:9999;display:flex;flex-direction:column;gap:10px;min-width:300px;max-width:380px;';
        document.body.appendChild(contenedor);
    }

    const toast = document.createElement('div');
    toast.style.cssText = `
        background:${c.bg}; border:1px solid ${c.border}; color:${c.text || '#fff'};
        border-radius:12px; padding:14px 18px;
        display:flex; align-items:flex-start; gap:12px;
        box-shadow:0 8px 24px rgba(0,0,0,0.4);
        animation: slideIn 0.3s ease;
    `;
    toast.innerHTML = `
        <i class="fa-solid fa-${c.icon} fa-lg mt-1" style="color:${c.color}; flex-shrink:0;"></i>
        <div style="flex:1;">
            <div style="font-weight:700; color:${c.color}; font-size:0.9rem;">${titulo}</div>
            ${mensaje ? `<div style="color:rgba(255,255,255,0.7); font-size:0.82rem; margin-top:3px;">${mensaje}</div>` : ''}
        </div>
        <button onclick="this.parentElement.remove()" style="background:none;border:none;color:rgba(255,255,255,0.4);cursor:pointer;font-size:1rem;line-height:1;padding:0;">✕</button>
    `;

    contenedor.appendChild(toast);
    setTimeout(() => { toast.style.opacity = '0'; toast.style.transition = 'opacity 0.4s'; setTimeout(() => toast.remove(), 400); }, 4500);
}

if (!document.getElementById('toastAnim')) {
    const style = document.createElement('style');
    style.id = 'toastAnim';
    style.textContent = `@keyframes slideIn { from { opacity:0; transform:translateX(30px); } to { opacity:1; transform:translateX(0); } }`;
    document.head.appendChild(style);
}

// ── CONFIRMAR ACCIÓN SILLA ─────────────────────────────────────
function confirmarAccion(mensaje, onAceptar) {
    let modal = document.getElementById('modalConfirmacion');
    if (!modal) {
        modal = document.createElement('div');
        modal.id = 'modalConfirmacion';
        modal.innerHTML = `
        <div style="position:fixed;inset:0;background:rgba(0,0,0,0.7);z-index:10000;display:flex;align-items:center;justify-content:center;">
            <div style="background:#111;border:1px solid rgba(201,168,76,0.3);border-radius:16px;padding:28px 32px;max-width:380px;width:90%;text-align:center;">
                <i class="fa-solid fa-circle-question fa-2x mb-3" style="color:#c9a84c;"></i>
                <p id="textoConfirmacion" style="color:#f0ece0;font-size:0.95rem;margin-bottom:24px;"></p>
                <div style="display:flex;gap:12px;justify-content:center;">
                    <button id="btnCancelarConfirm" style="background:transparent;border:1px solid rgba(255,255,255,0.2);color:#aaa;padding:10px 24px;border-radius:8px;cursor:pointer;font-weight:600;">Cancelar</button>
                    <button id="btnAceptarConfirm" style="background:linear-gradient(135deg,#c9a84c,#9a7a30);color:#0a0a0a;padding:10px 24px;border-radius:8px;cursor:pointer;font-weight:700;border:none;">Confirmar</button>
                </div>
            </div>
        </div>`;
        document.body.appendChild(modal);
    }

    document.getElementById('textoConfirmacion').textContent = mensaje;
    modal.style.display = 'flex';

    document.getElementById('btnCancelarConfirm').onclick = () => { modal.style.display = 'none'; };
    document.getElementById('btnAceptarConfirm').onclick = () => { modal.style.display = 'none'; onAceptar(); };
}

document.addEventListener('DOMContentLoaded', () => {
    const elOcupar = document.getElementById('modalOcuparSilla');
    const elGestionar = document.getElementById('modalGestionarSilla');
    if (elOcupar) modalOcupar = new bootstrap.Modal(elOcupar);
    if (elGestionar) modalGestionar = new bootstrap.Modal(elGestionar);

    document.addEventListener('click', e => {
        const btnOcupar = e.target.closest('.btn-ocupar');
        if (btnOcupar) {
            abrirModalOcupar(btnOcupar.dataset.id, btnOcupar.dataset.nombre);
            return;
        }

        const btnConReserva = e.target.closest('.btn-ocupar-con-reserva');
        if (btnConReserva) {
            const barberoId = btnConReserva.dataset.id;
            const barberoNombre = btnConReserva.dataset.nombre;
            const horaReserva = btnConReserva.dataset.horaReserva;

            fetch(`/secretario/recepcion/api-estado-barbero/${barberoId}`)
                .then(r => r.json())
                .then(data => {
                    if (data.bloqueado) {
                        mostrarToast('warning', 'Reserva próxima', `${data.cliente} tiene cita a las ${horaReserva} (en ${data.minutosRestantes} min). Usa "Atender Reserva".`);
                    } else {
                        const mins = data.minutosRestantes;
                        const aviso = mins > 0 ? `⚠ Hay reserva a las ${horaReserva} (en ${mins} min).` : '';
                        confirmarAccion(`Iniciar Reserva con ${barberoNombre}. ${aviso} ¿Continuar?`, () => abrirModalOcupar(barberoId, barberoNombre));
                    }
                })
                .catch(() => abrirModalOcupar(barberoId, barberoNombre));
            return;
        }

        const btnGestionar = e.target.closest('.btn-gestionar');
        if (btnGestionar) {
            abrirModalGestionar(btnGestionar.dataset.id, btnGestionar.dataset.nombre);
            return;
        }
    });
});

function abrirModalOcupar(barberoId, barberoNombre) {
    document.getElementById('modalOcuparBarberoId').value = barberoId;
    document.getElementById('modalOcuparBarberoNombre').textContent = barberoNombre;
    document.getElementById('selectServicio').value = '';
    modalOcupar.show();
}

function abrirModalGestionar(id, nombre) {
    document.getElementById('modalGestionarBarberoNombre').innerText = nombre;
    document.getElementById('consumoBarberoId').value = id;
    limpiarClienteGestion();

    seleccionarMetodoPago('EFECTIVO');
    const inputYape = document.getElementById('inputMontoYape');
    const inputCod = document.getElementById('inputCodigoYape');
    if (inputYape) inputYape.value = '';
    if (inputCod) inputCod.value = '';
    const anticipoDiv = document.getElementById('anticipoYapeInfo');
    if (anticipoDiv) anticipoDiv.style.display = 'none';

    switchGestionTab('existente');
    cargarConsumosDeSilla(id);
    modalGestionar.show();
}

let serviciosEnUsoSilla = [];

function cargarConsumosDeSilla(barberoId) {
    fetch(`/secretario/recepcion/api-consumos/${barberoId}`)
        .then(r => r.json())
        .then(data => {
            serviciosEnUsoSilla = [];
            if (data.servicio && data.servicio.id) {
                serviciosEnUsoSilla.push(String(data.servicio.id));
            }
            if (data.consumos) {
                data.consumos.forEach(c => {
                    if (c.tipo === 'SERVICIO' && c.servicioId) {
                        serviciosEnUsoSilla.push(String(c.servicioId));
                    }
                });
            }

            // Jalamos directamente los precios estructurados que envía el backend
            const precioFinal = data.servicio ? Number(data.servicio.precio) : 0;
            const precioOriginal = data.servicio ? Number(data.servicio.precioOriginal) : precioFinal;

            const labelServicio = document.getElementById('labelServicioActual');
            if (labelServicio) {
                labelServicio.textContent = data.servicio ? data.servicio.nombre + '  ·  S/ ' + precioFinal.toFixed(2) : '—';
            }

            if (data.cliente) {
                document.getElementById('clienteGestionNombre').textContent = data.cliente.nombres + ' ' + data.cliente.apellidos + ' — ' + data.cliente.dni;
                document.getElementById('clienteGestionInfo').style.display = 'block';
            }

            const tbody = document.getElementById('tablaDetalleConsumos');
            tbody.innerHTML = '';

            // ── RENDERIZADO DEL SERVICIO CON PRECIO TACHADO SI TIENE PROMO ──
            if (data.servicio) {
                let columnaSubtotalHtml = ``;

                // Si el precio cobrado es menor al original de la base de datos, mostramos el tachado
                if (precioFinal < precioOriginal) {
                    columnaSubtotalHtml = `
                        <span style="text-decoration: line-through; color: rgba(255,255,255,0.3); font-size: 0.8rem; margin-right: 6px;">S/ ${precioOriginal.toFixed(2)}</span>
                        <span class="text-warning fw-bold">S/ ${precioFinal.toFixed(2)}</span>
                    `;
                } else {
                    columnaSubtotalHtml = `<span class="text-success fw-bold">S/ ${precioOriginal.toFixed(2)}</span>`;
                }

                const esPromo = precioFinal < precioOriginal;
                tbody.innerHTML += `
                <tr style="border-left:3px solid #c9a84c;">
                    <td>
                        <i class="fa-solid fa-scissors me-1 text-warning small"></i>
                        <span class="fw-bold text-warning">${data.servicio.nombre}</span>
                        <span class="badge bg-secondary ms-1" style="font-size:0.6rem;">SERVICIO</span>
                        ${esPromo ? '<span class="badge bg-warning text-dark ms-1 fw-bold" style="font-size:0.65rem;"><i class="fa-solid fa-tag me-1"></i>PROMO</span>' : ''}
                    </td>
                    <td class="text-center">1</td>
                    <td class="text-end">S/ ${precioOriginal.toFixed(2)}</td>
                    <td class="text-end">${columnaSubtotalHtml}</td>
                    <td class="text-center"><i class="fa-solid fa-lock text-muted small"></i></td>
                </tr>`;
            }

            // Renderizado de consumos existentes (Productos y Servicios Adicionales)
            if (!data.consumos || data.consumos.length === 0) {
                tbody.innerHTML += `<tr><td colspan="5" class="text-center text-muted py-2 small"><i class="fa-solid fa-box-open me-1"></i>Sin consumos o productos adicionales</td></tr>`;
            } else {
                data.consumos.forEach(c => {
                    const esServicioExtra = c.tipo === 'SERVICIO';
                    const badgeTipoHtml = esServicioExtra 
                        ? `<span class="badge bg-warning text-dark ms-1" style="font-size:0.6rem;">SERVICIO EXTRA</span>`
                        : `<span class="badge bg-secondary ms-1" style="font-size:0.6rem;">PRODUCTO</span>`;

                    const icono = esServicioExtra ? 'fa-scissors text-warning' : 'fa-box text-muted';

                    tbody.innerHTML += `
                <tr>
                    <td>
                        <i class="fa-solid ${icono} me-1 small"></i>
                        <span class="${esServicioExtra ? 'fw-bold text-white' : ''}">${c.descripcion}</span>
                        ${badgeTipoHtml}
                    </td>
                    <td class="text-center">${c.cantidad}</td>
                    <td class="text-end">S/ ${Number(c.precioUnit).toFixed(2)}</td>
                    <td class="text-end ${esServicioExtra ? 'text-warning' : 'text-success'} fw-bold">S/ ${Number(c.subtotal).toFixed(2)}</td>
                    <td class="text-center">
                        <button class="btn btn-sm btn-outline-danger py-0 px-2" onclick="quitarConsumoDeSilla(${c.id}, ${barberoId})">
                            <i class="fa-solid fa-xmark"></i>
                        </button>
                    </td>
                </tr>`;
                });
            }

            // ── SOLUCIÓN AL BUG DE DUPLICACIÓN ──
            // data.total ya contiene la suma final (Servicio con/sin descuento + Productos) calculada por el backend
            const totalAcumulado = data.total || 0;

            document.getElementById('textoTotalSilla').textContent = 'S/ ' + totalAcumulado.toFixed(2);

            const anticipoWeb = data.anticipoYape ? parseFloat(data.anticipoYape) : 0;
            document.getElementById('lblAnticipoWeb').innerText = anticipoWeb.toFixed(2);

            mostrarAnticipoYape(data);
        })
        .catch(err => console.error('Error cargando consumos:', err));
}

function agregarProductoDirecto(productoId) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    const cantInput = document.getElementById(`cant-${productoId}`);
    const cantidad = parseInt(cantInput?.value) || 1;

    fetch(`/secretario/recepcion/api-consumos/agregar?barberoId=${barberoId}&productoId=${productoId}&cantidad=${cantidad}`, {
        method: 'POST',
        headers: { 'X-CSRF-TOKEN': getCsrf() }
    })
        .then(r => r.json())
        .then(data => {
            if (data.error) {
                mostrarToast('error', 'Stock insuficiente', data.error);
                return;
            }
            if (cantInput) cantInput.value = 1;
            cargarConsumosDeSilla(barberoId);
            mostrarToast('exito', 'Producto agregado', '');
        })
        .catch(err => mostrarToast('error', 'Error', err.message));
}

function quitarConsumoDeSilla(consumoId, barberoId) {
    confirmarAccion('¿Remover este producto de la cuenta?', () => {
        fetch(`/secretario/recepcion/api-consumos/eliminar/${consumoId}`, {
            method: 'DELETE',
            headers: { 'X-CSRF-TOKEN': getCsrf() }
        })
            .then(r => {
                if (!r.ok) throw new Error('No se pudo eliminar.');
                cargarConsumosDeSilla(barberoId);
                mostrarToast('info', 'Producto removido', '');
            })
            .catch(err => mostrarToast('error', 'Error', err.message));
    });
}

function filtrarProductos(texto) {
    document.querySelectorAll('.fila-producto').forEach(fila => {
        fila.style.display = fila.dataset.nombre.toLowerCase().includes(texto.toLowerCase()) ? '' : 'none';
    });
}

function abrirModalAgregarServicios() {
    document.querySelectorAll('.tarjeta-servicio-modal').forEach(tarjeta => {
        const id = String(tarjeta.dataset.id || '');
        if (serviciosEnUsoSilla.includes(id)) {
            tarjeta.style.display = 'none';
        } else {
            tarjeta.style.display = '';
        }
    });
    const modalEl = document.getElementById('modalAgregarServicio');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

function abrirModalAgregarProductos() {
    const modalEl = document.getElementById('modalAgregarProducto');
    if (modalEl) {
        const modal = new bootstrap.Modal(modalEl);
        modal.show();
    }
}

function agregarServicioDirecto(servicioId) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    fetch(`/secretario/recepcion/api-consumos/agregar-servicio?barberoId=${barberoId}&servicioId=${servicioId}`, {
        method: 'POST',
        headers: { 'X-CSRF-TOKEN': getCsrf() }
    })
        .then(r => r.json())
        .then(data => {
            if (data.error) {
                mostrarToast('error', 'Error', data.error);
                return;
            }
            cargarConsumosDeSilla(barberoId);
            mostrarToast('exito', 'Servicio agregado a la silla', '');
            const modalEl = document.getElementById('modalAgregarServicio');
            if (modalEl) {
                const m = bootstrap.Modal.getInstance(modalEl);
                if (m) m.hide();
            }
        })
        .catch(err => mostrarToast('error', 'Error', err.message));
}

function agregarProductoModalDirecto(productoId) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    const cantInput = document.getElementById(`cantModal-${productoId}`);
    const cantidad = parseInt(cantInput?.value) || 1;

    fetch(`/secretario/recepcion/api-consumos/agregar?barberoId=${barberoId}&productoId=${productoId}&cantidad=${cantidad}`, {
        method: 'POST',
        headers: { 'X-CSRF-TOKEN': getCsrf() }
    })
        .then(r => r.json())
        .then(data => {
            if (data.error) {
                mostrarToast('error', 'Stock insuficiente', data.error);
                return;
            }
            if (cantInput) cantInput.value = 1;
            cargarConsumosDeSilla(barberoId);
            mostrarToast('exito', 'Producto agregado', '');
            const modalEl = document.getElementById('modalAgregarProducto');
            if (modalEl) {
                const m = bootstrap.Modal.getInstance(modalEl);
                if (m) m.hide();
            }
        })
        .catch(err => mostrarToast('error', 'Error', err.message));
}

function filtrarServiciosModal(texto) {
    document.querySelectorAll('.tarjeta-servicio-modal').forEach(tarjeta => {
        tarjeta.style.display = tarjeta.dataset.nombre.toLowerCase().includes(texto.toLowerCase()) ? '' : 'none';
    });
}

function filtrarProductosModal(texto) {
    document.querySelectorAll('.tarjeta-producto-modal').forEach(tarjeta => {
        tarjeta.style.display = tarjeta.dataset.nombre.toLowerCase().includes(texto.toLowerCase()) ? '' : 'none';
    });
}

function switchGestionTab(tab) {
    document.getElementById('gTabExistente').style.display = tab === 'existente' ? 'block' : 'none';
    document.getElementById('gTabNuevo').style.display = tab === 'nuevo' ? 'block' : 'none';
    document.getElementById('gTabLibre').style.display = tab === 'libre' ? 'block' : 'none';

    const btns = { existente: 'gBtnExistente', nuevo: 'gBtnNuevo', libre: 'gBtnLibre' };
    Object.entries(btns).forEach(([key, id]) => {
        const btn = document.getElementById(id);
        if (!btn) return;
        btn.className = key === tab ? 'btn btn-sm fw-bold px-3 rounded-pill btn-warning text-dark' : 'btn btn-sm fw-bold px-3 rounded-pill btn-outline-secondary text-white';
    });
}

let timeoutGestion = null;
function buscarClienteGestion(valor) {
    const resultado = document.getElementById('resultadoBusquedaGestion');
    resultado.style.display = 'none';
    resultado.innerHTML = '';
    if (valor.length < 3) return;

    clearTimeout(timeoutGestion);
    timeoutGestion = setTimeout(() => {
        fetch(`/api/clientes/buscar-dni?q=${encodeURIComponent(valor)}`)
            .then(r => r.json())
            .then(clientes => {
                resultado.style.display = 'block';
                if (!clientes || clientes.length === 0) {
                    resultado.innerHTML = `<span class="text-muted small p-2 d-block">No encontrado.</span>`;
                    return;
                }
                resultado.innerHTML = '';
                clientes.forEach(cli => {
                    const div = document.createElement('div');
                    div.className = 'p-2 rounded-2 mb-1 d-flex justify-content-between align-items-center';
                    div.style.cssText = 'background:#252525; cursor:pointer;';
                    div.innerHTML = `
                    <div>
                        <span class="fw-bold text-white small">${cli.nombres} ${cli.apellidos}</span>
                        <span class="text-muted font-monospace ms-2 small">${cli.dni}</span>
                    </div>
                    <button type="button" class="btn btn-sm btn-success py-0 px-2 rounded-pill small fw-bold">Seleccionar</button>`;
                    div.querySelector('button').onclick = () => asociarClienteGestion(cli.id, `${cli.nombres} ${cli.apellidos}`, cli.dni);
                    resultado.appendChild(div);
                });
            })
            .catch(() => {
                resultado.style.display = 'block';
                resultado.innerHTML = `<span class="text-danger small p-2 d-block">Error al buscar.</span>`;
            });
    }, 350);
}

async function asociarClienteGestion(clienteId, nombre, dni) {
    const barberoId = document.getElementById('consumoBarberoId').value;
    try {
        const res = await fetch(`/secretario/recepcion/asociar-cliente?barberoId=${barberoId}&clienteId=${clienteId}`, { method: 'POST', headers: { 'X-CSRF-TOKEN': getCsrf() } });
        if (!res.ok) {
            const err = await res.json();
            mostrarToast('error', 'Error', err.error || 'No se pudo asociar.');
            return;
        }
        document.getElementById('clienteGestionNombre').textContent = nombre + ' — ' + dni;
        document.getElementById('clienteGestionInfo').style.display = 'block';
        document.getElementById('resultadoBusquedaGestion').style.display = 'none';
        document.getElementById('buscarDniGestion').value = '';
        mostrarToast('exito', 'Cliente asociado', nombre);
    } catch {
        mostrarToast('error', 'Error de conexión', '');
    }
}

function limpiarClienteGestion() {
    const info = document.getElementById('clienteGestionInfo');
    const nombre = document.getElementById('clienteGestionNombre');
    const input = document.getElementById('buscarDniGestion');
    const resultado = document.getElementById('resultadoBusquedaGestion');
    if (info) info.style.display = 'none';
    if (nombre) nombre.textContent = '';
    if (input) input.value = '';
    if (resultado) resultado.style.display = 'none';
}

async function buscarDniGestionNuevo() {
    const dni = document.getElementById('gNuevoDni').value.trim();
    if (dni.length !== 8) return;
    try {
        const res = await fetch(`/api/clientes/consulta-dni/${dni}`);
        const data = await res.json();
        document.getElementById('gNuevoNombres').value = data.nombres || '';
        document.getElementById('gNuevoApellidos').value = data.apellidos || '';
    } catch { }
}

async function registrarYAsociarCliente() {
    const dni = document.getElementById('gNuevoDni').value.trim();
    const nombres = document.getElementById('gNuevoNombres').value.trim();
    const apellidos = document.getElementById('gNuevoApellidos').value.trim();
    const telefono = document.getElementById('gNuevoTelefono').value.trim();
    const correo = document.getElementById('gNuevoCorreo').value.trim();
    const msg = document.getElementById('gNuevoClienteMsg');

    if (!dni || dni.length !== 8) { mostrarMsg(msg, 'error', 'DNI debe tener 8 dígitos.'); return; }
    if (!nombres || !apellidos) { mostrarMsg(msg, 'error', 'Nombres y apellidos obligatorios.'); return; }
    if (!correo) { mostrarMsg(msg, 'error', 'Correo obligatorio.'); return; }

    try {
        const res = await fetch('/api/clientes/guardar-rapido', {
            method: 'POST',
            headers: HEADERS_JSON,
            body: JSON.stringify({ dni, nombres, apellidos, telefono, correo })
        });
        const data = await res.json();
        if (!res.ok) { mostrarMsg(msg, 'error', data.error || 'Error al guardar.'); return; }

        mostrarMsg(msg, 'exito', `Cliente ${data.nombres} guardado.`);
        await asociarClienteGestion(data.id, `${data.nombres} ${data.apellidos}`, data.dni);
        ['gNuevoDni', 'gNuevoNombres', 'gNuevoApellidos', 'gNuevoTelefono', 'gNuevoCorreo'].forEach(id => document.getElementById(id).value = '');
        switchGestionTab('existente');
    } catch {
        mostrarMsg(msg, 'error', 'Error de conexión.');
    }
}

function mostrarMsg(el, tipo, texto) {
    el.style.display = 'block';
    el.className = 'mt-2 small fw-bold ' + (tipo === 'error' ? 'text-danger' : 'text-success');
    el.innerHTML = `<i class="fa-solid fa-${tipo === 'error' ? 'circle-xmark' : 'check'} me-1"></i>${texto}`;
}

async function finalizarAtencion() {
    const barberoId = document.getElementById('consumoBarberoId').value;
    const metodoPago = document.getElementById('selectMetodoPago')?.value || 'EFECTIVO';
    const montoYape = parseFloat(document.getElementById('inputMontoYape')?.value || '0') || 0;
    const codigoYape = document.getElementById('inputCodigoYape')?.value?.trim() || '';
    const netoTexto = document.getElementById('lblNetoPorCobrar')?.innerText || '0';
    const netoPorCobrar = parseFloat(netoTexto) || 0;

    if (metodoPago === 'MIXTO' && montoYape <= 0) {
        mostrarToast('error', 'Falta monto', 'Ingresa el monto pagado por Yape.');
        return;
    }

    if (metodoPago === 'YAPE' && netoPorCobrar > 0 && montoYape <= 0) {
        mostrarToast('error', 'Falta monto', 'Ingresa el monto pagado por Yape.');
        return;
    }

    confirmarAccion('¿Confirmar pago y liberar la silla?', async () => {
        try {
            const params = new URLSearchParams({ metodoPago, montoYape, ...(codigoYape && { codigoYape }) });
            const res = await fetch(`/secretario/recepcion/finalizar-pago/${barberoId}?${params}`, { method: 'GET', redirect: 'manual', headers: { 'X-CSRF-TOKEN': getCsrf() } });

            if (modalGestionar) modalGestionar.hide();

            document.querySelectorAll('.modal-backdrop').forEach(el => el.remove());
            document.body.classList.remove('modal-open');
            document.body.style.overflow = '';
            document.body.style.paddingRight = '';

            const resNota = await fetch(`/secretario/recepcion/ultima-nota?barberoId=${barberoId}`);
            if (resNota.ok) {
                const nota = await resNota.json();
                mostrarResumenNota(nota);
            } else {
                mostrarToast('exito', '¡Pago procesado!', 'Silla liberada correctamente.');
                setTimeout(() => window.location.href = '/secretario/recepcion', 2000);
            }
        } catch (e) {
            mostrarToast('error', 'Error', e.message);
        }
    });
}

function mostrarResumenNota(nota) {
    document.getElementById('resumenNotaId').textContent = '#' + nota.id;
    document.getElementById('resumenNotaFecha').textContent = nota.fecha;
    document.getElementById('resumenNotaCliente').textContent = nota.cliente || 'Sin registro';
    document.getElementById('resumenNotaBarbero').textContent = nota.barbero || '—';
    document.getElementById('resumenNotaTotal').textContent = 'S/ ' + Number(nota.total).toFixed(2);

    const pagoEl = document.getElementById('resumenNotaPago');
    if (pagoEl) {
        let html = '';
        if (nota.metodoPago === 'EFECTIVO') {
            html = `<span style="color:#2ecc71;"><i class="fa-solid fa-money-bill me-1"></i>Efectivo: S/ ${Number(nota.total).toFixed(2)}</span>`;
        } else if (nota.metodoPago === 'YAPE') {
            html = `<span style="color:#6c1ea8;"><i class="fa-solid fa-mobile me-1"></i>Yape: S/ ${Number(nota.montoYape).toFixed(2)}</span>`;
            if (nota.codigoYape) html += `<br><small style="color:rgba(255,255,255,0.4);">Cód: ${nota.codigoYape}</small>`;
        } else if (nota.metodoPago === 'MIXTO') {
            html = `<span style="color:#6c1ea8;"><i class="fa-solid fa-mobile me-1"></i>Yape: S/ ${Number(nota.montoYape).toFixed(2)}</span>`;
            html += `<br><span style="color:#2ecc71;"><i class="fa-solid fa-money-bill me-1"></i>Efectivo: S/ ${Number(nota.montoEfectivo).toFixed(2)}</span>`;
            if (nota.codigoYape) html += `<br><small style="color:rgba(255,255,255,0.4);">Cód Yape: ${nota.codigoYape}</small>`;
        }
        pagoEl.innerHTML = html;
    }

    const tbody = document.getElementById('resumenNotaDetalles');
    tbody.innerHTML = '';
    nota.detalles.forEach(d => {
        tbody.innerHTML += `<tr><td>${d.descripcion}</td><td class="text-center">${d.cantidad}</td><td class="text-end">S/ ${Number(d.subtotal).toFixed(2)}</td></tr>`;
    });

    new bootstrap.Modal(document.getElementById('modalResumenNota')).show();
}

function actualizarDesglosePago() {
    const metodo = document.getElementById('selectMetodoPago')?.value || 'EFECTIVO';
    const totalText = document.getElementById('textoTotalSilla')?.textContent || 'S/ 0';
    const totalAcumulado = parseFloat(totalText.replace('S/ ', '')) || 0;
    const anticipoWeb = parseFloat(document.getElementById('lblAnticipoWeb')?.innerText || '0') || 0;

    const netoPorCobrar = Math.max(0, totalAcumulado - anticipoWeb);
    const lblNeto = document.getElementById('lblNetoPorCobrar');
    if (lblNeto) lblNeto.innerText = netoPorCobrar.toFixed(2);

    const inputMontoYape = document.getElementById('inputMontoYape');
    const panelYape = document.getElementById('panelYape');
    const labelEfectivo = document.getElementById('labelEfectivoRestante');
    const btnLiberar = document.getElementById('btnLiberarSillaFinal');

    if (panelYape) panelYape.style.display = (metodo === 'YAPE' || metodo === 'MIXTO') ? 'block' : 'none';

    if (btnLiberar) {
        btnLiberar.disabled = false;
        btnLiberar.innerHTML = '<i class="fa-solid fa-cash-register me-1"></i> Procesar Pago y Liberar Silla';
    }

    if (metodo === 'EFECTIVO') {
        if (inputMontoYape) { inputMontoYape.value = 0; inputMontoYape.readOnly = true; }
        if (labelEfectivo) labelEfectivo.style.display = 'none';

        // Evitamos cobrar en efectivo si ya se pagó todo por la web, forzando a que usen YAPE
        if (netoPorCobrar === 0 && anticipoWeb > 0 && btnLiberar) {
            btnLiberar.disabled = true;
            btnLiberar.innerText = "⚠️ Ya pagado en web. Selecciona YAPE";
        }
    }
    else if (metodo === 'YAPE') {
        if (inputMontoYape) {
            // Si el neto es 0, el monto extra de Yape en caja es 0
            inputMontoYape.value = netoPorCobrar.toFixed(2);
            inputMontoYape.readOnly = true;
        }
        if (labelEfectivo) labelEfectivo.style.display = 'none';

        // ── CONFIGURACIÓN DE SEGURIDAD ──
        // Si ya está totalmente pagado, no bloqueamos el botón, permitimos procesarlo de inmediato
    }
    else if (metodo === 'MIXTO') {
        if (inputMontoYape) inputMontoYape.readOnly = false;

        let montoYapeDigitado = parseFloat(inputMontoYape?.value || '0') || 0;
        if (montoYapeDigitado < 0) { montoYapeDigitado = 0; inputMontoYape.value = 0; }

        const efectivoRestante = Math.max(0, netoPorCobrar - montoYapeDigitado);
        if (labelEfectivo) {
            labelEfectivo.textContent = `Efectivo restante a cobrar en físico: S/ ${efectivoRestante.toFixed(2)}`;
            labelEfectivo.style.display = 'block';
        }

        // Si el saldo neto ya es 0, no tiene sentido usar el modo Mixto
        if (netoPorCobrar === 0 && btnLiberar) {
            btnLiberar.disabled = true;
            btnLiberar.innerText = "Saldo es S/ 0.00, usa el modo YAPE";
        } else {
            if (montoYapeDigitado >= netoPorCobrar && btnLiberar) {
                btnLiberar.disabled = true;
                btnLiberar.innerText = "En Mixto, Yape debe ser menor al neto";
            }
            if (montoYapeDigitado <= 0 && btnLiberar) {
                btnLiberar.disabled = true;
                btnLiberar.innerText = "Ingresa un monto Yape válido";
            }
        }
    }
}

function seleccionarMetodoPago(metodo) {
    const selMetodo = document.getElementById('selectMetodoPago');
    if (selMetodo) selMetodo.value = metodo;

    document.querySelectorAll('.btn-metodo').forEach(btn => {
        const m = btn.dataset.metodo;
        if (m === metodo) {
            if (m === 'EFECTIVO') btn.style.cssText = 'background:#1a3a27; border:1px solid #2ecc71; color:#2ecc71;';
            else if (m === 'YAPE') btn.style.cssText = 'background:rgba(108,30,168,0.3); border:1px solid #a855f7; color:#a855f7;';
            else btn.style.cssText = 'background:rgba(201,168,76,0.15); border:1px solid #c9a84c; color:#c9a84c;';
        } else {
            if (m === 'EFECTIVO') btn.style.cssText = 'background:transparent; border:1px solid rgba(255,255,255,0.1); color:#aaa;';
            else if (m === 'YAPE') btn.style.cssText = 'background:rgba(108,30,168,0.05); border:1px solid rgba(108,30,168,0.2); color:#888;';
            else btn.style.cssText = 'background:transparent; border:1px solid rgba(255,255,255,0.1); color:#aaa;';
        }
    });

    actualizarDesglosePago();
}

function mostrarAnticipoYape(sesionData) {
    const anticipoDiv = document.getElementById('anticipoYapeInfo');
    const anticipoTexto = document.getElementById('anticipoYapeTexto');

    if (sesionData.anticipoYape && sesionData.anticipoYape > 0) {
        if (anticipoDiv) anticipoDiv.style.display = 'block';
        if (anticipoTexto) {
            anticipoTexto.textContent = `Esta reserva cuenta con un anticipo web de S/ ${Number(sesionData.anticipoYape).toFixed(2)} por Yape` + (sesionData.codigoYape ? ` (Cód: ${sesionData.codigoYape})` : '');
        }

        const totalText = document.getElementById('textoTotalSilla')?.textContent || 'S/ 0';
        const totalAcumulado = parseFloat(totalText.replace('S/ ', '')) || 0;

        if (parseFloat(sesionData.anticipoYape) >= totalAcumulado) {
            seleccionarMetodoPago('YAPE');
        } else {
            seleccionarMetodoPago('MIXTO');
        }
    } else {
        if (anticipoDiv) anticipoDiv.style.display = 'none';
        const lblNeto = document.getElementById('lblNetoPorCobrar');
        if (lblNeto) lblNeto.innerText = parseFloat(document.getElementById('textoTotalSilla')?.textContent.replace('S/ ', '') || 0).toFixed(2);
    }
    actualizarDesglosePago();
}