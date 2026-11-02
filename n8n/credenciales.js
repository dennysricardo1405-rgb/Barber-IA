// U4 - Genera las credenciales de n8n a partir de las variables del .env, para no cargarlas a mano.
// Se ejecuta al arrancar el contenedor; el archivo resultante se borra apenas se importa.
const credenciales = [];

if (process.env.TELEGRAM_BOT_TOKEN) {
  credenciales.push({
    id: 'telegramBarberia',
    name: 'Telegram - bot de citas',
    type: 'telegramApi',
    data: { accessToken: process.env.TELEGRAM_BOT_TOKEN },
  });
}

if (process.env.GEMINI_API_KEY) {
  credenciales.push({
    id: 'geminiBarberia',
    name: 'Gemini - clave del asesor IA',
    type: 'googlePalmApi',
    data: {
      host: process.env.GEMINI_API_HOST || 'https://generativelanguage.googleapis.com',
      apiKey: process.env.GEMINI_API_KEY,
    },
  });
}

process.stdout.write(JSON.stringify(credenciales));
