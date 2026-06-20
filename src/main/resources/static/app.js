const TOKEN_KEY = "backendApiJwt";
const baseUrl = window.location.origin;

const output = document.querySelector("#output");
const tokenBox = document.querySelector("#tokenBox");
const authState = document.querySelector("#authState");
const productsEl = document.querySelector("#products");
const productForm = document.querySelector("#productForm");

document.querySelector("#baseUrl").textContent = `Base URL: ${baseUrl}`;

function uniqueEmail() {
  return `vijay.${Date.now()}@example.com`;
}

document.querySelector("#registerForm [name=email]").value = uniqueEmail();
document.querySelector("#loginForm [name=email]").value = localStorage.getItem("backendApiEmail") || "";
document.querySelector("#productForm [name=sku]").value = `SKU-${Date.now()}`;

function getToken() {
  return localStorage.getItem(TOKEN_KEY) || "";
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
  tokenBox.value = token;
  authState.textContent = token ? "JWT saved" : "Not logged in";
}

function show(data) {
  output.textContent = typeof data === "string" ? data : JSON.stringify(data, null, 2);
}

async function api(path, options = {}) {
  const headers = { "Content-Type": "application/json", ...(options.headers || {}) };
  const token = getToken();
  if (token) headers.Authorization = `Bearer ${token}`;

  const response = await fetch(`${baseUrl}${path}`, { ...options, headers });
  const text = await response.text();
  const body = parseResponseBody(text);

  if (!response.ok) {
    const error = body || { status: response.status, message: response.statusText || text };
    show(error);
    throw error;
  }

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
  try {
    const body = formJson(event.currentTarget);
    const response = await api("/api/v1/auth/register", {
      method: "POST",
      body: JSON.stringify(body)
    });
    setToken(response.token, body.email);
    document.querySelector("#loginForm [name=email]").value = body.email;
    await loadProducts();
  } catch {
    // api() already renders the response.
  }
});

document.querySelector("#loginForm").addEventListener("submit", async (event) => {
  event.preventDefault();
  try {
    const body = formJson(event.currentTarget);
    const response = await api("/api/v1/auth/login", {
      method: "POST",
      body: JSON.stringify(body)
    });
    setToken(response.token, body.email);
    await loadProducts();
  } catch {
    // api() already renders the response.
  }
});

productForm.addEventListener("submit", async (event) => {
  event.preventDefault();
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

    resetProductForm();
    await loadProducts();
  } catch {
    // api() already renders the response.
  }
});

async function loadProducts() {
  if (!getToken()) {
    productsEl.innerHTML = '<p class="empty">Login or register first.</p>';
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
        <span>${escapeHtml(product.sku)} · ${product.quantity} pcs · ${product.price}</span>
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
    });
  });

  productsEl.querySelectorAll("[data-delete]").forEach((button) => {
    button.addEventListener("click", async () => {
      await api(`/api/v1/products/${button.dataset.delete}`, { method: "DELETE" });
      await loadProducts();
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

document.querySelector("#refreshProductsBtn").addEventListener("click", loadProducts);
document.querySelector("#resetProductBtn").addEventListener("click", resetProductForm);
document.querySelector("#clearOutputBtn").addEventListener("click", () => show(""));
document.querySelector("#clearTokenBtn").addEventListener("click", () => {
  clearToken();
  loadProducts();
});
document.querySelector("#copyTokenBtn").addEventListener("click", async () => {
  await navigator.clipboard.writeText(getToken());
  show("JWT copied to clipboard.");
});
tokenBox.addEventListener("input", () => {
  if (tokenBox.value.trim()) {
    localStorage.setItem(TOKEN_KEY, tokenBox.value.trim());
  } else {
    localStorage.removeItem(TOKEN_KEY);
  }
  syncTokenUi();
});

syncTokenUi();
loadProducts().catch(() => {});
