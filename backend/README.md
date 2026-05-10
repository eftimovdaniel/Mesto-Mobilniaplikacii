# mesto API (Node.js + MySQL)

## 1. MySQL

Инсталирај MySQL локално, па изврши:

```bash
mysql -u root -p < schema.sql
```

Креирај корисник/лозинка ако треба и уреди го `backend/.env` (копирај од `.env.example`).

## 2. API

```bash
cd backend
npm install
cp .env.example .env
# уреди .env — DB_USER, DB_PASSWORD, DB_NAME
npm start
```

Проверка: во прелистувач отвори `http://localhost:3000/health` — треба `{"ok":true}`.

## 3. Android

- **Емулатор:** во `app/build.gradle.kts` остави `API_BASE_URL = http://10.0.2.2:3000/` (тоа е `localhost` на компјутерот).
- **Телефон на Wi‑Fi:** најди ја LAN IP на Mac (`System Settings → Network`), па смени ја во `buildConfigField`, на пр. `http://192.168.0.15:3000/` — истата мрежа како телефонот.

Телефонот **не користи MySQL директно** — само HTTP кон овој сервер.

## 4. Без рачно `npm start` во Terminal

**MySQL (Homebrew):** еднаш `brew services start mysql` — сервисот останува вклучен и по рестарт.

**API — препорака (PM2):**

```bash
npm install -g pm2
cd backend
pm2 start server.js --name mesto-api
pm2 save
pm2 startup
```

Изврши ја линијата што `pm2 startup` ќе ја покаже (еднаш).

**Алтернатива:** двојно клик на **`start-mesto-api.command`** во оваа папка (се отвора Terminal и пушта `npm start`). Прво: десен клик → Отвори ако macOS блокира непознат фајл.
