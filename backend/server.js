/**
 * Мини REST API: Express + mysql2
 * Пушти: npm install && npm start (ја чита .env)
 */
require("dotenv").config();
const express = require("express");
const cors = require("cors");
const mysql = require("mysql2/promise");

const PORT = Number(process.env.PORT || 3000);

const pool = mysql.createPool({
  host: process.env.DB_HOST || "127.0.0.1",
  port: Number(process.env.DB_PORT || 3306),
  user: process.env.DB_USER || "root",
  password: process.env.DB_PASSWORD || "",
  database: process.env.DB_NAME || "mesto",
  waitForConnections: true,
  connectionLimit: 10,
});

(async () => {
  try {
    await pool.query("SELECT 1");
    console.log("MySQL: поврзано со базата „" + (process.env.DB_NAME || "mesto") + "“.");
  } catch (e) {
    console.error(
      "MySQL: НЕМА врска. Пушти го MySQL, провери .env (DB_USER/DB_PASSWORD) и дека постои базата (schema.sql).\n",
      e.message
    );
  }
})();

const app = express();
app.use(cors());
app.use(express.json());

app.get("/health", (_req, res) => {
  res.json({ ok: true });
});

app.get("/companies", async (_req, res) => {
  try {
    const [rows] = await pool.query(
      "SELECT id, name, address, latitude, longitude, email, phone, website, category FROM companies ORDER BY id DESC"
    );
    const out = rows.map((r) => ({
      id: Number(r.id),
      name: r.name,
      address: r.address,
      latitude: Number(r.latitude),
      longitude: Number(r.longitude),
      email: r.email,
      phone: r.phone,
      website: r.website,
      category: r.category,
    }));
    res.json(out);
  } catch (e) {
    console.error(e);
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
    "category",
  ];
  for (const k of required) {
    if (b[k] === undefined || b[k] === null || b[k] === "") {
      return res.status(400).json({ error: "missing_" + k });
    }
  }

  try {
    const [result] = await pool.execute(
      `INSERT INTO companies (name, address, latitude, longitude, email, phone, website, category)
       VALUES (?, ?, ?, ?, ?, ?, ?, ?)`,
      [
        String(b.name),
        String(b.address),
        Number(b.latitude),
        Number(b.longitude),
        String(b.email),
        String(b.phone),
        String(b.website),
        String(b.category),
      ]
    );
    const id = result.insertId;
    const [rows] = await pool.query(
      "SELECT id, name, address, latitude, longitude, email, phone, website, category FROM companies WHERE id = ?",
      [id]
    );
    const row = rows[0];
    res.status(201).json({
      id: Number(row.id),
      name: row.name,
      address: row.address,
      latitude: Number(row.latitude),
      longitude: Number(row.longitude),
      email: row.email,
      phone: row.phone,
      website: row.website,
      category: row.category,
    });
  } catch (e) {
    console.error(e);
    res.status(500).json({ error: "database_error" });
  }
});

app.listen(PORT, () => {
  console.log(`mesto API на http://localhost:${PORT}`);
});
