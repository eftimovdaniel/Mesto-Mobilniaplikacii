# Место

Ова е моја Android апликација за локален бизнис директориум — нешто како мини „жолти страници" за компании. Може да се пребарува, да се додаваат нови компании, да се отвораат детали (повик, email, веб, мапа) и праќа известување кога ке поминеш блиску до некој зачуван бизнис.

Проектот е направен за предметот **Мобилни Апликации**.

---

## Структура на проектот (код)

| Дел | Папка / име | Што е |
|-----|-------------|--------|
| Android апликација | `app/` | Клиентот што го гледа корисникот |
| Backend API | `backend/` | Мојот REST сервис `mesto-api` |
| Шема на база | `supabase/schema.sql` | SQL за Supabase табелата `companies` |
| Деплој на Render | `render.yaml` (во root и во `backend/`) | Blueprint за сервисот `mesto-api` |

**Android**
- Име на апликацијата: **Место**
- Gradle проект: `Mesto_samostojna`
- Package: `com.example.mesto_samostojna`
- Главни класи: `MainActivity`, `AddCompanyActivity`, `CompanyDetailActivity`, `api/MestoApi` (Retrofit), `data/Company`, `geofence/` за известувања
- Повикување на API: URL од `local.properties` → `backend.url` (не оди на Git)

**Backend (`mesto-api`)**
- Фајл: `backend/server.js`
- Stack: **Node.js** + **Express** + **`pg`** (директно кон Postgres)
- `npm start` → слуша на порт 3000 (локално) или `PORT` на Render

---

## База на податоци

- **Платформа:** [Supabase](https://supabase.com) — managed **PostgreSQL**
- **Табела:** `public.companies` (име, адреса, lat/lng, телефон, email, веб, слика, категории како `jsonb`)
- **Шема и RLS:** ја имам во `supabase/schema.sql` (политики за читање и внесување)
- **Поврзување:** backend-от користи `DATABASE_URL` од `.env` (локално) или од Render env (продукција) — URI од Supabase → Project → Connect → **Transaction pooler**

Апликацијата **не** се поврзува директно со Supabase; сите податоци одат преку мојот API.

---

## Мој сервис (каде е направен и како работи)

| | |
|--|--|
| **Име на сервисот** | `mesto-api` |
| **Хостинг** | [Render](https://render.com) — free web service |
| **URL (пример)** | `https://mesto-api-xxxx.onrender.com` |
| **Како е деплојнат** | Push на GitHub → Render Blueprint од `render.yaml` → `npm install` + `npm start` во `backend/` |
| **Env на Render** | `DATABASE_URL` = Supabase connection string |

**Тек на податоци:**

```
Android (Retrofit)  →  mesto-api (Render, Express)  →  Supabase (Postgres)
```

1. Апликацијата праќа HTTP кон `backend.url` (во `local.properties`).
2. `mesto-api` ги обработува барањата (`GET/POST /companies`) и преку `pg` чита/пишува во Supabase.
3. Резултатот се враќа како JSON кон Android.

---

## Што може да прави корисникот

**Главен екран (преглед на компании)**
- Гледа листа на сите внесени компании, повлечени директно од базата преку API-то.
- Поминува низ пет табови по категорија: **Сервис**, **Забава**, **Индустрија**, **Едукација**, **Друго**.
- Пребарува по име во горниот search bar — резултатите се филтрираат на лету (case-insensitive).
- Со тап на компанија се отвора нејзината детална страница.
- Од копчето во toolbar-от отвора форма за додавање нова компанија.
- На секој ред има мала **канта за бришење**. Тап на неа отвора AlertDialog „Дали сте сигурни дека сакате да ја избришете „X"? Оваа акција не може да се врати." со копчиња **Избриши** / **Откажи**. По потврда оди `DELETE /companies/:id` кон backend-от, записот се брише од Supabase, листата автоматски се освежува и геофенс-от за таа компанија се отстранува.

**Додавање нова компанија**
- Внесува име, адреса, телефон, email (опц.), веб страна (опц.), URL до лого (опц.) и координати.
- Може со едно копче **„Најди локација од адреса"** автоматски да ги добие `latitude` / `longitude` од внесениот текст (преку Android Geocoder; ако не препознае земја, се додава „North Macedonia" за поточен резултат).
- Избира една или повеќе категории преку чекбокс.
- Под полињата гледа жив preview на координатите во N/S · E/W формат.
- Имам валидација на сите задолжителни полиња; при грешка покажува порака до полето.
- На **Зачувај** оди `POST /companies` кон мојот backend, кој ги внесува во Supabase. По успех листата автоматски се освежува.

**Детали на компанија**
- Гледа лого (или favicon / og:image како fallback ако нема URL), име, адреса, координати, телефон, email, веб страна и чипови со категории.
- **Тап на адреса** — отвора локацијата во инсталираната мап апликација (Google Maps fallback преку линк ако нема друга).
- **Тап на телефон** — отвора `dialer` со пополнет број.
- **Тап на email** — отвора email апликација со пополнет адресат.
- **Тап на веб страна** — отвора во browser (автоматски додава `https://` ако недостасува).

**Локациски функции (background)**
- Кога апликацијата е отворена и корисникот ќе се приближи под **50 m** до некој зачуван бизнис, добива **Toast** известување (со 60 s cooldown да не спам-а).
- Кога апликацијата е во background, истото се случува преку **геофенс нотификација** (Google Play Services + `GeofenceBroadcastReceiver`) — допирот на нотификацијата го носи право во деталите за таа компанија.
- Дозволите за локација и нотификации се бараат само еднаш, само ако се потребни.

## API endpoints (`mesto-api`)

| Метод | Пат | Што прави |
|-------|-----|-----------|
| GET | `/health` | Дали серверот работи (и за Render health check) |
| GET | `/companies` | Сите компании |
| GET | `/companies?category=service` | Филтер по категорија |
| GET | `/companies?q=име` | Пребарување по име |
| POST | `/companies` | Нова компанија (JSON body) |
| DELETE | `/companies/:id` | Бришење на компанија по `id` |

Повеќе за деплој: [`backend/README.md`](backend/README.md).

---

## Локално пуштање

1. `local.properties` (од `local.properties.example`):
   - `sdk.dir` — Android SDK
   - `backend.url` — `http://10.0.2.2:3000` (емулатор) или Render URL
2. Backend:
   ```bash
   cd backend
   cp .env.example .env
   # во .env: DATABASE_URL=<Supabase URI>
   npm install
   npm start
   ```
3. Android Studio → Run на емулатор или телефон.

`local.properties`, `backend/.env` и лозинките за Supabase **не** се комитираат на Git.
