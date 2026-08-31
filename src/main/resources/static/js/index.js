// ── Carrito ───────────────────────────────────────────────
const CSRF_TOKEN = document.querySelector('meta[name="_csrf"]')?.content || '';
const ES_CLIENTE = /*[[${#authorization.expression('hasRole(''CLIENTE'')')}]]*/ false;

(function () {
    var slides = document.querySelectorAll('.slide-bg-item');
    var dots = document.querySelectorAll('.slider-dot');
    var titulo = document.getElementById('heroTitulo');
    var desc = document.getElementById('heroDesc');
    var DEFAULT_TITULO = 'El Arte del Buen Corte';
    var DEFAULT_DESC = 'Reserva tu cita con los mejores barberos de la ciudad. Experiencia, precisión y estilo en cada visita.';
    if (!slides.length) return;
    var current = 0, timer = null, total = slides.length;
    function activar(idx) {
        slides[current].classList.remove('active');
        if (dots[current]) dots[current].classList.remove('active');
        current = ((idx % total) + total) % total;
        slides[current].classList.add('active');
        if (dots[current]) dots[current].classList.add('active');
        var t = slides[current].dataset.titulo;
        var d = slides[current].dataset.desc;
        if (titulo) titulo.textContent = (t && t.trim() !== '') ? t : DEFAULT_TITULO;
        if (desc) desc.textContent = (d && d.trim() !== '') ? d : DEFAULT_DESC;
    }
    function resetTimer() {
        clearInterval(timer);
        if (total > 1) timer = setInterval(function () { activar(current + 1); }, 5000);
    }
    window.sliderGoTo = function (idx) { activar(idx); resetTimer(); };
    window.sliderNext = function () { activar(current + 1); resetTimer(); };
    window.sliderPrev = function () { activar(current - 1); resetTimer(); };
    resetTimer();
    var heroEl = document.querySelector('.hero-slider');
    if (heroEl) {
        heroEl.addEventListener('mouseenter', function () { clearInterval(timer); });
        heroEl.addEventListener('mouseleave', resetTimer);
    }
})();

// Navbar scroll effect
window.addEventListener('scroll', function () {
    var nb = document.querySelector('.nb');
    if (window.scrollY > 50) {
        nb.style.background = 'rgba(8,8,8,0.97)';
        nb.style.boxShadow = '0 4px 30px rgba(0,0,0,0.4)';
    } else {
        nb.style.background = 'rgba(10,10,10,0.82)';
        nb.style.boxShadow = 'none';
    }
});


function mostrarModalAuth() {
    new bootstrap.Modal(document.getElementById('modalAuthCarrito')).show();
}

function irAlCarrito() {
    const carrito = getCarrito();
    if (carrito.length === 0) {
        mostrarToast('⚠ Tu carrito está vacío', '#e74c3c');
        return;
    }
    abrirCarrito();
}

async function agregarAlCarrito(productoId, stock) {
    // ELIMINAMOS la validación de "if (!ES_CLIENTE) { mostrarModalAuth(); return; }" 
    // que te estaba bloqueando e invocando el modal antes de tiempo.

    const btn = event.currentTarget;
    const card = btn.closest('.card-producto-premium');
    const nombre = card.querySelector('.prod-titulo')?.textContent?.trim() || '';
    const precio = parseFloat(
        card.querySelector('.prod-precio')?.textContent?.replace('S/ ', '') || 0);
    const imagen = card.querySelector('img')?.src || '';

    const carrito = getCarrito();
    const existe = carrito.findIndex(i => i.id == productoId);

    if (existe >= 0) {
        const nueva = carrito[existe].cantidad + 1;
        if (nueva > stock) {
            mostrarToast('⚠ Solo hay ' + stock + ' unidades disponibles', '#e74c3c');
            return;
        }
        carrito[existe].cantidad = nueva;
    } else {
        carrito.push({ id: productoId, nombre, precio, imagen, cantidad: 1, stock });
    }

    saveCarrito(carrito);
    renderCarrito();
    abrirCarrito(); // Esto abrirá el Drawer lateral a todos los usuarios sin importar el rol
    mostrarToast('✓ ' + nombre + ' agregado', '#c9a84c');
}

function actualizarContadorCarrito(total) {
    const badge = document.getElementById('carritoCount');
    if (!badge) return;
    if (total > 0) {
        badge.textContent = total;
        badge.style.display = 'flex';
    } else {
        badge.style.display = 'none';
    }
}


// Cargar contador y renderizar carrito al iniciar
document.addEventListener('DOMContentLoaded', async () => {
    renderCarrito();
    if (!ES_CLIENTE) return;
    try {
        const res = await fetch('/api/carrito/count');
        const data = await res.json();
        if (data && typeof data.totalItems === 'number' && data.totalItems > 0) {
            actualizarContadorNavbar(data.totalItems);
        }
    } catch { }
});


function getCarrito() {
    try { return JSON.parse(localStorage.getItem('carrito_barberia') || '[]'); }
    catch { return []; }
}
function saveCarrito(carrito) {
    localStorage.setItem('carrito_barberia', JSON.stringify(carrito));
    sincronizarConServidor(carrito);
}

// Sincroniza con la sesión del servidor
async function sincronizarConServidor(carrito) {
    try {
        await fetch('/api/carrito/sincronizar', {
            method: 'POST',
            headers: {
                'Content-Type': 'application/json',
                'X-CSRF-TOKEN': CSRF_TOKEN
            },
            body: JSON.stringify(carrito)
        });
    } catch { }
}

function abrirCarrito() {
    document.getElementById('carritoDrawer').classList.add('open');
    document.getElementById('carritoOverlay').classList.add('open');
    document.body.style.overflow = 'hidden';
}
function cerrarCarrito() {
    document.getElementById('carritoDrawer').classList.remove('open');
    document.getElementById('carritoOverlay').classList.remove('open');
    document.body.style.overflow = '';
}

function renderCarrito() {
    const carrito = getCarrito();
    const body = document.getElementById('carritoDrawerBody');
    const vacio = document.getElementById('carritoVacio');
    const total = carrito.reduce((s, i) => s + i.precio * i.cantidad, 0);
    const count = carrito.reduce((s, i) => s + i.cantidad, 0);

    document.getElementById('drawerTotal').textContent =
        'S/ ' + total.toFixed(2);
    document.getElementById('drawerCount').textContent = count;
    actualizarContadorNavbar(count);

    if (carrito.length === 0) {
        body.innerHTML = '';
        body.appendChild(document.getElementById('carritoVacio') ||
            crearVacio());
        vacio && (vacio.style.display = 'flex');
        return;
    }

    body.innerHTML = carrito.map((item, idx) => `
        <div class="carrito-item">
            <img class="carrito-item-img"
                 src="${item.imagen || 'https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'}"
                 alt="${item.nombre}"
                 onerror="this.src='https://thebarbercompany.pe/wp-content/uploads/2019/04/serv4.webp'">
            <div class="carrito-item-info">
                <div class="carrito-item-nombre" title="${item.nombre}">${item.nombre}</div>
                <div class="carrito-item-precio">S/ ${item.precio.toFixed(2)} c/u</div>
                <div class="carrito-item-controles">
                    <button class="ctrl-btn" onclick="cambiarCantidad(${idx}, -1)">
                        <i class="fa-solid fa-minus"></i>
                    </button>
                    <span class="ctrl-cantidad">${item.cantidad}</span>
                    <button class="ctrl-btn" onclick="cambiarCantidad(${idx}, 1)">
                        <i class="fa-solid fa-plus"></i>
                    </button>
                    <button class="ctrl-btn-del" onclick="eliminarItem(${idx})"
                            title="Eliminar">
                        <i class="fa-solid fa-trash"></i>
                    </button>
                </div>
            </div>
            <div class="carrito-item-subtotal">
                S/ ${(item.precio * item.cantidad).toFixed(2)}
            </div>
        </div>
    `).join('');
}

function cambiarCantidad(idx, delta) {
    const carrito = getCarrito();
    const item = carrito[idx];
    if (!item) return;

    const nueva = item.cantidad + delta;
    if (nueva <= 0) {
        eliminarItem(idx); return;
    }
    if (nueva > item.stock) {
        mostrarToast('⚠ Solo hay ' + item.stock + ' unidades disponibles', '#e74c3c');
        return;
    }
    carrito[idx].cantidad = nueva;
    saveCarrito(carrito);
    renderCarrito();
}

function eliminarItem(idx) {
    const carrito = getCarrito();
    carrito.splice(idx, 1);
    saveCarrito(carrito);
    renderCarrito();
}

function actualizarContadorNavbar(count) {
    const badge = document.getElementById('carritoCount');
    const badgeAnon = document.getElementById('carritoCountAnon');
    if (badge) {
        badge.textContent = count;
        badge.style.display = count > 0 ? 'flex' : 'none';
    }
    if (badgeAnon) {
        badgeAnon.textContent = count;
        badgeAnon.style.display = count > 0 ? 'flex' : 'none';
    }
}

async function agregarAlCarrito(productoId, stock) {
    // Buscar datos del producto desde la página
    const btn = event.currentTarget;
    const card = btn.closest('.card-producto-premium');
    const nombre = card.querySelector('.prod-titulo')?.textContent?.trim() || '';
    const precio = parseFloat(
        card.querySelector('.prod-precio')?.textContent?.replace('S/ ', '') || 0);
    const imagen = card.querySelector('img')?.src || '';

    const carrito = getCarrito();
    const existe = carrito.findIndex(i => i.id == productoId);

    if (existe >= 0) {
        const nueva = carrito[existe].cantidad + 1;
        if (nueva > stock) {
            mostrarToast('⚠ Solo hay ' + stock + ' unidades disponibles', '#e74c3c');
            return;
        }
        carrito[existe].cantidad = nueva;
    } else {
        carrito.push({ id: productoId, nombre, precio, imagen, cantidad: 1, stock });
    }

    saveCarrito(carrito);
    renderCarrito();
    abrirCarrito();
    mostrarToast('✓ ' + nombre + ' agregado', '#c9a84c');
}

function irAPagar() {
    const carrito = getCarrito();

    if (carrito.length === 0) {
        mostrarToast('⚠ Tu carrito está vacío', '#e74c3c');
        return; // No redirige, solo muestra el mensaje
    }

    fetch('/api/auth/check')
        .then(r => r.json())
        .then(data => {
            if (data.logueado) {
                window.location.href = '/cliente/carrito/pago';
            } else {
                cerrarCarrito();
                setTimeout(() => {
                    new bootstrap.Modal(
                        document.getElementById('modalAuthCarrito')).show();
                }, 300);
            }
        })
        .catch(() => {
            window.location.href = '/cliente/carrito/pago';
        });
}
function mostrarToast(mensaje, color = '#c9a84c') {
    const toast = document.getElementById('toastCarrito');
    const span = document.getElementById('toastMensaje');
    if (!toast || !span) return;
    span.textContent = mensaje;
    toast.style.borderColor = color;
    toast.style.transform = 'translateX(-50%) translateY(0)';
    setTimeout(() => {
        toast.style.transform = 'translateX(-50%) translateY(100px)';
    }, 3000);
}

// ── Filtro subcategoría ───────────────────────────────────
function filtrarSubcategoria(nombre, event) {
    document.querySelectorAll('.item-producto-tarjeta').forEach(item => {
        const itemSubcat = item.dataset.subcategoria || '';
        item.style.display =
            (nombre === 'TODOS' || itemSubcat.trim().toLowerCase() === nombre.trim().toLowerCase()) ? '' : 'none';
    });
    document.querySelectorAll('.filter-btn').forEach(btn => {
        btn.classList.remove('active');
    });
    if (event?.target) event.target.classList.add('active');
}

function toggleMobileMenu() {
    const actionsMenu = document.getElementById('navbarActions');
    const hamburgerIcon = document.getElementById('hamburgerBtn').querySelector('i');

    // Alterna la clase para mostrar/ocultar el menú
    actionsMenu.classList.toggle('show-menu');

    // Cambia el ícono estéticamente de barras (☰) a una equis (✕) al abrirse
    if (actionsMenu.classList.contains('show-menu')) {
        hamburgerIcon.classList.remove('fa-bars');
        hamburgerIcon.classList.add('fa-xmark');
    } else {
        hamburgerIcon.classList.remove('fa-xmark');
        hamburgerIcon.classList.add('fa-bars');
    }
}

// Cierra el menú automáticamente si el usuario cambia el tamaño de la ventana
window.addEventListener('resize', () => {
    if (window.innerWidth > 991) {
        const actionsMenu = document.getElementById('navbarActions');
        const hamburgerIcon = document.getElementById('hamburgerBtn').querySelector('i');
        actionsMenu.classList.remove('show-menu');
        if (hamburgerIcon) {
            hamburgerIcon.classList.remove('fa-xmark');
            hamburgerIcon.classList.add('fa-bars');
        }
    }
});