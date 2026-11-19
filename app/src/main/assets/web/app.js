// Toque – Weboberfläche für das Handy (ohne Bibliotheken, alles vom Gerät).
"use strict";

const $ = (id) => document.getElementById(id);
let TX = {};
/** Text in der Sprache des Geräts; %d/%s werden der Reihe nach ersetzt. */
function T(key, ...args) {
  let i = 0;
  return (TX[key] || key).replace(/%%|%[ds]/g, (m) => (m === "%%" ? "%" : String(args[i++] ?? "")));
}
function esc(s) {
  return String(s ?? "").replace(/[&<>"']/g, (c) => ({ "&": "&amp;", "<": "&lt;", ">": "&gt;", '"': "&quot;", "'": "&#39;" }[c]));
}
async function get(path) {
  const r = await fetch(path, { cache: "no-store" });
  if (!r.ok) throw Object.assign(new Error("HTTP " + r.status), { status: r.status, body: await r.json().catch(() => ({})) });
  return r.json();
}
async function post(path, body) {
  const r = await fetch(path, { method: "POST", headers: { "Content-Type": "application/json", "X-Toque": "1" }, body: JSON.stringify(body || {}) });
  const data = await r.json().catch(() => ({}));
  if (!r.ok) throw Object.assign(new Error("HTTP " + r.status), { status: r.status, body: data });
  return data;
}
let toastTimer = 0;
function toast(msg) {
  const t = $("toast");
  t.textContent = msg;
  t.hidden = false;
  clearTimeout(toastTimer);
  toastTimer = setTimeout(() => (t.hidden = true), 3200);
}
const num = (v) => (v >= 10 ? Math.round(v) : Math.round(v * 10) / 10).toLocaleString(document.documentElement.lang);

// ------------------------------------------------------------------ Zustand

const state = {
  tab: "recipes",
  q: "",
  sort: "RELEVANCE",
  favorites: false,
  mine: false,
  minutes: 0,
  kcal: 0,
  category: "",
  offset: 0,
  total: 0,
  categories: [],
  kcalLevels: [200, 300, 400, 500, 600, 800],
  detail: null,
  shoppingVersion: -1,
};

// ------------------------------------------------------------------ Rezepte

function renderChips() {
  const c = $("chips");
  const sorts = [["RELEVANCE", "sortRelevance"], ["POPULAR", "sortPopular"], ["NEWEST", "sortNewest"], ["RATING", "sortRating"], ["LIGHT", "sortLight"]];
  c.innerHTML =
    `<select class="chip${state.sort !== "RELEVANCE" ? " on" : ""}" id="sort">` +
    sorts.map(([v, k]) => `<option value="${v}"${state.sort === v ? " selected" : ""}>⇅ ${esc(T(k))}</option>`).join("") +
    `</select>` +
    `<button class="chip${state.favorites ? " on" : ""}" data-f="favorites">★ ${esc(T("favorites"))}</button>` +
    `<button class="chip${state.mine ? " on" : ""}" data-f="mine">✎ ${esc(T("mine"))}</button>` +
    `<button class="chip${state.minutes ? " on" : ""}" data-f="minutes">${esc(T("minutes30"))}</button>` +
    `<select class="chip${state.kcal ? " on" : ""}" id="kcal" title="${esc(T("kcalHint"))}"><option value="0">${esc(T("calories"))}</option>` +
    state.kcalLevels.map((k) => `<option value="${k}"${state.kcal === k ? " selected" : ""}>${esc(T("kcal500", k))}</option>`).join("") +
    `</select>` +
    `<select class="chip${state.category ? " on" : ""}" id="cat"><option value="">${esc(T("category"))}: ${esc(T("all"))}</option>` +
    state.categories.map((k) => `<option${state.category === k ? " selected" : ""}>${esc(k)}</option>`).join("") +
    `</select>`;
  $("sort").onchange = (e) => { state.sort = e.target.value; reload(); };
  $("cat").onchange = (e) => { state.category = e.target.value; reload(); };
  $("kcal").onchange = (e) => { state.kcal = Number(e.target.value); reload(); };
  c.querySelectorAll("button[data-f]").forEach((b) => (b.onclick = () => {
    const f = b.dataset.f;
    if (f === "favorites") state.favorites = !state.favorites;
    if (f === "mine") state.mine = !state.mine;
    if (f === "minutes") state.minutes = state.minutes ? 0 : 30;
    reload();
  }));
}

function query() {
  const p = new URLSearchParams({ q: state.q, sort: state.sort, offset: state.offset, limit: 40 });
  if (state.favorites) p.set("favorites", "1");
  if (state.mine) p.set("mine", "1");
  if (state.minutes) p.set("minutes", state.minutes);
  if (state.kcal) p.set("kcal", state.kcal);
  if (state.category) p.set("category", state.category);
  return "/api/recipes?" + p;
}

function card(r) {
  const meta = [];
  if (r.isNew) meta.push(`<span class="new">${esc(T("isNew"))}</span>`);
  if (r.own) meta.push("✎");
  if (r.ratingCount > 0) meta.push("★ " + num(r.rating));
  if (r.minutes > 0) meta.push(r.minutes + " min");
  if (r.kcal > 0) meta.push(r.kcal + " kcal");
  const img = r.image ? ` style="background-image:url('${esc(r.image)}')"` : "";
  return `<button class="card" data-id="${esc(r.id)}"><div class="img"${img}>${r.favorite ? '<span class="fav">★</span>' : ""}</div>` +
    `<div class="t">${esc(r.title)}</div><div class="m">${meta.join(" · ")}</div></button>`;
}

let searchSeq = 0;
async function reload(append = false) {
  renderChips();
  if (!append) state.offset = 0;
  const seq = ++searchSeq;
  try {
    const data = await get(query());
    if (seq !== searchSeq) return;
    state.total = data.total;
    const html = data.items.map(card).join("");
    const grid = $("grid");
    grid.innerHTML = append ? grid.innerHTML + html : html;
    $("count").textContent = (data.total === 1 ? T("countOne") : data.total ? T("count", data.total) : "") +
      (state.kcal || state.sort === "LIGHT" ? " · " + T("kcalHint") : "");
    if (!data.total) grid.innerHTML = `<p class="empty">${esc(T("none"))}</p>`;
    state.offset = data.offset + data.items.length;
    $("more").hidden = state.offset >= data.total;
    grid.querySelectorAll(".card").forEach((b) => (b.onclick = () => openDetail(b.dataset.id)));
  } catch (e) {
    $("count").textContent = T("offline");
  }
}

// ------------------------------------------------------------------ Rezeptseite

async function openDetail(id, servings = 0, push = true) {
  try {
    const r = await get(`/api/recipe?id=${encodeURIComponent(id)}&servings=${servings}`);
    state.detail = r;
    if (push) history.pushState({ recipe: id }, "", "#r=" + encodeURIComponent(id));
    show("detail");
    renderDetail();
    if (push) window.scrollTo(0, 0);
  } catch (e) {
    toast(T("offline"));
  }
}

function renderDetail() {
  const r = state.detail;
  $("title").textContent = r.title;
  const diff = { 1: T("easy"), 2: T("medium"), 3: T("hard") }[r.difficulty] || "";
  const meta = [];
  if (r.minutes > 0) meta.push(r.minutes + " min");
  meta.push(diff);
  if (r.ratingCount > 0) meta.push(`★ ${num(r.rating)} (${r.ratingCount})`);
  if (r.author) meta.push(T("by", /official/i.test(r.author) ? T("official") : r.author));
  if (r.own) meta.push(T("ownRecipe"));
  let html = `<div class="detail">`;
  if (r.image) html += `<div class="hero" style="background-image:url('${esc(r.image)}')"></div>`;
  html += `<h2>${esc(r.title)}</h2><div class="meta">${meta.map(esc).join(" · ")}</div>`;
  if (r.nutrition) {
    const n = r.nutrition;
    const per = r.servings > 0 && r.servingUnit ? T("perOf", r.servings, r.servingUnit) : T("perServing");
    // Eigene Rezepte geben oft nur die Kalorien an: dann keine „0 g“ für den Rest.
    const line = n.carbsG || n.fatG || n.proteinG ? T("nutrition", n.kcal, num(n.carbsG), num(n.fatG), num(n.proteinG)) : n.kcal + " kcal";
    html += `<div class="meta">${esc(line)} (${esc(per)})</div>`;
  }
  html += `<div class="actions">
    <button class="btn primary" id="open">▶ ${esc(T("openOnDevice"))}</button>
    <button class="btn" id="shop">+ ${esc(T("addShopping"))}</button>
    <button class="btn" id="fav">${r.favorite ? "★" : "☆"} ${esc(T("favorite"))}</button>
    ${r.own ? `<button class="btn" id="del">✕ ${esc(T("delete"))}</button>` : ""}
    ${r.url ? `<a class="btn" href="${esc(r.url)}" target="_blank" rel="noopener">${esc(T("website"))}</a>` : ""}
  </div>`;
  html += `<div class="box"><h3><span>${esc(T("ingredients"))}</span>`;
  if (r.servings > 0) {
    const label = r.servingUnit ? `${r.wanted} ${r.servingUnit}` : T("servings", r.wanted);
    html += `<span class="stepper"><button id="minus">−</button><b>${esc(label)}</b><button id="plus">+</button></span>`;
  }
  html += `</h3>`;
  if (r.scaled) html += `<p class="note">${esc(T("scaled", r.servings))}</p>`;
  for (const g of r.ingredients) {
    if (g.group) html += `<div class="group">${esc(g.group)}</div>`;
    html += `<ul class="ing">${g.items.map((i) => `<li>${esc(i)}</li>`).join("")}</ul>`;
  }
  html += `</div><div class="box"><h3><span>${esc(T("steps"))}</span></h3><ol class="steps">`;
  for (const s of r.steps) {
    html += `<li>${esc(s.text)}`;
    if (s.weigh) html += ` <span class="tag">⚖ ${esc(T("weigh", s.weigh))}</span>`;
    if (s.machine) html += ` <span class="tag${s.valid ? "" : " warn"}">${esc(s.machine)}${s.review ? " · ✋ " + esc(T("reviewOnDevice")) : ""}</span>`;
    html += `</li>`;
  }
  html += `</ol></div>`;
  if (r.notes) html += `<div class="box"><h3><span>${esc(T("notes"))}</span></h3><p style="white-space:pre-line;margin:0">${esc(r.notes)}</p></div>`;
  html += `</div>`;
  $("detail").innerHTML = html;

  const step = r.servings >= 12 ? Math.max(1, Math.floor(r.servings / 4)) : 1;
  const max = Math.max(r.servings * 4, 12);
  if ($("minus")) $("minus").onclick = () => r.wanted - step >= step && openDetail(r.id, r.wanted - step, false);
  if ($("plus")) $("plus").onclick = () => r.wanted + step <= max && openDetail(r.id, r.wanted + step, false);
  $("open").onclick = async () => {
    try {
      await post("/api/open", { id: r.id, servings: r.wanted });
      toast(T("opened"));
    } catch (e) {
      toast(e.body && e.body.error === "vendor" ? T("vendor") : e.status === 409 ? T("busy") : T("offline"));
    }
  };
  $("shop").onclick = async () => {
    try {
      renderShopping(await post("/api/shopping/add", { id: r.id, servings: r.wanted }));
      toast(T("added", r.ingredients.reduce((n, g) => n + g.items.length, 0)));
    } catch (e) { toast(T("offline")); }
  };
  if ($("del")) $("del").onclick = async () => {
    if (!confirm(T("deleteConfirm"))) return;
    try {
      await post("/api/delete", { id: r.id });
      toast(T("deleted"));
      history.back();
      reload();
    } catch (e) { toast(T("offline")); }
  };
  $("fav").onclick = async () => {
    try {
      await post("/api/favorite", { id: r.id, on: !r.favorite });
      r.favorite = !r.favorite;
      renderDetail();
    } catch (e) { toast(T("offline")); }
  };
}

// ------------------------------------------------------------------ Einkaufsliste

async function loadShopping(force = false) {
  try {
    const data = await get("/api/shopping");
    if (!force && data.version === state.shoppingVersion) return;
    renderShopping(data);
  } catch (e) {
    if (force) $("shopping").innerHTML = `<p class="empty">${esc(T("offline"))}</p>`;
  }
}

function renderShopping(data) {
  state.shoppingVersion = data.version;
  $("tabShopping").textContent = T("shopping") + (data.open ? ` (${data.open})` : "");
  let html = `<div class="actions"><button class="btn" id="rmChecked">✓ ${esc(T("removeChecked"))}</button>` +
    `<button class="btn" id="clearAll">✕ ${esc(T("clearAll"))}</button></div>`;
  if (!data.groups.length) html += `<p class="empty">${esc(T("emptyList"))}</p>`;
  for (const g of data.groups) {
    html += `<div class="aisle">${esc(g.aisle)}</div>`;
    for (const i of g.items) {
      html += `<button class="item${i.checked ? " done" : ""}" data-key="${esc(i.key)}"><span class="box2">${i.checked ? "✓" : ""}</span>` +
        `<span><div class="name">${esc(i.text)}</div><div class="from">${esc(i.recipes.join(" · "))}</div></span></button>`;
    }
  }
  $("shopping").innerHTML = html;
  $("shopping").querySelectorAll(".item").forEach((b) => (b.onclick = async () => {
    try { renderShopping(await post("/api/shopping/toggle", { key: b.dataset.key })); } catch (e) { toast(T("offline")); }
  }));
  $("rmChecked").onclick = async () => {
    try { renderShopping(await post("/api/shopping/clear-checked")); } catch (e) { toast(T("offline")); }
  };
  $("clearAll").onclick = async () => {
    if (!confirm(T("clearConfirm"))) return;
    try { renderShopping(await post("/api/shopping/clear")); } catch (e) { toast(T("offline")); }
  };
}

// ------------------------------------------------------------------ Eigenes Rezept

async function copyPrompt() {
  let text = "";
  try {
    text = await (await fetch("/api/prompt", { cache: "no-store" })).text();
  } catch (e) { toast(T("offline")); return; }
  // Im Heimnetz (http) gibt es oft keine Zwischenablage-API: dann markiertes Textfeld als Ersatz.
  try {
    await navigator.clipboard.writeText(text);
    toast(T("copied"));
    return;
  } catch (e) {}
  const box = $("promptText");
  box.value = text;
  box.hidden = false;
  box.focus();
  box.select();
  box.setSelectionRange(0, text.length);
  let ok = false;
  try { ok = document.execCommand("copy"); } catch (e) {}
  if (ok) toast(T("copied"));
  else $("copyManual").hidden = false;
}

function list(items) {
  return `<ul>${items.map((i) => `<li>${i}</li>`).join("")}</ul>`;
}

async function importRecipes() {
  const text = $("paste").value.trim();
  if (!text) return;
  const btn = $("importSave");
  btn.disabled = true;
  let data;
  try {
    const r = await fetch("/api/import", { method: "POST", headers: { "Content-Type": "application/json", "X-Toque": "1" }, body: text });
    data = await r.json().catch(() => ({}));
    if (r.status === 409) { toast(T("busy")); return; }
    if (!r.ok && !data.errors) throw new Error("HTTP " + r.status);
  } catch (e) {
    toast(T("offline"));
    return;
  } finally {
    btn.disabled = false;
  }
  let html = "";
  const n = (data.imported || []).length;
  if (n) {
    html += `<div class="result"><h4>✓ ${esc(n === 1 ? T("importedOne") : T("importedN", n))}</h4>` +
      list(data.imported.map((r) => `<a href="#r=${encodeURIComponent(r.id)}">${esc(r.title)}</a>`)) + `</div>`;
    $("paste").value = "";
  }
  if ((data.warnings || []).length) html += `<div class="result warn"><h4>${esc(T("importWarnings"))}</h4>${list(data.warnings.map(esc))}</div>`;
  if ((data.errors || []).length) html += `<div class="result warn"><h4>${esc(T("importErrors"))}</h4>${list(data.errors.map(esc))}</div>`;
  $("importResult").innerHTML = html;
  if (n) { state.mine = true; reload(); }
}

// ------------------------------------------------------------------ Status des Geräts

function clock(ms) {
  const s = Math.max(0, Math.round(ms / 1000));
  return `${Math.floor(s / 60)}:${String(s % 60).padStart(2, "0")}`;
}

async function pollStatus() {
  const box = $("status");
  try {
    const s = await get("/api/status");
    if (s.busy || s.fault) {
      const parts = [s.program || s.state];
      if (s.remainingMs > 0) parts[0] = T("left", parts[0], clock(s.remainingMs));
      if (s.tempC != null && s.tempC > 0) parts.push(s.tempC + " °C");
      if (s.fault) parts.push(s.fault);
      box.textContent = parts.join(" · ");
      box.hidden = false;
    } else box.hidden = true;
  } catch (e) {
    box.textContent = T("offline");
    box.hidden = false;
  }
}

// ------------------------------------------------------------------ Navigation

function show(view) {
  for (const v of ["recipes", "detail", "shopping", "add"]) $(v).hidden = v !== view;
  $("back").hidden = view !== "detail";
  if (view !== "detail") $("title").textContent = "Toque";
  $("tabRecipes").classList.toggle("on", view === "recipes" || view === "detail");
  $("tabShopping").classList.toggle("on", view === "shopping");
  $("tabAdd").classList.toggle("on", view === "add");
  state.tab = view;
}

function route() {
  const m = location.hash.match(/^#r=(.+)$/);
  if (m) openDetail(decodeURIComponent(m[1]), 0, false);
  else if (location.hash === "#shopping") { show("shopping"); loadShopping(true); }
  else if (location.hash === "#add") show("add");
  else show("recipes");
}

async function start() {
  try {
    const t = await get("/api/texts");
    TX = t.texts;
    document.documentElement.lang = t.language;
  } catch (e) {}
  $("tabRecipes").textContent = T("recipes");
  $("tabShopping").textContent = T("shopping");
  $("q").placeholder = T("search");
  $("more").textContent = T("more");
  $("back").setAttribute("aria-label", T("back"));
  $("tabAdd").setAttribute("aria-label", T("addRecipe"));
  $("tabAdd").title = T("addRecipe");
  $("addHelp").textContent = T("addHelp");
  $("copyPrompt").textContent = "⧉ " + T("copyPrompt");
  $("copyManual").textContent = T("copyManual");
  $("paste").placeholder = T("pasteHere");
  $("importSave").textContent = "✓ " + T("importSave");
  $("copyPrompt").onclick = copyPrompt;
  $("importSave").onclick = importRecipes;
  $("tabAdd").onclick = () => { history.pushState({}, "", "#add"); show("add"); };
  try { state.categories = await get("/api/categories"); } catch (e) {}
  try { state.kcalLevels = (await get("/api/filters")).kcal; } catch (e) {}
  let timer = 0;
  $("q").oninput = (e) => { clearTimeout(timer); timer = setTimeout(() => { state.q = e.target.value; reload(); }, 250); };
  $("more").onclick = () => reload(true);
  $("back").onclick = () => history.back();
  $("tabRecipes").onclick = () => { history.pushState({}, "", "#"); show("recipes"); };
  $("tabShopping").onclick = () => { history.pushState({}, "", "#shopping"); show("shopping"); loadShopping(true); };
  window.onpopstate = route;
  reload();
  loadShopping(false);
  route();
  pollStatus();
  setInterval(pollStatus, 5000);
  setInterval(() => { if (state.tab === "shopping" && !document.hidden) loadShopping(false); }, 5000);
}
start();
