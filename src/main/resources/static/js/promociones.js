document.addEventListener("DOMContentLoaded", () => {
    // ── DECLARACIONES INICIALES DE VARIABLES (El error estaba aquí) ──
    const inputInicio = document.getElementById("promoInicio");
    const inputFin = document.getElementById("promoFin");
    const form = document.getElementById("promoForm");

    const buscador = document.getElementById("buscadorModalProd");
    const itemsProductos = document.querySelectorAll(".item-prod-modal");
    const inputHiddenId = document.getElementById("promoProductoId");
    const inputTextoVisible = document.getElementById("promoProductoTexto");

    // ── 1. VALIDACIÓN DE FORMULARIO AL ENVIAR (SUBMIT) ──
    if (form) {
        form.addEventListener("submit", function (e) {
            const tipo = document.getElementById("promoTipo").value;
            const servicioId = document.getElementById("promoServicioId").value;
            const alcanceProd = document.getElementById("filtroAlcanceProducto") ? document.getElementById("filtroAlcanceProducto").value : "ESPECIFICO";
            const productoId = document.getElementById("promoProductoId") ? document.getElementById("promoProductoId").value : "";
            const categoriaId = document.getElementById("promoCategoriaId") ? document.getElementById("promoCategoriaId").value : "";
            const inicio = inputInicio ? inputInicio.value : "";
            const fin = inputFin ? inputFin.value : "";

            if (tipo === "SERVICIO" && (!servicioId || servicioId.trim() === "")) {
                e.preventDefault();
                alert("Por favor, selecciona el Servicio al cual aplica esta promoción.");
                return false;
            }

            if (tipo === "PRODUCTO") {
                if (alcanceProd === "ESPECIFICO" && (!productoId || productoId.trim() === "")) {
                    e.preventDefault();
                    alert("Por favor, selecciona un Producto específico haciendo clic en 'Buscar'.");
                    return false;
                }
                if (alcanceProd === "CATEGORIA" && (!categoriaId || categoriaId.trim() === "")) {
                    e.preventDefault();
                    alert("Por favor, selecciona una Categoría para aplicar la promoción.");
                    return false;
                }
            }

            if (inicio && fin) {
                const dateInicio = new Date(inicio);
                const dateFin = new Date(fin);
                if (dateFin <= dateInicio) {
                    e.preventDefault();
                    alert("La fecha y hora de fin debe ser posterior a la fecha de inicio.");
                    return false;
                }
            }
        });
    }

    // ── 1.1 VALIDACIÓN DE CALENDARIO (No días pasados, máximo 4 meses) ──
    if (inputInicio && inputFin) {
        const ahora = new Date();
        const offset = ahora.getTimezoneOffset() * 60000;
        const tiempoLocal = new Date(ahora - offset).toISOString().slice(0, 16);

        // Bloqueamos días pasados para el inicio
        inputInicio.min = tiempoLocal;

        inputInicio.addEventListener("change", () => {
            if (inputInicio.value) {
                // El fin no puede ser menor al inicio
                inputFin.min = inputInicio.value;

                // Calculamos el tope máximo de 4 meses exactos en el futuro
                let fechaMax = new Date(inputInicio.value);
                fechaMax.setMonth(fechaMax.getMonth() + 4);
                inputFin.max = fechaMax.toISOString().slice(0, 16);
            }
        });
    }

    // ── 2. FILTRO DINÁMICO DEL BUSCADOR DEL MODAL ──
    if (buscador) {
        buscador.addEventListener("input", function () {
            const termino = this.value.toLowerCase().trim();
            itemsProductos.forEach(item => {
                const nombre = item.dataset.nombre.toLowerCase();
                if (nombre.includes(termino)) {
                    item.style.setProperty("display", "block", "important");
                } else {
                    item.style.setProperty("display", "none", "important");
                }
            });
        });
    }

    // ── 3. ACCIÓN DE SELECCIONAR UN PRODUCTO DESDE EL MODAL ──
    itemsProductos.forEach(item => {
        item.addEventListener("click", function () {
            const id = this.dataset.id;
            const nombre = this.dataset.nombre;
            const precio = this.dataset.precio;
            const stock = this.dataset.stock;

            // Asignamos los valores a los inputs del formulario
            if (inputHiddenId) inputHiddenId.value = id;
            if (inputTextoVisible) inputTextoVisible.value = `${nombre} (S/ ${precio})`;

            recalcularPrecioFinalVistaAdmin();

            // Cerramos el modal de forma limpia usando la API de Bootstrap
            const modalElement = document.getElementById('modalBuscarProducto');
            const modal = bootstrap.Modal.getInstance(modalElement);
            if (modal) modal.hide();
        });
    });

    // ── 4. LOGICA INTEGRAL PARA EL BOTÓN DE EDICIÓN ──
    document.querySelectorAll(".btn-editar-promo").forEach(boton => {
        boton.addEventListener("click", function () {
            // Cambiar título del formulario
            document.getElementById("formTitle").textContent = "Editar Promoción";

            // Extraer los datasets del botón
            const id = this.dataset.id;
            const nombre = this.dataset.nombre;
            const description = this.dataset.descripcion;
            const tipo = this.dataset.tipo;
            const descuento = this.dataset.descuento;
            const inicio = this.dataset.inicio;
            const fin = this.dataset.fin;
            const visitas = this.dataset.visitas;
            const servicioId = this.dataset.servicio;
            const productoId = this.dataset.producto;
            const categoriaId = this.dataset.categoria;

            // Poblar campos base de la promoción en el formulario
            document.getElementById("promoId").value = id;
            document.getElementById("promoNombre").value = nombre;
            document.getElementById("promoDescripcion").value = description;
            document.getElementById("promoPorcentaje").value = descuento;
            document.getElementById("promoVisitas").value = visitas;

            // Asignar fechas recortando los segundos
            if (inicio && inputInicio) inputInicio.value = inicio.slice(0, 16);
            if (fin && inputFin) inputFin.value = fin.slice(0, 16);

            // Cambiar el tipo de promoción y alternar los contenedores
            const selectTipo = document.getElementById("promoTipo");
            selectTipo.value = tipo;
            alternarCamposFlujo();

            // Mapear relaciones específicas según el flujo
            if (tipo === "PRODUCTO") {
                const selectAlcance = document.getElementById("filtroAlcanceProducto");
                if (categoriaId) {
                    selectAlcance.value = "CATEGORIA";
                    document.getElementById("promoCategoriaId").value = categoriaId;
                } else {
                    selectAlcance.value = "ESPECIFICO";

                    // Cargamos el texto estético en el input del modal
                    const itemCorrespondiente = document.querySelector(`.item-prod-modal[data-id="${productoId}"]`);
                    if (itemCorrespondiente && inputHiddenId && inputTextoVisible) {
                        inputHiddenId.value = productoId;
                        inputTextoVisible.value = `${itemCorrespondiente.dataset.nombre} (S/ ${itemCorrespondiente.dataset.precio}) - [Stock: ${itemCorrespondiente.dataset.stock}]`;
                    }
                }
                alternarAlcanceProducto();
            } else if (tipo === "SERVICIO") {
                document.getElementById("promoServicioId").value = servicioId;
            }

            recalcularPrecioFinalVistaAdmin();
        });
    });
});

// ── 5. FUNCIONES DE FLUJO ORIGINALES INTEGRADAS (Fuera del DOMContentLoaded) ──
function alternarCamposFlujo() {
    const tipo = document.getElementById("promoTipo").value;
    const wrapperServicio = document.getElementById("wrapperServicio");
    const wrapperProducto = document.getElementById("wrapperProducto");
    const wrapperVisitas  = document.getElementById("wrapperVisitas");
    const inputVisitas    = document.getElementById("promoVisitas");

    if (tipo === "SERVICIO") {
        if (wrapperServicio) wrapperServicio.classList.remove("d-none");
        if (wrapperProducto) wrapperProducto.classList.add("d-none");
        if (wrapperVisitas)  wrapperVisitas.classList.remove("d-none");
        document.getElementById("promoProductoId").value = "";
        document.getElementById("promoProductoTexto").value = "";
        document.getElementById("promoCategoriaId").value = "";
    } else {
        if (wrapperServicio) wrapperServicio.classList.add("d-none");
        if (wrapperProducto) wrapperProducto.classList.remove("d-none");
        if (wrapperVisitas)  wrapperVisitas.classList.add("d-none");
        if (inputVisitas)    inputVisitas.value = "0";
        document.getElementById("promoServicioId").value = "";
        alternarAlcanceProducto();
    }

    recalcularPrecioFinalVistaAdmin();
}

function alternarAlcanceProducto() {
    const alcance = document.getElementById("filtroAlcanceProducto").value;
    const wrapperUnico = document.getElementById("subWrapperProductoUnico");
    const wrapperCat = document.getElementById("subWrapperCategoria");

    if (alcance === "ESPECIFICO") {
        if (wrapperUnico) wrapperUnico.classList.remove("d-none");
        if (wrapperCat) wrapperCat.classList.add("d-none");
        document.getElementById("promoCategoriaId").value = "";
    } else {
        if (wrapperUnico) wrapperUnico.classList.add("d-none");
        if (wrapperCat) wrapperCat.classList.remove("d-none");
        document.getElementById("promoProductoId").value = "";
        document.getElementById("promoProductoTexto").value = "";
    }
}

function limpiarFormulario() {
    document.getElementById("promoForm").reset();
    document.getElementById("promoId").value = "";
    document.getElementById("promoProductoId").value = "";
    document.getElementById("promoProductoTexto").value = "";
    document.getElementById("formTitle").textContent = "Nueva Promoción";
    const err = document.getElementById("errorDescuentoHTML");
    if (err) err.style.display = "none";
    document.getElementById("promoPorcentaje").classList.remove("is-invalid");
    alternarCamposFlujo();
    recalcularPrecioFinalVistaAdmin();
}
function abrirModalEliminar(urlUrl) {
    document.getElementById('btnConfirmarEliminarHref').setAttribute('href', urlUrl);
    var myModal = new bootstrap.Modal(document.getElementById('modalConfirmarEliminar'));
    myModal.show();
}
function validarPorcentajeEntrada(input) {
    // 1. Eliminamos cualquier signo negativo o caracteres no numéricos por si pegan texto
    input.value = input.value.replace(/[^0-9]/g, '');

    const errorDiv = document.getElementById('errorDescuentoHTML');
    const valor = parseInt(input.value, 10);

    // 2. Si el usuario escribe un monto superior a 80, lo frenamos en seco
    if (valor > 80) {
        input.value = 80;
        if (errorDiv) errorDiv.style.display = 'block';
    } else {
        if (errorDiv) errorDiv.style.display = 'none';
    }

    // 3. Evitamos que se quede en vacío o en 0 al perder el foco si deseas un mínimo de 1
    if (input.value !== '' && valor < 1) {
        input.value = 1;
    }

    recalcularPrecioFinalVistaAdmin();
}

function recalcularPrecioFinalVistaAdmin() {
    const tipo = document.getElementById("promoTipo") ? document.getElementById("promoTipo").value : "SERVICIO";
    const porcentajeInput = document.getElementById("promoPorcentaje");
    const porcentaje = porcentajeInput && porcentajeInput.value ? parseFloat(porcentajeInput.value) : 0;
    const inputPrecioFinal = document.getElementById("promoPrecioFinalCalculado");
    const hintPrecioOriginal = document.getElementById("promoHintPrecioOriginal");

    if (!inputPrecioFinal) return;

    let precioOriginal = 0;

    if (tipo === "SERVICIO") {
        const selectServicio = document.getElementById("promoServicioId");
        if (selectServicio && selectServicio.selectedIndex > 0) {
            const opt = selectServicio.options[selectServicio.selectedIndex];
            precioOriginal = parseFloat(opt.dataset.precio || 0);
        }
    } else if (tipo === "PRODUCTO") {
        const prodId = document.getElementById("promoProductoId") ? document.getElementById("promoProductoId").value : "";
        if (prodId) {
            const item = document.querySelector(`.item-prod-modal[data-id="${prodId}"]`);
            if (item) {
                precioOriginal = parseFloat(item.dataset.precio || 0);
            }
        }
    }

    if (precioOriginal > 0 && porcentaje > 0) {
        const desc = precioOriginal * (porcentaje / 100.0);
        const finalCalculado = Math.max(0, precioOriginal - desc);
        inputPrecioFinal.value = finalCalculado.toFixed(2);
        if (hintPrecioOriginal) {
            hintPrecioOriginal.textContent = `Precio original: S/ ${precioOriginal.toFixed(2)} (Ahorras: S/ ${desc.toFixed(2)})`;
        }
    } else {
        inputPrecioFinal.value = "";
        if (hintPrecioOriginal) hintPrecioOriginal.textContent = "";
    }
}