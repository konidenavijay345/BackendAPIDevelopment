const TOKEN_KEY = "backendApiJwt";
const baseUrl = window.location.origin;
const logLimit = 80;

const output = document.querySelector("#output");
const tokenBox = document.querySelector("#tokenBox");
const authState = document.querySelector("#authState");
const productsEl = document.querySelector("#products");
const productForm = document.querySelector("#productForm");
const serverState = document.querySelector("#serverState");
const serverHealth = document.querySelector("#serverHealth");
const serverHealthMeta = document.querySelector("#serverHealthMeta");
const connectionMode = document.querySelector("#connectionMode");
const connectionMeta = document.querySelector("#connectionMeta");
const environmentName = document.querySelector("#environmentName");
const environmentMeta = document.querySelector("#environmentMeta");
const lastRequestState = document.querySelector("#lastRequestState");
const lastRequestMeta = document.querySelector("#lastRequestMeta");
const liveLogs = document.querySelector("#liveLogs");
const requestBuilder = document.querySelector("#requestBuilder");
const requestTemplate = document.querySelector("#requestTemplate");
const requestMethod = document.querySelector("#requestMethod");
const requestPath = document.querySelector("#requestPath");
const queryParamsInput = document.querySelector("#queryParamsInput");
const headersInput = document.querySelector("#headersInput");
const bodyInput = document.querySelector("#bodyInput");
const includeAuthHeader = document.querySelector("#includeAuthHeader");
const sendRequestBtn = document.querySelector("#sendRequestBtn");
const responseStatus = document.querySelector("#responseStatus");
const responseTime = document.querySelector("#responseTime");
const responseSize = document.querySelector("#responseSize");
const responseBody = document.querySelector("#responseBody");
const responseHeaders = document.querySelector("#responseHeaders");
const responseRaw = document.querySelector("#responseRaw");
const requestHistoryEl = document.querySelector("#requestHistory");
const HISTORY_KEY = "backendApiRequestHistory";
let lastCurlCommand = "";

const requestTemplates = {
  health: {
    method: "GET",
    path: "/actuator/health",
    query: "",
    headers: "",
    body: ""
  },
  register: {
    method: "POST",
    path: "/api/v1/auth/register",
    query: "",
    headers: "Content-Type: application/json",
    body: () => JSON.stringify({
      name: "Vijay",
      email: uniqueEmail(),
      password: "Password@123"
    }, null, 2)
  },
  login: {
    method: "POST",
    path: "/api/v1/auth/login",
    query: "",
    headers: "Content-Type: application/json",
    body: () => JSON.stringify({
      email: localStorage.getItem("backendApiEmail") || "vijay@example.com",
      password: "Password@123"
    }, null, 2)
  },
  listProducts: {
    method: "GET",
    path: "/api/v1/products",
    query: "page=0\nsize=5\nsort=id,desc",
    headers: "",
    body: ""
  },
  createProduct: {
    method: "POST",
    path: "/api/v1/products",
    query: "",
    headers: "Content-Type: application/json",
    body: () => JSON.stringify({
      name: "Mechanical Keyboard",
      sku: `SKU-${Date.now()}`,
      description: "Hot-swappable mechanical keyboard",
      price: 89.99,
      quantity: 25
    }, null, 2)
  },
  updateProduct: {
    method: "PUT",
    path: "/api/v1/products/1",
    query: "",
    headers: "Content-Type: application/json",
    body: () => JSON.stringify({
      name: "Mechanical Keyboard",
      sku: `SKU-${Date.now()}`,
      description: "Updated product details",
      price: 99.99,
      quantity: 20
    }, null, 2)
  },
  deleteProduct: {
    method: "DELETE",
    path: "/api/v1/products/1",
    query: "",
    headers: "",
    body: ""
  }
};

document.querySelector("#baseUrl").textContent = `Base URL: ${baseUrl}`;
document.querySelector("#registerForm [name=email]").value = uniqueEmail();
document.querySelector("#loginForm [name=email]").value = localStorage.getItem("backendApiEmail") || "";
document.querySelector("#productForm [name=sku]").value = `SKU-${Date.now()}`;
headersInput.value = "Content-Type: application/json";

syncConnectionUi();
applyRequestTemplate("listProducts");
renderHistory();
addLog("info", "Interface loaded", `Using ${baseUrl}`);

function uniqueEmail() {
  return `vijay.${Date.now()}@example.com`;
}

function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
}

function decodeJwtPayload(token) {
  try {
    const payload = token.split(".")[1];
    const normalized = payload.replace(/-/g, "+").replace(/_/g, "/");
    const padded = normalized.padEnd(Math.ceil(normalized.length / 4) * 4, "=");
    return JSON.parse(atob(padded));
  } catch {
    return null;
  }
}

function getTokenStatus() {
  const token = getToken();
  if (!token) {
    return { state: "missing", label: "Not logged in", message: "Login or register first" };
  }

  const payload = decodeJwtPayload(token);
  if (!payload || !payload.exp) {
    return { state: "invalid", label: "JWT invalid", message: "Token cannot be decoded" };
  }

  const secondsRemaining = Math.floor(payload.exp - Date.now() / 1000);
  const expiresAt = new Date(payload.exp * 1000);
  if (secondsRemaining <= 0) {
    return {
      state: "expired",
      label: "JWT expired",
      message: `Expired at ${expiresAt.toLocaleTimeString([], { hour12: false })}`,
      subject: payload.sub,
      expiresAt,
      secondsRemaining
    };
  }

  return {
    state: "valid",
    label: `JWT valid ${formatDuration(secondsRemaining)}`,
    message: `Expires at ${expiresAt.toLocaleTimeString([], { hour12: false })}`,
    subject: payload.sub,
    expiresAt,
    secondsRemaining
  };
}

function isTokenUsable() {
  return getTokenStatus().state === "valid";
}

function formatDuration(totalSeconds) {
  const minutes = Math.floor(totalSeconds / 60);
  const seconds = totalSeconds % 60;
  return minutes > 0 ? `${minutes}m ${seconds}s` : `${seconds}s`;
}

function requiresAuth(pathOrUrl) {
  try {
    return new URL(pathOrUrl, baseUrl).pathname.startsWith("/api/v1/products");
  } catch {
    return String(pathOrUrl).startsWith("/api/v1/products");
  }
}

function authRequiredError(pathOrUrl) {
  const status = getTokenStatus();
  const message = status.state === "expired"
    ? "Your JWT expired. Login again, then retry the product request."
    : "A valid JWT is required. Login or register first, then retry the product request.";
  const error = {
    status: 401,
    error: "Unauthorized",
    message,
    path: new URL(pathOrUrl, baseUrl).pathname,
    tokenStatus: status.message
  };

  show(error);
  setResponseSummary({ ok: false, status: 401, statusText: "Unauthorized" }, null, 0);
  setResponseTabs(JSON.stringify(error, null, 2), "", JSON.stringify(error));
  setLastRequest("error", "JWT required", `${error.path} was not sent`);
  addLog("error", "JWT required", message);
  return error;
}

function setToken(token, email) {
  localStorage.setItem(TOKEN_KEY, token);
  if (email) localStorage.setItem("backendApiEmail", email);
  syncTokenUi();
}

function clearToken() {
  localStorage.removeItem(TOKEN_KEY);
  syncTokenUi();
}

function syncTokenUi() {
  const token = getToken();
  const status = getTokenStatus();
  tokenBox.value = token;
  authState.textContent = status.label;
  authState.title = status.message;
  authState.className = `pill ${status.state === "valid" ? "ok" : status.state === "missing" ? "muted" : "error"}`;
}

function syncConnectionUi() {
  const host = window.location.hostname;
  const isTunnel = host.endsWith(".trycloudflare.com");
  const isLocal = ["localhost", "127.0.0.1", ""].includes(host);
  const metric = environmentName.closest(".metric");

  if (isTunnel) {
    connectionMode.textContent = "Public tunnel";
    connectionMeta.textContent = "Cloudflare tunnel is serving this page";
    setMetric(metric, environmentName, environmentMeta, "ok", "Public tunnel", host);
  } else if (isLocal) {
    connectionMode.textContent = "Local";
    connectionMeta.textContent = "Browser is connected to this machine";
    setMetric(metric, environmentName, environmentMeta, "ok", "Local", baseUrl);
  } else {
    connectionMode.textContent = "Remote";
    connectionMeta.textContent = host;
    setMetric(metric, environmentName, environmentMeta, "warn", "Remote", host);
  }
}

function show(data) {
  output.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

function nowTime() {
  return new Date().toLocaleTimeString([], { hour12: false });
}

function addLog(level, title, detail = "") {
  const entry = document.createElement("div");
  entry.className = `log-entry ${level}`;
  entry.innerHTML = `
    <span class="log-time">${nowTime()}</span>
    <span>
      <strong class="log-title">${escapeHtml(title)}</strong>
      <span class="log-detail">${escapeHtml(detail)}</span>
    </span>
  `;
  liveLogs.prepend(entry);

  while (liveLogs.children.length > logLimit) {
    liveLogs.lastElementChild.remove();
  }
}

function setMetric(metricElement, valueElement, metaElement, state, value, meta) {
  metricElement.classList.remove("ok", "warn", "error");
  if (state) metricElement.classList.add(state);
  valueElement.textContent = value;
  metaElement.textContent = meta;
}

function setLastRequest(state, title, meta) {
  setMetric(lastRequestState.closest(".metric"), lastRequestState, lastRequestMeta, state, title, meta);
}

function setButtonLoading(button, isLoading, loadingText = "Working") {
  if (!button) return;

  if (isLoading) {
    button.dataset.label = button.textContent;
    button.textContent = loadingText;
    button.classList.add("loading");
    button.disabled = true;
    return;
  }

  button.textContent = button.dataset.label || button.textContent;
  button.classList.remove("loading");
  button.disabled = false;
  delete button.dataset.label;
}

async function withButtonLoading(button, loadingText, task) {
  setButtonLoading(button, true, loadingText);
  try {
    return await task();
  } finally {
    setButtonLoading(button, false);
  }
}

function applyRequestTemplate(templateKey) {
  const template = requestTemplates[templateKey];
  if (!template) return;

  requestMethod.value = template.method;
  requestPath.value = template.path;
  queryParamsInput.value = template.query;
  headersInput.value = template.headers;
  bodyInput.value = typeof template.body === "function" ? template.body() : template.body;
  requestTemplate.value = templateKey;
  document.querySelectorAll("[data-template-shortcut]").forEach((button) => {
    button.classList.toggle("active", button.dataset.templateShortcut === templateKey);
  });
  document.querySelectorAll(".request-tab").forEach((button) => {
    button.classList.toggle("active", button.dataset.templateShortcut === templateKey);
  });
  addLog("info", "Request template loaded", `${template.method} ${template.path}`);
}

function parseKeyValueLines(text, separator = ":") {
  return text.split(/\r?\n/)
    .map((line) => line.trim())
    .filter(Boolean)
    .reduce((items, line) => {
      const index = line.indexOf(separator);
      if (index === -1) {
        items.push([line, ""]);
      } else {
        items.push([line.slice(0, index).trim(), line.slice(index + 1).trim()]);
      }
      return items;
    }, []);
}

function buildRequestUrl(path, queryText) {
  const url = new URL(path, baseUrl);
  parseKeyValueLines(queryText, "=").forEach(([key, value]) => {
    if (key) url.searchParams.set(key, value);
  });
  return url;
}

function buildHeaders() {
  const headers = {};
  parseKeyValueLines(headersInput.value, ":").forEach(([key, value]) => {
    if (key) headers[key] = value;
  });

  const token = getToken();
  const hasAuthHeader = Object.keys(headers).some((key) => key.toLowerCase() === "authorization");
  if (includeAuthHeader.checked && token && !hasAuthHeader) {
    headers.Authorization = `Bearer ${token}`;
  }

  return headers;
}

function formatBytes(bytes) {
  if (bytes < 1024) return `${bytes} B`;
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(1)} KB`;
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`;
}

function setResponseSummary(response, duration, size) {
  const ok = response && response.ok;
  responseStatus.className = `pill ${ok ? "ok" : response ? "error" : "muted"}`;
  responseStatus.textContent = response ? `${response.status} ${response.statusText || ""}`.trim() : "No response";
  responseTime.textContent = duration == null ? "Time: -" : `Time: ${duration} ms`;
  responseSize.textContent = size == null ? "Size: -" : `Size: ${formatBytes(size)}`;
}

function setResponseTabs(body, headers, raw) {
  responseBody.textContent = body || "";
  responseHeaders.textContent = headers || "";
  responseRaw.textContent = raw || "";
}

function responseHeadersToText(headers) {
  return Array.from(headers.entries())
    .map(([key, value]) => `${key}: ${value}`)
    .join("\n");
}

function buildCurl(method, url, headers, body) {
  const parts = [`curl -X ${method}`, `"${url}"`];
  Object.entries(headers).forEach(([key, value]) => {
    parts.push(`-H "${key}: ${String(value).replaceAll('"', '\\"')}"`);
  });
  if (body) {
    parts.push(`--data '${body.replaceAll("'", "'\\''")}'`);
  }
  return parts.join(" \\\n  ");
}

function readHistory() {
  try {
    return JSON.parse(localStorage.getItem(HISTORY_KEY) || "[]");
  } catch {
    return [];
  }
}

function saveHistoryItem(item) {
  const history = [item, ...readHistory()].slice(0, 20);
  localStorage.setItem(HISTORY_KEY, JSON.stringify(history));
  renderHistory();
}

function renderHistory() {
  const history = readHistory();
  if (!history.length) {
    requestHistoryEl.innerHTML = '<p class="empty">No requests sent yet.</p>';
    return;
  }

  requestHistoryEl.innerHTML = history.map((item, index) => `
    <button class="history-item" type="button" data-history-index="${index}">
      <span class="history-method">${escapeHtml(item.method)}</span>
      <span class="history-path">${escapeHtml(item.path)}</span>
      <span class="history-status ${item.ok ? "ok" : "error"}">${escapeHtml(item.status)}</span>
      <span>${escapeHtml(String(item.duration))} ms</span>
    </button>
  `).join("");

  requestHistoryEl.querySelectorAll("[data-history-index]").forEach((button) => {
    button.addEventListener("click", () => {
      const item = readHistory()[Number(button.dataset.historyIndex)];
      if (!item) return;
      requestMethod.value = item.method;
      requestPath.value = item.path;
      queryParamsInput.value = item.query || "";
      headersInput.value = item.headers || "";
      bodyInput.value = item.body || "";
      addLog("info", "History restored", `${item.method} ${item.path}`);
    });
  });
}

async function sendCustomRequest() {
  const method = requestMethod.value;
  const url = buildRequestUrl(requestPath.value.trim(), queryParamsInput.value);
  const headers = buildHeaders();
  const rawBody = bodyInput.value.trim();
  const body = ["GET", "HEAD"].includes(method) ? undefined : rawBody;
  const startedAt = performance.now();

  if (includeAuthHeader.checked && requiresAuth(url.href) && !isTokenUsable()) {
    authRequiredError(url.href);
    return;
  }

  setLastRequest("warn", "Running", `${method} ${url.pathname}${url.search}`);
  setResponseSummary(null, null, null);
  setResponseTabs("Waiting for response...", "", "");
  addLog("pending", "Manual request started", `${method} ${url.href}`);

  lastCurlCommand = buildCurl(method, url.href, headers, body);

  let response;
  let text = "";
  try {
    response = await fetch(url.href, { method, headers, body });
    text = await response.text();
  } catch (error) {
    const duration = Math.round(performance.now() - startedAt);
    setLastRequest("error", "Network failed", `${method} ${url.pathname} after ${duration} ms`);
    setResponseSummary({ ok: false, status: "ERR", statusText: "Network failed" }, duration, 0);
    setResponseTabs(JSON.stringify({ message: error.message }, null, 2), "", error.message || "");
    addLog("error", "Manual request failed", `${error.message}. Check backend or tunnel connection.`);
    return;
  }

  const duration = Math.round(performance.now() - startedAt);
  const bodyObject = parseResponseBody(text);
  const prettyBody = typeof bodyObject === "object" ? JSON.stringify(bodyObject, null, 2) : String(bodyObject || "");
  const headerText = responseHeadersToText(response.headers);
  const size = new Blob([text]).size;

  setResponseSummary(response, duration, size);
  setResponseTabs(prettyBody, headerText, text);
  show(bodyObject || { status: response.status });
  setLastRequest(response.ok ? "ok" : "error", `${response.status} ${response.statusText}`, `${method} ${url.pathname}${url.search} in ${duration} ms`);
  addLog(response.ok ? "ok" : "error", "Manual request completed", `${method} ${url.pathname}${url.search} returned ${response.status} in ${duration} ms`);

  saveHistoryItem({
    method,
    path: requestPath.value.trim(),
    query: queryParamsInput.value,
    headers: headersInput.value,
    body: bodyInput.value,
    status: String(response.status),
    ok: response.ok,
    duration
  });
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const method = options.method || "GET";
  if (requiresAuth(path) && !isTokenUsable()) {
    throw authRequiredError(path);
  }

  const startedAt = performance.now();
  setLastRequest("warn", "Running", `${method} ${path}`);
  addLog("pending", "Request started", `${method} ${path}`);

  let response;
  let text = "";
  let body = null;

  try {
    response = await fetch(`${baseUrl}${path}`, { ...options, headers });
    text = await response.text();
    body = parseResponseBody(text);
  } catch (error) {
    const duration = Math.round(performance.now() - startedAt);
    const message = error.message || "Network request failed";
    setLastRequest("error", "Network failed", `${method} ${path} after ${duration} ms`);
    addLog("error", "Server connection failed", `${message}. Check whether the backend and tunnel are running.`);
    show({ status: "network-error", message });
    throw error;
  }

  const duration = Math.round(performance.now() - startedAt);

  if (!response.ok) {
    const error = body || { status: response.status, message: response.statusText || text };
    const message = error.message || response.statusText || "Request failed";
    setLastRequest("error", `${response.status} ${response.statusText}`, `${method} ${path} in ${duration} ms`);
    addLog("error", "Request failed", `${method} ${path} returned ${response.status}. ${message}`);
    show(error);
    throw error;
  }

  setLastRequest("ok", `${response.status} ${response.statusText || "OK"}`, `${method} ${path} in ${duration} ms`);
  addLog("ok", "Request completed", `${method} ${path} returned ${response.status} in ${duration} ms`);
  show(body || { status: response.status });
  return body;
}

function parseResponseBody(text) {
  if (!text) return null;
  try {
    return JSON.parse(text);
  } catch {
    return { message: text };
  }
}

function formJson(form) {
  return Object.fromEntries(new FormData(form).entries());
}

document.querySelector("#registerForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  await withButtonLoading(event.submitter, "Registering", async () => {
    try {
      const body = formJson(event.currentTarget);
      const response = await api("/api/v1/auth/register", {
        method: "POST",
        body: JSON.stringify(body)
      });
      setToken(response.token, body.email);
      addLog("ok", "JWT saved", `Registered and saved token for ${body.email}`);
      document.querySelector("#loginForm [name=email]").value = body.email;
      await loadProducts();
    } catch {
      // api() already renders the response.
    }
  });
});

document.querySelector("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  await withButtonLoading(event.submitter, "Logging in", async () => {
    try {
      const body = formJson(event.currentTarget);
      const response = await api("/api/v1/auth/login", {
        method: "POST",
        body: JSON.stringify(body)
      });
      setToken(response.token, body.email);
      addLog("ok", "JWT saved", `Logged in as ${body.email}`);
      await loadProducts();
    } catch {
      // api() already renders the response.
    }
  });
});

productForm.addEventListener("submit", async (event) => {
  event.preventDefault();
  await withButtonLoading(event.submitter, "Saving", async () => {
    try {
      const values = formJson(productForm);
      const id = values.id;
      const payload = {
        name: values.name,
        sku: values.sku,
        description: values.description,
        price: Number(values.price),
        quantity: Number(values.quantity)
      };

      await api(id ? `/api/v1/products/${id}` : "/api/v1/products", {
        method: id ? "PUT" : "POST",
        body: JSON.stringify(payload)
      });

      addLog("ok", "Product saved", `${payload.name} is now stored on the server`);
      resetProductForm();
      await loadProducts();
    } catch {
      // api() already renders the response.
    }
  });
});

async function loadProducts() {
  if (!getToken()) {
    productsEl.innerHTML = '<p class="empty">Login or register first.</p>';
    addLog("warn", "Products paused", "A JWT is required before loading products");
    return;
  }

  const page = await api("/api/v1/products?page=0&size=5&sort=id,desc");
  const products = page.content || [];

  if (!products.length) {
    productsEl.innerHTML = '<p class="empty">No products yet.</p>';
    return;
  }

  productsEl.innerHTML = products.map((product) => `
    <article class="product">
      <div>
        <strong>${escapeHtml(product.name)}</strong>
        <span>${escapeHtml(product.sku)} &middot; ${product.quantity} pcs &middot; ${product.price}</span>
      </div>
      <div class="row-actions">
        <button type="button" data-edit="${product.id}">Edit</button>
        <button type="button" data-delete="${product.id}">Delete</button>
      </div>
    </article>
  `).join("");

  productsEl.querySelectorAll("[data-edit]").forEach((button) => {
    button.addEventListener("click", () => {
      const product = products.find((item) => String(item.id) === button.dataset.edit);
      fillProductForm(product);
      addLog("info", "Product loaded for edit", `${product.name} is ready in the form`);
    });
  });

  productsEl.querySelectorAll("[data-delete]").forEach((button) => {
    button.addEventListener("click", async () => {
      await withButtonLoading(button, "Deleting", async () => {
        await api(`/api/v1/products/${button.dataset.delete}`, { method: "DELETE" });
        addLog("ok", "Product deleted", `Product ${button.dataset.delete} was removed`);
        await loadProducts();
      });
    });
  });
}

function fillProductForm(product) {
  productForm.id.value = product.id;
  productForm.name.value = product.name;
  productForm.sku.value = product.sku;
  productForm.description.value = product.description || "";
  productForm.price.value = product.price;
  productForm.quantity.value = product.quantity;
}

function resetProductForm() {
  productForm.reset();
  productForm.id.value = "";
  productForm.name.value = "Mechanical Keyboard";
  productForm.sku.value = `SKU-${Date.now()}`;
  productForm.description.value = "Hot-swappable mechanical keyboard";
  productForm.price.value = "89.99";
  productForm.quantity.value = "25";
}

function escapeHtml(value) {
  return String(value ?? "").replace(/[&<>"']/g, (char) => ({
    "&": "&amp;",
    "<": "&lt;",
    ">": "&gt;",
    '"': "&quot;",
    "'": "&#39;"
  }[char]));
}

document.querySelector("#refreshProductsBtn").addEventListener("click", async (event) => {
  await withButtonLoading(event.currentTarget, "Refreshing", loadProducts);
});

requestTemplate.addEventListener("change", () => applyRequestTemplate(requestTemplate.value));

document.querySelectorAll("[data-template-shortcut]").forEach((button) => {
  button.addEventListener("click", () => applyRequestTemplate(button.dataset.templateShortcut));
});

requestBuilder.addEventListener("submit", async (event) => {
  event.preventDefault();
  await withButtonLoading(sendRequestBtn, "Sending", sendCustomRequest);
});

document.querySelector("#formatBodyBtn").addEventListener("click", () => {
  if (!bodyInput.value.trim()) return;
  try {
    bodyInput.value = JSON.stringify(JSON.parse(bodyInput.value), null, 2);
    addLog("ok", "JSON formatted", "Request body is valid JSON");
  } catch (error) {
    addLog("error", "JSON format failed", error.message);
  }
});

document.querySelector("#clearRequestBtn").addEventListener("click", () => {
  requestMethod.value = "GET";
  requestPath.value = "";
  queryParamsInput.value = "";
  headersInput.value = "";
  bodyInput.value = "";
  setResponseSummary(null, null, null);
  setResponseTabs("", "", "");
  addLog("info", "Request cleared", "API client fields were reset");
});

document.querySelector("#copyCurlBtn").addEventListener("click", async () => {
  if (!lastCurlCommand) {
    const url = buildRequestUrl(requestPath.value.trim() || "/", queryParamsInput.value);
    lastCurlCommand = buildCurl(requestMethod.value, url.href, buildHeaders(), bodyInput.value.trim());
  }
  await navigator.clipboard.writeText(lastCurlCommand);
  addLog("ok", "cURL copied", "Current request copied to clipboard");
});

document.querySelector("#clearHistoryBtn").addEventListener("click", () => {
  localStorage.removeItem(HISTORY_KEY);
  renderHistory();
  addLog("warn", "History cleared", "Saved request history was removed");
});

document.querySelectorAll("[data-response-tab]").forEach((button) => {
  button.addEventListener("click", () => {
    document.querySelectorAll("[data-response-tab]").forEach((tab) => tab.classList.remove("active"));
    document.querySelectorAll(".response-pane").forEach((pane) => pane.classList.add("hidden"));

    button.classList.add("active");
    document.querySelector(`#response${button.dataset.responseTab[0].toUpperCase()}${button.dataset.responseTab.slice(1)}`).classList.remove("hidden");
  });
});

document.querySelector("#resetProductBtn").addEventListener("click", () => {
  resetProductForm();
  addLog("info", "Product form reset", "Ready for a new product");
});

document.querySelector("#clearOutputBtn").addEventListener("click", () => show(""));

document.querySelector("#clearLogsBtn").addEventListener("click", () => {
  liveLogs.innerHTML = "";
  addLog("info", "Logs cleared", "New activity will appear here");
});

document.querySelector("#clearTokenBtn").addEventListener("click", () => {
  clearToken();
  addLog("warn", "JWT cleared", "Authenticated product requests will pause until login");
  loadProducts();
});

document.querySelector("#copyTokenBtn").addEventListener("click", async () => {
  await navigator.clipboard.writeText(getToken());
  show("JWT copied to clipboard.");
  addLog("ok", "JWT copied", "Token copied to clipboard");
});

tokenBox.addEventListener("input", () => {
  if (tokenBox.value.trim()) {
    localStorage.setItem(TOKEN_KEY, tokenBox.value.trim());
    addLog("warn", "JWT edited", "Token was manually updated from the text area");
  } else {
    localStorage.removeItem(TOKEN_KEY);
    addLog("warn", "JWT removed", "Token text area is empty");
  }
  syncTokenUi();
});

async function checkServerHealth({ log = false } = {}) {
  const startedAt = performance.now();

  try {
    const response = await fetch(`${baseUrl}/actuator/health`, { cache: "no-store" });
    const text = await response.text();
    const body = parseResponseBody(text) || {};
    const duration = Math.round(performance.now() - startedAt);
    const healthy = response.ok && body.status === "UP";

    setMetric(
      serverHealth.closest(".metric"),
      serverHealth,
      serverHealthMeta,
      healthy ? "ok" : "error",
      healthy ? "Up" : "Unhealthy",
      `Health returned ${response.status} in ${duration} ms`
    );

    serverState.className = `pill ${healthy ? "ok" : "error"}`;
    serverState.textContent = healthy ? "Server up" : "Server unhealthy";

    if (log) {
      addLog(healthy ? "ok" : "error", "Health check", `Server status is ${body.status || response.status}`);
    }
  } catch (error) {
    setMetric(serverHealth.closest(".metric"), serverHealth, serverHealthMeta, "error", "Down", "Health endpoint is not reachable");
    serverState.className = "pill error";
    serverState.textContent = "Server down";

    if (log) {
      addLog("error", "Health check failed", error.message || "Backend is not reachable");
    }
  }
}

syncTokenUi();
checkServerHealth({ log: true });
setInterval(checkServerHealth, 15000);
setInterval(syncTokenUi, 5000);
loadProducts().catch(() => {});
