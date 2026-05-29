# mesto API

Мал REST сервис (Express + `pg`) што Android апликацијата го користи за листање и додавање компании. Базата е **Supabase Postgres**.

## Локално

```bash
cd backend
cp .env.example .env
# постави DATABASE_URL од Supabase → Project → Connect → URI (Transaction pooler)
npm install
npm start
```

Тест:

```bash
curl http://localhost:3000/health
curl http://localhost:3000/companies
```

## Endpoints

| Метод | Пат                              | Опис                              |
|-------|----------------------------------|-----------------------------------|
| GET   | `/health`                        | Healthcheck (за Render)           |
| GET   | `/companies`                     | Сите компании                     |
| GET   | `/companies?category=service`    | Филтер по категорија              |
| GET   | `/companies?q=име`               | Пребарување по име (ILIKE)        |
| POST  | `/companies`                     | Додавање нова (JSON body)         |

## Деплојмент на Render

1. Push на овој repo во GitHub.
2. На [render.com](https://render.com) → **New +** → **Blueprint** → избери го репото.
3. Render го наоѓа `backend/render.yaml` и креира `mesto-api` сервис.
4. Кога ќе те праша за `DATABASE_URL`, залепи го URI-то од Supabase.
5. По 2–3 минути добиваш јавен URL: `https://mesto-api-xxxx.onrender.com`.
6. Тоа URL ставаш во Android `local.properties`:
   ```
   backend.url=https://mesto-api-xxxx.onrender.com
   ```

> Free Render планот „заспива" по 15 мин неактивност — првиот повик потоа трае ~30 сек.
