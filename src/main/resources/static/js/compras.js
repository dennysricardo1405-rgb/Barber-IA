function seleccionarEscenario(valor) {
    document.getElementById("tipoCompra").value = valor;

    const btnPaquete = document.getElementById("btnEscenarioPaquete");
    const btnUnidad = document.getElementById("btnEscenarioUnidad");

    if (valor === "PAQUETE") {
        btnPaquete.classList.add("active");
        btnUnidad.classList.remove("active");
    } else {
        btnUnidad.classList.add("active");
        btnPaquete.classList.remove("active");
    }

    alternarCamposCompra(valor);
}

function alternarCamposCompra(tipo) {
    const labelCantidad = document.getElementById("labelCantidad");
    const labelPrecioCompra = document.getElementById("labelPrecioCompra");
    const grupoUnidades = document.getElementById("grupoUnidadesPorPaquete");
    const inputUnidades = document.getElementById("unidadesPorPaquete");
    const titleEscenario = document.getElementById("titleEscenario");

    if (!tipo) {
        tipo = document.getElementById("tipoCompra").value;
    }

    if (tipo === "PAQUETE") {
        titleEscenario.innerText = "Especificaciones de Lote (Por Caja / Paquete)";
        labelCantidad.innerText = "Cantidad Cajas/Paquetes";
        labelPrecioCompra.innerText = "Costo por Caja";

        // Mostrar unidades por paquete
        grupoUnidades.style.setProperty("display", "block", "important");
        inputUnidades.setAttribute("required", "required");
    } else {
        titleEscenario.innerText = "Especificaciones Unitarias (Unidades Sueltas)";
        labelCantidad.innerText = "Total Unidades Compradas";
        labelPrecioCompra.innerText = "Costo por Unidad";

        // Ocultar por completo unidades por paquete (Soluciona tu bug de la captura)
        grupoUnidades.style.setProperty("display", "none", "important");
        inputUnidades.removeAttribute("required");
        inputUnidades.value = "";
    }
}

function seleccionarProductoDelBuscador(elemento) {
    const id = elemento.getAttribute("data-id");
    const nombre = elemento.getAttribute("data-nombre");
    const precioActual = elemento.getAttribute("data-precio");

    // Inyectamos los valores al formulario oculto y visual del modal principal
    document.getElementById("productoIdDestino").value = id;
    document.getElementById("productoNombreVisual").value = nombre;

    const txtVenta = document.getElementById("precioVentaUnidad");
    const valorPrecio = parseFloat(precioActual);

    // CONTROL INTELIGENTE: Si el precio es mayor a 0, lo precarga automáticamente.
    // Si es 0.00 (producto nuevo o sin precio fijado), deja la caja vacía para que el usuario digite sin trabas.
    if (!isNaN(valorPrecio) && valorPrecio > 0) {
        txtVenta.value = valorPrecio.toFixed(2);
        // Ocultamos el error por si acaso estaba encendido
        document.getElementById("errorVenta").classList.add("d-none");
        txtVenta.style.borderColor = "rgba(255, 255, 255, 0.07)";
    } else {
        txtVenta.value = ""; // Dejar vacío en lugar de clavar un cero molesto
        txtVenta.placeholder = "0.00";
    }

    // Regresar de forma nativa al modal principal de compras
    const modalBuscar = bootstrap.Modal.getInstance(document.getElementById('modalBuscarProducto'));
    modalBuscar.hide();

    const modalPrincipal = new bootstrap.Modal(document.getElementById('modalNuevaCompra'));
    modalPrincipal.show();
}

function filtrarProductosCatalogo() {
    const filtro = document.getElementById("inputBuscarFiltro").value.toLowerCase();
    const filas = document.getElementsByClassName("item-producto-fila");

    for (let i = 0; i < filas.length; i++) {
        const nombre = filas[i].getAttribute("data-nombre").toLowerCase();
        const categoria = filas[i].getAttribute("data-categoria").toLowerCase();

        if (nombre.includes(filtro) || categoria.includes(filtro)) {
            filas[i].style.setProperty("display", "block", "important");
        } else {
            filas[i].style.setProperty("display", "none", "important");
        }
    }
}

// Inicialización limpia al arrancar la vista
document.addEventListener("DOMContentLoaded", function () {
    // Forzamos el arranque en PAQUETE por defecto de forma limpia
    seleccionarEscenario("PAQUETE");
});

// Interceptamos el envío del formulario para validar los precios en caliente
document.addEventListener("DOMContentLoaded", function () {
    const form = document.querySelector("form[action*='compras/guardar']");

    if (form) {
        form.addEventListener("submit", function (event) {
            let tieneErrores = false;

            // Jalamos los valores de los inputs
            const txtVenta = document.getElementById("precioVentaUnidad");
            const txtCompra = document.getElementById("precioCompraPaquete");

            // Jalamos las etiquetas de error en rojo
            const errorVenta = document.getElementById("errorVenta");
            const errorCompra = document.getElementById("errorCompra");

            // Validar Precio Venta
            if (parseFloat(txtVenta.value) <= 0 || txtVenta.value.trim() === "") {
                errorVenta.classList.remove("d-none"); // Muestra las letras rojas
                txtVenta.style.borderColor = "#dc3545"; // Pinta el borde de rojo
                tieneErrores = true;
            } else {
                errorVenta.classList.add("d-none");
                txtVenta.style.borderColor = "rgba(255, 255, 255, 0.07)";
            }

            // Validar Costo Compra
            if (parseFloat(txtCompra.value) <= 0 || txtCompra.value.trim() === "") {
                errorCompra.classList.remove("d-none"); // Muestra las letras rojas
                txtCompra.style.borderColor = "#dc3545"; // Pinta el borde de rojo
                tieneErrores = true;
            } else {
                errorCompra.classList.add("d-none");
                txtCompra.style.borderColor = "rgba(255, 255, 255, 0.07)";
            }

            // Si hay algún error, frenamos el envío por completo
            if (tieneErrores) {
                event.preventDefault();
            }
        });
    }
});

function toggleProveedor(esDirecta) {
    const wrapperProv = document.getElementById('wrapperProveedor');
    const badgeDirecta = document.getElementById('badgeCompraDirecta');
    const selectProveedor = document.getElementById('selectProveedor');

    if (esDirecta) {
        wrapperProv.style.display = 'none';
        badgeDirecta.style.display = 'block';
        selectProveedor.value = '';
        selectProveedor.removeAttribute('required');
    } else {
        wrapperProv.style.display = 'block';
        badgeDirecta.style.display = 'none';
        selectProveedor.setAttribute('required', 'required');
    }
}