let modalPagoInstance;

// ── Ver comprobante en modal secundario ───────────────────────────────────
function verComprobante(src) {
    const imgGrande = document.getElementById('comprobanteGrande');
    if (imgGrande) {
        imgGrande.src = src;
        new bootstrap.Modal(document.getElementById('modalComprobante')).show();
    }
}

// ── Obtener Token CSRF de Spring Security ──────────────────────────────────
function getCsrf() {
    return document.querySelector('meta[name="_csrf"]')?.content || '';
}

function abrirModalConfirmacion(button) {
    const id = button.getAttribute('data-id');
    const totalServicio = parseFloat(button.getAttribute('data-total')) || 0;
    const precioOriginal = parseFloat(button.getAttribute('data-precio-original')) || 0;
    const esPromo = button.getAttribute('data-es-promo') === 'true';

    const fila = document.getElementById('fila-' + id);
    const imgElement = fila ? fila.querySelector('.comprobante-thumb') : null;

    document.getElementById('modalCitaId').value = id;
    
    if (document.getElementById('txtTotalServicio')) {
        document.getElementById('txtTotalServicio').innerText = totalServicio.toFixed(2);
    }

    const tagPromo = document.getElementById('tagPromoAplicada');
    const txtOriginal = document.getElementById('txtPrecioOriginalModal');

    if (esPromo) {
        if (tagPromo) tagPromo.style.display = 'inline-block';
        if (txtOriginal) {
            txtOriginal.style.display = 'inline';
            txtOriginal.innerText = 'S/ ' + precioOriginal.toFixed(2);
        }
    } else {
        if (tagPromo) tagPromo.style.display = 'none';
        if (txtOriginal) txtOriginal.style.display = 'none';
    }
    
    const txtCodigo = document.getElementById('txtCodigoDetectado');
    if (txtCodigo) txtCodigo.innerText = "Escaneando...";

    document.getElementById('formMontoYape').value = ""; 
    
    calcularCuadre();

    modalPagoInstance = new bootstrap.Modal(document.getElementById('modalConfirmarPago'));
    modalPagoInstance.show();

    if (imgElement && imgElement.src) {
        ejecutarOcrInteligente(imgElement.src);
    } else {
        if (txtCodigo) txtCodigo.innerText = "NO_DETECTADO";
    }
}
function ejecutarOcrInteligente(imgUrl) {
    if (typeof Tesseract === 'undefined') return;

    Tesseract.recognize(imgUrl, 'eng')
        .then(({ data: { text } }) => {
            // Extrae el código de 8 dígitos continuo de Yape
            const matchCodigo = text.match(/\b\d{8}\b/);
            let codigoDetectado = matchCodigo ? matchCodigo[0] : "NO_DETECTADO";

            if (codigoDetectado === "NO_DETECTADO") {
                const matchFlex = text.match(/(?:operación|nro\.?|nro)\s*:?\s*(\d+)/i);
                if (matchFlex && matchFlex[1]) codigoDetectado = matchFlex[1];
            }

            const txtCodigo = document.getElementById('txtCodigoDetectado');
            if (txtCodigo) txtCodigo.innerText = codigoDetectado;
        })
        .catch(() => {
            const txtCodigo = document.getElementById('txtCodigoDetectado');
            if (txtCodigo) txtCodigo.innerText = "NO_DETECTADO";
        });
}

// ── Calculadora de Cuadre y Alertas de Seguridad ───────────────────────────
function calcularCuadre() {
    const txtTotal = document.getElementById('txtTotalServicio');
    if (!txtTotal) return;

    const total = parseFloat(txtTotal.innerText) || 0;
    let yape = parseFloat(document.getElementById('formMontoYape').value) || 0;

    if (yape < 0) { yape = 0; document.getElementById('formMontoYape').value = 0; }

    const saldo = total - yape; // El saldo es simplemente Total menos Yape 💸
    const box = document.getElementById('boxStatusSaldo');

    if (box) {
        if (saldo === 0) {
            box.style.background = "rgba(46,204,113,0.15)";
            box.style.color = "#2ecc71";
            box.innerHTML = '<h6 class="m-0 font-monospace">✨ ¡SERVICIO CANCELADO COMPLETAMENTE! S/ 0.00</h6>';
        } else if (saldo < 0) {
            box.style.background = "rgba(231,76,60,0.15)";
            box.style.color = "#e74c3c";
            box.innerHTML = '<h6>⚠️ ERROR: El monto supera el costo del servicio</h6>';
        } else {
            box.style.background = "rgba(201,168,76,0.15)";
            box.style.color = "#c9a84c";
            box.innerHTML = `<h6 class="m-0 font-monospace">Saldo Restante para cobrar en Silla: S/ ${saldo.toFixed(2)}</h6>`;
        }
    }
}

// ── Envío AJAX con Popups de Confirmación Contextuales ─────────────────────
async function enviarConfirmacionHibrida() {
    const id = document.getElementById('modalCitaId').value;
    const total = parseFloat(document.getElementById('txtTotalServicio').innerText) || 0;
    const yape = parseFloat(document.getElementById('formMontoYape').value) || 0;
    const efectivo = 0;
    const codigo = document.getElementById('txtCodigoDetectado').innerText;

    // Guardrails de validación rápida
    if (yape < 0) {
        alert("El monto no puede ser negativo.");
        return;
    }
    if (yape > total) {
        alert("Error: El monto ingresado no puede superar el costo del servicio.");
        return;
    }

    const saldo = total - yape;

    // 1. Construir el mensaje contextual según el saldo
    const mensajeTexto = saldo === 0
        ? `El servicio quedará marcado como TOTALMENTE PAGADO (Saldo: S/ 0.00).`
        : `Quedará un saldo pendiente por cobrar en la barbería de S/ ${saldo.toFixed(2)}.`;

    // 2. Inyectar el texto en el modal premium
    document.getElementById('txtMensajeSaldoPremium').innerText = mensajeTexto;

    // 3. Levantar el modal de confirmación premium
    const modalConfirmInstance = new bootstrap.Modal(document.getElementById('modalConfirmarSaldoPremium'));
    modalConfirmInstance.show();

    // 4. Asignar la acción de ejecución real al botón "Aceptar" del nuevo modal
    document.getElementById('btnAceptarSaldoPremium').onclick = async function () {
        modalConfirmInstance.hide(); // Ocultamos el modal de confirmación

        try {
            const res = await fetch(`/secretario/citas/${id}/aceptar?montoYape=${yape}&montoEfectivo=${efectivo}&codigoYape=${codigo}`, {
                method: 'POST',
                headers: { 'X-CSRF-TOKEN': getCsrf() }
            });

            if (res.ok) {
                // Ocultamos el modal principal de auditoría
                if (modalPagoInstance) modalPagoInstance.hide();

                const fila = document.getElementById('fila-' + id);
                if (fila) fila.remove();

                mostrarToast('ok', 'Reserva verificada con éxito.');
            } else {
                mostrarToast('err', 'Error al procesar el abono en el servidor.');
            }
        } catch (e) {
            mostrarToast('err', 'Error de conexión.');
        }
    };
}

// ── Acción para Cancelar/Rechazar Reservas ────────────────────────────────
async function accionCita(id, accion) {
    if (accion === 'aceptar') return; // Se gestiona en el modal
    if (!confirm('¿Seguro que deseas cancelar esta cita?')) return;

    try {
        const res = await fetch(`/secretario/citas/${id}/${accion}`, {
            method: 'POST',
            headers: { 'X-CSRF-TOKEN': getCsrf() }
        });
        if (res.ok) {
            const fila = document.getElementById('fila-' + id);
            if (fila) fila.remove();
            mostrarToast('ok', '❌ Cita cancelada.');
        }
    } catch (e) {
        mostrarToast('err', 'Error de conexión.');
    }
}

// ── Mostrar Notificaciones Flotantes ──────────────────────────────────────
function mostrarToast(tipo, texto) {
    const box = document.getElementById('toastBox');
    if (box) {
        box.innerHTML = `<div class="toast-${tipo}"><span>${texto}</span></div>`;
        box.style.display = 'block';
        setTimeout(() => { box.style.display = 'none'; }, 4500);
    }
}