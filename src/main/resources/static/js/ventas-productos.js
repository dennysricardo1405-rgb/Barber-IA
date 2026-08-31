// ============================================================
// ventas-productos.js — Módulo de Venta Directa en Caja
// ============================================================

let carrito = [];

document.addEventListener('DOMContentLoaded', function () {
    cambiarMetodoPago();
});

// ── AGREGAR AL CARRITO ─────────────────────────────────────────
function agregarAlCarrito(productoId) {
    const el = document.querySelector(`.producto-item[data-id="${productoId}"]`);
    if (!el) return;

    const nombre   = el.getAttribute('data-nombre');
    const precio   = parseFloat(el.getAttribute('data-precio') || '0');
    const stockMax = parseInt(el.getAttribute('data-stock') || '0');

    if (stockMax <= 0) {
        mostrarAlerta('Este producto no tiene stock disponible.', 'danger');
        return;
    }

    const existente = carrito.find(item => item.id === productoId);
    if (existente) {
        if (existente.cantidad >= stockMax) {
            mostrarAlerta(`No puedes agregar más unidades de ${nombre}. Stock disponible: ${stockMax}`, 'warning');
            return;
        }
        existente.cantidad++;
    } else {
        carrito.push({
            id: productoId,
            nombre: nombre,
            precio: precio,
            cantidad: 1,
            stockMax: stockMax
        });
    }

    renderCarrito();
}

// ── ACTUALIZAR CANTIDAD / REMOVER ──────────────────────────────
function cambiarCantidad(productoId, cambio) {
    const item = carrito.find(i => i.id === productoId);
    if (!item) return;

    item.cantidad += cambio;

    if (item.cantidad > item.stockMax) {
        item.cantidad = item.stockMax;
        mostrarAlerta(`Alcanzaste el límite de stock disponible (${item.stockMax}).`, 'warning');
    }

    if (item.cantidad <= 0) {
        carrito = carrito.filter(i => i.id !== productoId);
    }

    renderCarrito();
}

function eliminarDelCarrito(productoId) {
    carrito = carrito.filter(i => i.id !== productoId);
    renderCarrito();
}

function vaciarCarrito() {
    carrito = [];
    renderCarrito();
}

// ── RENDERIZAR CARRITO EN PANTALLA ─────────────────────────────
function renderCarrito() {
    const contenedor = document.getElementById('contenedorItemsCarrito');
    const msgVacio = document.getElementById('carritoVacioMsg');

    if (!contenedor) return;

    if (carrito.length === 0) {
        contenedor.innerHTML = `
            <div class="text-center text-muted py-4" id="carritoVacioMsg">
                <i class="fa-solid fa-cart-arrow-down fa-2x mb-2 opacity-50"></i>
                <div class="small">El carrito está vacío. Haz clic en los productos para agregarlos.</div>
            </div>`;
        actualizarTotales(0);
        return;
    }

    let html = '';
    let total = 0;

    carrito.forEach(item => {
        const subtotalItem = item.precio * item.cantidad;
        total += subtotalItem;

        html += `
            <div class="cart-item d-flex justify-content-between align-items-center">
                <div class="me-2 flex-grow-1" style="min-width: 0;">
                    <div class="fw-bold text-white text-truncate small">${item.nombre}</div>
                    <div class="text-warning small">S/ ${item.precio.toFixed(2)} c/u</div>
                </div>
                <div class="d-flex align-items-center gap-1">
                    <button class="btn btn-sm btn-outline-secondary py-0 px-2 text-white" onclick="cambiarCantidad(${item.id}, -1)">-</button>
                    <span class="px-2 text-white fw-bold small">${item.cantidad}</span>
                    <button class="btn btn-sm btn-outline-secondary py-0 px-2 text-white" onclick="cambiarCantidad(${item.id}, 1)">+</button>
                    <button class="btn btn-sm text-danger ms-1 border-0" onclick="eliminarDelCarrito(${item.id})" title="Quitar">
                        <i class="fa-solid fa-xmark"></i>
                    </button>
                </div>
            </div>`;
    });

    contenedor.innerHTML = html;
    actualizarTotales(total);
}

function actualizarTotales(total) {
    document.getElementById('lblSubtotal').textContent = `S/ ${total.toFixed(2)}`;
    document.getElementById('lblTotal').textContent = `S/ ${total.toFixed(2)}`;
    calcularVuelto();
}

function cambiarMetodoPago() {
    const metodo = document.getElementById('selectMetodoPago').value;
    const secYape = document.getElementById('secYape');

    if (secYape) {
        secYape.style.display = metodo === 'YAPE' ? 'block' : 'none';
    }
}

// ── FILTROS POR CATEGORÍA PRINCIPAL Y SECUNDARIA ───────────────
function filtrarCategorias() {
    const mainCatId = document.getElementById('selectCatPrincipal').value;
    const subCatSelect = document.getElementById('selectCatSecundaria');

    Array.from(subCatSelect.options).forEach(opt => {
        if (!opt.value) return; // opción por defecto
        const padre = opt.getAttribute('data-padre');
        if (!mainCatId || padre === mainCatId) {
            opt.style.display = 'block';
        } else {
            opt.style.display = 'none';
        }
    });

    subCatSelect.value = '';
    filtrarProductos();
}

function filtrarProductos() {
    const mainCatId = document.getElementById('selectCatPrincipal').value;
    const subCatId  = document.getElementById('selectCatSecundaria').value;
    const qSearch   = document.getElementById('inputBuscarProducto').value.trim().toLowerCase();

    const items = document.querySelectorAll('.producto-item');
    let visibles = 0;

    items.forEach(el => {
        const itemMainCat = el.getAttribute('data-cat-principal');
        const itemSubCat  = el.getAttribute('data-cat-secundaria');
        const itemNombre  = el.getAttribute('data-nombre').toLowerCase();

        let cumpleMain = !mainCatId || itemMainCat === mainCatId;
        let cumpleSub  = !subCatId  || itemSubCat === subCatId;
        let cumpleText = !qSearch   || itemNombre.includes(qSearch);

        if (cumpleMain && cumpleSub && cumpleText) {
            el.style.display = 'block';
            visibles++;
        } else {
            el.style.display = 'none';
        }
    });

    const msgNo = document.getElementById('noProductosMsg');
    if (msgNo) {
        msgNo.style.display = visibles === 0 ? 'block' : 'none';
    }
}

// ── PROCESAR VENTA ─────────────────────────────────────────────
async function procesarVenta() {
    if (carrito.length === 0) {
        mostrarAlerta('El carrito está vacío. Agrega productos antes de procesar.', 'warning');
        return;
    }

    const clienteId  = document.getElementById('selectCliente').value;
    const metodoPago = document.getElementById('selectMetodoPago').value;
    const codigoYape = document.getElementById('inputCodigoYape')?.value.trim() || '';
    const total      = parseFloat(document.getElementById('lblTotal').textContent.replace('S/', '').trim()) || 0;

    const btn = document.getElementById('btnProcesarVenta');
    btn.disabled = true;
    btn.innerHTML = '<i class="fa-solid fa-circle-notch fa-spin me-2"></i>Procesando...';

    const payload = {
        clienteId:     clienteId ? parseInt(clienteId) : null,
        metodoPago:    metodoPago,
        montoEfectivo: metodoPago === 'EFECTIVO' ? total : 0,
        montoYape:     metodoPago === 'YAPE' ? total : 0,
        codigoYape:    codigoYape,
        items: carrito.map(i => ({
            productoId: i.id,
            cantidad:   i.cantidad
        }))
    };

    try {
        const csrfToken = document.querySelector('meta[name="_csrf"]')?.getAttribute('content') || '';
        const csrfHeader = document.querySelector('meta[name="_csrf_header"]')?.getAttribute('content') || 'X-CSRF-TOKEN';

        const headers = { 'Content-Type': 'application/json' };
        if (csrfToken && csrfHeader) {
            headers[csrfHeader] = csrfToken;
        }

        const res = await fetch('/secretario/api/ventas-productos/procesar', {
            method: 'POST',
            headers: headers,
            body: JSON.stringify(payload)
        });

        const data = await res.json();

        if (res.ok && data.success) {
            vaciarCarrito();
            mostrarAlerta(data.mensaje || 'Venta procesada con éxito.', 'success');
            if (data.notaId) {
                verDetalleNota(data.notaId);
            }
        } else {
            mostrarAlerta('Error: ' + (data.error || 'No se pudo procesar la venta.'), 'danger');
        }
    } catch (err) {
        mostrarAlerta('Error de conexión al procesar la venta.', 'danger');
    } finally {
        btn.disabled = false;
        btn.innerHTML = '<i class="fa-solid fa-check-circle me-2"></i>Procesar Venta';
    }
}

// ── VER DETALLE NOTA / TICKET TÉRMICO DE ALTO CONTRASTE ─────────
async function verDetalleNota(notaId) {
    const contenedor = document.getElementById('cuerpoModalTicket');
    if (!contenedor) return;

    contenedor.innerHTML = '<div class="text-center py-4"><i class="fa-solid fa-circle-notch fa-spin fa-2x text-warning"></i></div>';

    const modalEl = document.getElementById('modalDetalleTicket');
    const modal = bootstrap.Modal.getInstance(modalEl) || new bootstrap.Modal(modalEl);
    modal.show();

    try {
        const res  = await fetch(`/secretario/recepcion/notas-venta/${notaId}/detalle`);
        const data = await res.json();

        let itemsHtml = '';
        if (data.detalles) {
            data.detalles.forEach(d => {
                itemsHtml += `
                    <tr style="border-bottom: 1px solid #eeeeee;">
                        <td style="padding: 6px 0; color: #111111 !important; font-weight: 600;">${d.descripcion}</td>
                        <td style="padding: 6px 0; color: #111111 !important; text-align: center;">${d.cantidad}</td>
                        <td style="padding: 6px 0; color: #111111 !important; text-align: right;">S/ ${d.precioUnitario.toFixed(2)}</td>
                        <td style="padding: 6px 0; color: #111111 !important; text-align: right; font-weight: bold;">S/ ${d.subtotal.toFixed(2)}</td>
                    </tr>`;
            });
        }

        contenedor.innerHTML = `
            <div style="background-color: #ffffff; color: #111111 !important; padding: 24px; border-radius: 8px; font-family: 'Courier New', Courier, monospace, sans-serif; border: 1px dashed #cccccc; box-shadow: 0 4px 15px rgba(0,0,0,0.2);">
                <div class="text-center mb-3">
                    <h4 style="color: #111111 !important; font-weight: 900; margin-bottom: 2px; letter-spacing: 1px;">BARBERÍA "LA CLÁSICA"</h4>
                    <div style="color: #444444 !important; font-size: 0.9rem; font-weight: 700;">Ticket de Venta #${data.id}</div>
                    <div style="color: #666666 !important; font-size: 0.8rem;">Fecha: ${data.fecha || ''}</div>
                </div>

                <div style="border-top: 1px dashed #888888; border-bottom: 1px dashed #888888; padding: 10px 0; margin-bottom: 14px; font-size: 0.85rem; color: #222222 !important;">
                    <div><strong style="color: #111111 !important;">Cliente:</strong> ${data.cliente || 'Cliente General'}</div>
                    <div><strong style="color: #111111 !important;">Origen / Vendedor:</strong> ${data.barbero ? ('Silla Barbero: ' + data.barbero) : 'Venta Directa (Caja)'}</div>
                    <div><strong style="color: #111111 !important;">Método de Pago:</strong> ${data.metodoPago || 'EFECTIVO'}</div>
                    ${data.codigoYape ? `<div><strong style="color: #111111 !important;">Cod. Yape:</strong> ${data.codigoYape}</div>` : ''}
                </div>

                <table class="table table-sm table-borderless" style="color: #111111 !important; font-size: 0.85rem; margin-bottom: 14px;">
                    <thead>
                        <tr style="border-bottom: 2px solid #111111;">
                            <th style="color: #111111 !important; font-weight: bold;">Producto</th>
                            <th style="color: #111111 !important; font-weight: bold; text-align: center;">Cant.</th>
                            <th style="color: #111111 !important; font-weight: bold; text-align: right;">P.U.</th>
                            <th style="color: #111111 !important; font-weight: bold; text-align: right;">Subtotal</th>
                        </tr>
                    </thead>
                    <tbody>
                        ${itemsHtml}
                    </tbody>
                </table>

                <div style="border-top: 2px solid #111111; padding-top: 10px; display: flex; justify-content: space-between; align-items: center; font-size: 1.15rem; font-weight: 900; color: #111111 !important;">
                    <span style="color: #111111 !important;">TOTAL RECAUDADO:</span>
                    <span style="color: #15803d !important;">S/ ${data.total.toFixed(2)}</span>
                </div>
            </div>`;
    } catch (e) {
        contenedor.innerHTML = '<div class="alert alert-danger mb-0">Error al cargar el detalle del ticket.</div>';
    }
}

function desplazarseAHistorial() {
    document.getElementById('seccionHistorial')?.scrollIntoView({ behavior: 'smooth' });
}

function mostrarAlerta(mensaje, tipo) {
    const alertDiv = document.getElementById('alertaGlobal');
    const alertText = document.getElementById('alertaTexto');
    if (alertDiv && alertText) {
        alertDiv.className = `alert alert-${tipo} alert-dismissible fade show bg-dark border-${tipo} text-${tipo}`;
        alertText.textContent = mensaje;
        alertDiv.style.display = 'block';
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
}

function cerrarAlerta() {
    const alertDiv = document.getElementById('alertaGlobal');
    if (alertDiv) alertDiv.style.display = 'none';
}
