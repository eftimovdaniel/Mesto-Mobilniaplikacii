/**
 * mesto-api — REST backend za Android aplikacijata "Mesto".
 *
 * ZOSO E ODDELEN BACKEND?
 * Android-ot NE se povrzuva direktno so Supabase. Site podatoci odat
 * preku ovoj API. Taka: API klucot / DATABASE_URL ne e vo APK-to,
 * a logikata (validacija, SQL) e na edno mesto.
 *
 * KAKO RABOTI TEKOT:
 *   Android (Retrofit)  →  mesto-api (Express)  →  Supabase Postgres (tabela companies)
 *
 * Stack: Node.js + Express + pg
 * Start: npm install && npm start (od folderot backend/)
 *
 * Env (.env):
 *   PORT         — porta (default 3000; na Render doagja avtomatski)
 *   DATABASE_URL — connection string od Supabase (Transaction pooler)
 */
require("dotenv").config();
const express = require("express");
const cors = require("cors");
const { Pool } = require("pg");

const PORT = Number(process.env.PORT || 3000);
const DATABASE_URL = process.env.DATABASE_URL;

if (!DATABASE_URL) {
  console.error("Greska: treba DATABASE_URL vo .env (Supabase → Connect → URI).");
  process.exit(1);
}

/**
 * Gradime config za pg Pool.
 * ZOSO: Supabase pooler vraka self-signed cert; ako ostane sslmode=require
 * vo URL-to, pg moze da padne na SSL verify. Zatoa go briseme sslmode
 * od stringot i SSL go kontrolirame so { rejectUnauthorized: false }.
 */
function buildDbConfig(rawUrl) {
  try {
    const url = new URL(rawUrl);
    url.searchParams.delete("sslmode");
    return {
      connectionString: url.toString(),
      ssl: { rejectUnauthorized: false },
      max: 5, // mali pool — dovolno za student project / free plan
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

/**
 * Eden red od Postgres → cist JSON objekt sto Android go ocekuva.
 * ZOSO: tipovite od baza (numeric, jsonb) gi pretvorame vo Number / Array
 * za da Retrofit/Gson bezbedno gi mapira vo Company klasata.
 */
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

// Pri start proveri dali ima konekcija kon bazata (korisno za debug na Render)
(async () => {
  try {
    const { rows } = await pool.query("SELECT current_database() as db, current_user as usr");
    console.log("Postgres OK: db=" + rows[0].db + " user=" + rows[0].usr);
  } catch (e) {
    console.error("Postgres FAILED:", e.code, e.message);
  }
})();

const app = express();
// CORS: dozvoli HTTP povici od Android / browser (inak browser bi blokiral)
app.use(cors());
// JSON body parser: potreben za POST /companies (req.body)
app.use(express.json());

app.get("/", (_req, res) => {
  res.json({ ok: true, service: "mesto-api", endpoints: ["/health", "/companies"] });
});

// Health check — Render go koristi za da proveri dali servisot e ziv
app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

/**
 * GET /companies
 * Kako raboti: cita od tabela companies, opcionalno filtrira.
 *   ?category=service  — jsonb @> [slug] (kompanija so taa kategorija)
 *   ?q=ime             — ILIKE prebaruvanje po name
 * Android-ot vo ovaa verzija najcesto bara site, a filtrira lokalno po tab/search.
 */
app.get("/companies", async (req, res) => {
  try {
    // Query params od URL: /companies?category=service&q=cafe
    const category = req.query.category;
    const q = req.query.q;

    // Dinamicki WHERE — samo dodavame uslovi ako ima filter
    const where = [];
    const params = [];

    if (category) {
      // jsonb @> [slug] — kompanija cii categories go sodrzat toj slug
      params.push(JSON.stringify([String(category)]));
      where.push(`categories @> $${params.length}::jsonb`);
    }
    if (q) {
      // ILIKE = case-insensitive LIKE (npr. %cafe% najduva "Cafe Bar")
      params.push(`%${String(q)}%`);
      where.push(`name ILIKE $${params.length}`);
    }

    // $1, $2... = parametriziran SQL (NE string concat) → zashtita od SQL injection
    const sql =
      "SELECT id, name, address, latitude, longitude, email, phone, website, image_url, categories " +
      "FROM companies" +
      (where.length ? ` WHERE ${where.join(" AND ")}` : "") +
      " ORDER BY id DESC"; // najnovite prvi

    const { rows } = await pool.query(sql, params);
    // normalizeRow: tipovi (numeric/jsonb) → Number/Array za Android/Gson
    res.json(rows.map(normalizeRow));
  } catch (e) {
    console.error("GET /companies failed:", e.code, e.message);
    res.status(500).json({ error: "database_error", code: e.code, detail: e.message });
  }
});

/**
 * POST /companies
 * Kako raboti: validira body, pa INSERT vo Supabase.
 * Zadolzitelni: name, address, latitude, longitude, phone, categories[].
 * email/website se opcionalni (prazni stringovi ako nedostasuvaat).
 * Vraka 201 + noviot red — Android go koristi za da ja osvezi listata.
 */
app.post("/companies", async (req, res) => {
  const b = req.body || {};
  const required = ["name", "address", "latitude", "longitude", "phone"];
  for (const k of required) {
    if (b[k] === undefined || b[k] === null || b[k] === "") {
      return res.status(400).json({ error: "missing_" + k });
    }
  }
  if (b.email === undefined || b.email === null) b.email = "";
  if (b.website === undefined || b.website === null) b.website = "";

  const cats = b.categories;
  if (!Array.isArray(cats) || cats.length === 0) {
    return res.status(400).json({ error: "missing_categories" });
  }

  try {
    // $1..$9 + jsonb za categories — bez slepuvanje stringovi vo SQL
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
    res.status(201).json(normalizeRow(rows[0])); // 201 = Created
  } catch (e) {
    console.error("POST /companies failed:", e.code, e.message);
    res.status(500).json({ error: "database_error", code: e.code, detail: e.message });
  }
});

/**
 * DELETE /companies/:id
 * Kako raboti: brise red po id. 204 = uspesno bez body; 404 = ne postoi.
 * Se povikuva od Android koga korisnikot potvrdi brisenje vo AlertDialog.
 */
app.delete("/companies/:id", async (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id) || id <= 0) {
    return res.status(400).json({ error: "invalid_id" });
  }
  try {
    const { rowCount } = await pool.query(
      "DELETE FROM companies WHERE id = $1",
      [id]
    );
    if (rowCount === 0) {
      return res.status(404).json({ error: "not_found" });
    }
    res.status(204).end();
  } catch (e) {
    console.error("DELETE /companies failed:", e.code, e.message);
    res.status(500).json({ error: "database_error", code: e.code, detail: e.message });
  }
});

app.listen(PORT, () => {
  console.log(`mesto API slusa na http://localhost:${PORT}`);
});
