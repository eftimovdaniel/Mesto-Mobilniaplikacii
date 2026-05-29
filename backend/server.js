/**
 * mesto REST API: Express + Postgres (Supabase).
 * Start: npm install && npm start (чита .env од backend директориум).
 * Env:
 *   PORT (по default 3000)
 *   DATABASE_URL = postgresql://postgres.<ref>:<password>@<host>:6543/postgres?sslmode=require
 */
require("dotenv").config();
const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");

const PORT = Number(process.env.PORT || 3000);
const DATABASE_URL = process.env.DATABASE_URL;

if (!DATABASE_URL) {
  console.error("Грешка: треба DATABASE_URL во .env (Supabase → Connect → URI).");
  process.exit(1);
}

// Supabase pooler враќа self-signed cert; pg по default verify-а кога sslmode е во URL-то.
// Затоа го отстрануваме sslmode од connection string и SSL го контролираме преку config.
function buildDbConfig(rawUrl) {
  try {
    const url = new URL(rawUrl);
    url.searchParams.delete("sslmode");
    return {
      connectionString: url.toString(),
      ssl: { rejectUnauthorized: false },
      max: 5,
    };
  } catch {
    return {
      connectionString: rawUrl,
      ssl: { rejectUnauthorized: false },
      max: 5,
    };
  }
}

const pool = new Pool(buildDbConfig(DATABASE_URL));

function normalizeRow(r) {
  return {
    id: Number(r.id),
    name: r.name,
    address: r.address,
    latitude: Number(r.latitude),
    longitude: Number(r.longitude),
    email: r.email,
    phone: r.phone,
    website: r.website,
    image_url: r.image_url || null,
    categories: Array.isArray(r.categories) ? r.categories.map(String) : [],
  };
}

(async () => {
  try {
    const { rows } = await pool.query("SELECT current_database() as db, current_user as usr");
    console.log("Postgres OK: db=" + rows[0].db + " user=" + rows[0].usr);
  } catch (e) {
    console.error("Postgres FAILED:", e.code, e.message);
  }
})();

const app = express();
app.use(cors());
app.use(express.json());

app.get("/", (_req, res) => {
  res.json({ ok: true, service: "mesto-api", endpoints: ["/health", "/companies"] });
});

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

// GET /companies?category=service&q=name
app.get("/companies", async (req, res) => {
  try {
    const category = req.query.category;
    const q = req.query.q;

    const where = [];
    const params = [];

    if (category) {
      params.push(JSON.stringify([String(category)]));
      where.push(`categories @> $${params.length}::jsonb`);
    }
    if (q) {
      params.push(`%${String(q)}%`);
      where.push(`name ILIKE $${params.length}`);
    }

    const sql =
      "SELECT id, name, address, latitude, longitude, email, phone, website, image_url, categories " +
      "FROM companies" +
      (where.length ? ` WHERE ${where.join(" AND ")}` : "") +
      " ORDER BY id DESC";

    const { rows } = await pool.query(sql, params);
    res.json(rows.map(normalizeRow));
  } catch (e) {
    console.error("GET /companies failed:", e.code, e.message);
    res.status(500).json({ error: "database_error", code: e.code, detail: e.message });
  }
});

app.post("/companies", async (req, res) => {
  const b = req.body || {};
  const required = [
    "name",
    "address",
    "latitude",
    "longitude",
    "email",
    "phone",
    "website",
  ];
  for (const k of required) {
    if (b[k] === undefined || b[k] === null || b[k] === "") {
      return res.status(400).json({ error: "missing_" + k });
    }
  }

  const cats = b.categories;
  if (!Array.isArray(cats) || cats.length === 0) {
    return res.status(400).json({ error: "missing_categories" });
  }

  try {
    const sql =
      "INSERT INTO companies " +
      "(name, address, latitude, longitude, email, phone, website, image_url, categories) " +
      "VALUES ($1, $2, $3, $4, $5, $6, $7, $8, $9::jsonb) " +
      "RETURNING id, name, address, latitude, longitude, email, phone, website, image_url, categories";
    const params = [
      String(b.name),
      String(b.address),
      Number(b.latitude),
      Number(b.longitude),
      String(b.email),
      String(b.phone),
      String(b.website),
      b.image_url ? String(b.image_url) : null,
      JSON.stringify(cats.map(String)),
    ];
    const { rows } = await pool.query(sql, params);
    res.status(201).json(normalizeRow(rows[0]));
  } catch (e) {
    console.error("POST /companies failed:", e.code, e.message);
    res.status(500).json({ error: "database_error", code: e.code, detail: e.message });
  }
});

app.listen(PORT, () => {
  console.log(`mesto API слуша на http://localhost:${PORT}`);
});
