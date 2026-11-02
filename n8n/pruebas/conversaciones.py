"""
U4 - Conversaciones de prueba contra el agente de citas (de punta a punta: n8n + Gemini + backend).

Usa el webhook de prueba del flujo, así que no necesita Telegram. Cada escenario es una conversación
nueva con un DNI nuevo. Al final muestra los indicadores que calcula el backend.

Uso (con docker compose --profile bot levantado):
    python n8n/pruebas/conversaciones.py --clave TU_BOT_API_KEY --rondas 2
"""
import argparse
import datetime as dt
import json
import random
import time
import urllib.request

DIAS = ["LUNES", "MARTES", "MIERCOLES", "JUEVES", "VIERNES", "SABADO", "DOMINGO"]


def llamar(url, clave, datos=None):
    cuerpo = None if datos is None else json.dumps(datos).encode()
    req = urllib.request.Request(url, data=cuerpo, method="POST" if datos else "GET",
                                 headers={"content-type": "application/json", "x-bot-key": clave,
                                          "X-BOT-KEY": clave})
    with urllib.request.urlopen(req, timeout=120) as r:
        return json.loads(r.read().decode())


def escenarios(barbero, servicio, fecha, hora_libre, hora_ocupada):
    dia = fecha.strftime("%d/%m")
    def dni():
        return str(random.randint(40000000, 79999999))
    return {
        "cita exitosa": [
            "Hola, quiero reservar un corte", f"Mi DNI es {dni()}",
            "Me llamo Prueba Automática, mi celular es 9" + "".join(random.choices("0123456789", k=8)),
            f"Quiero {servicio} con {barbero} el {dia}", f"A las {hora_libre}", "Sí, confirmo"],
        "DNI inválido": ["Hola, quiero una cita", "Mi DNI es 1234", f"Perdón, es {dni()}"],
        "fecha pasada": ["Buenas", f"Mi DNI es {dni()}", "Me llamo Prueba Pasado, celular 987654321",
                         f"Quiero {servicio} con {barbero} para ayer a las 11"],
        "fuera de horario": ["Hola", f"Mi DNI es {dni()}", "Me llamo Prueba Noche, celular 912345678",
                             f"Quiero {servicio} con {barbero} el {dia} a las 11 de la noche"],
        "horario ocupado": ["Hola", f"Mi DNI es {dni()}", "Me llamo Prueba Ocupado, celular 923456789",
                            f"Quiero {servicio} con {barbero} el {dia} a las {hora_ocupada}"],
        "abandono": ["Hola, ¿cuánto cuesta un corte?"],
    }


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--n8n", default="http://localhost:5678")
    ap.add_argument("--api", default="http://localhost:8080")
    ap.add_argument("--clave", required=True, help="El mismo BOT_API_KEY del .env")
    ap.add_argument("--rondas", type=int, default=1)
    args = ap.parse_args()

    catalogo = llamar(args.api + "/api/bot/catalogo", args.clave)
    fecha = dt.date.today() + dt.timedelta(days=1)
    barberos = [b for b in catalogo["barberos"] if b.get("diaLibre") != DIAS[fecha.weekday()]]
    servicio = catalogo["servicios"][0]["nombre"]

    for ronda in range(args.rondas):
        barbero = barberos[ronda % len(barberos)]
        hora_libre = f"{11 + ronda % 8}:00"
        for nombre, mensajes in escenarios(barbero["nombre"], servicio, fecha, hora_libre, hora_libre).items():
            conv = f"r{ronda}-{nombre.replace(' ', '-')}-{random.randint(1000, 9999)}"
            print(f"\n=== {nombre} ({conv}) ===")
            inicio = time.time()
            for texto in mensajes:
                print(f"  Cliente: {texto}")
                try:
                    r = llamar(args.n8n + "/webhook/bot-prueba", args.clave, {"conversacion": conv, "texto": texto})
                    print(f"  Bot:     {r.get('respuesta')}")
                except Exception as e:  # noqa: BLE001 - se informa y se sigue con el siguiente escenario
                    print(f"  ERROR:   {e}")
                    break
            print(f"  ({time.time() - inicio:.1f} s)")

    print("\n=== Indicadores (/api/bot/metricas) ===")
    print(json.dumps(llamar(args.api + "/api/bot/metricas", args.clave), indent=2, ensure_ascii=False))


if __name__ == "__main__":
    main()
