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

const pool = new Pool({
  connectionString: DATABASE_URL,
  ssl: { rejectUnauthorized: false },
  max: 5,
});

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
    categories: Array.isArray(r.categories) ? r.categories.map(String) : [],
  };
}

(async () => {
  try {
    await pool.query("SELECT 1");
    console.log("Postgres: поврзан со Supabase.");
  } catch (e) {
    console.error("Postgres: НЕМА врска. Провери DATABASE_URL.\n", e.message);
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
      "SELECT id, name, address, latitude, longitude, email, phone, website, categories " +
      "FROM companies" +
      (where.length ? ` WHERE ${where.join(" AND ")}` : "") +
      " ORDER BY id DESC";

    const { rows } = await pool.query(sql, params);
    res.json(rows.map(normalizeRow));
  } catch (e) {
    console.error("GET /companies", e);
    res.status(500).json({ error: "database_error" });
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
      "(name, address, latitude, longitude, email, phone, website, categories) " +
      "VALUES ($1, $2, $3, $4, $5, $6, $7, $8::jsonb) " +
      "RETURNING id, name, address, latitude, longitude, email, phone, website, categories";
    const params = [
      String(b.name),
      String(b.address),
      Number(b.latitude),
      Number(b.longitude),
      String(b.email),
      String(b.phone),
      String(b.website),
      JSON.stringify(cats.map(String)),
    ];
    const { rows } = await pool.query(sql, params);
    res.status(201).json(normalizeRow(rows[0]));
  } catch (e) {
    console.error("POST /companies", e);
    res.status(500).json({ error: "database_error" });
  }
});

app.listen(PORT, () => {
  console.log(`mesto API слуша на http://localhost:${PORT}`);
});
