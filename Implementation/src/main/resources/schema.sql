-- Registered users (guests, staff). Surrogate PK id, username unique for login.
CREATE TABLE IF NOT EXISTS Users (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    username TEXT NOT NULL UNIQUE,
    password TEXT NOT NULL,
    name TEXT NOT NULL,
    phone TEXT,
    email TEXT,
    role TEXT NOT NULL,
    employee_id INTEGER
);

-- Hotel rooms (used by Add Room UI and reservation room_number)
CREATE TABLE IF NOT EXISTS Rooms (
    room_number INTEGER PRIMARY KEY,
    smoking INTEGER NOT NULL,
    available INTEGER NOT NULL,
    max_daily_rate REAL NOT NULL,
    floor_type TEXT NOT NULL,
    bed_type TEXT NOT NULL
);

CREATE TABLE IF NOT EXISTS Reservations (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    confirmation_number TEXT NOT NULL UNIQUE,
    room_number INTEGER NOT NULL,
    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,
    total_guests INTEGER NOT NULL,
    created_date DATE NOT NULL,
    guest_id INTEGER REFERENCES Users(id),
    guest_name TEXT NOT NULL,
    card_number TEXT NOT NULL,
    is_active INTEGER NOT NULL DEFAULT 0,
    total_cost REAL NOT NULL
);

-- Store catalog
CREATE TABLE IF NOT EXISTS Products (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    sku TEXT NOT NULL UNIQUE,
    name TEXT NOT NULL,
    description TEXT,
    unit_price REAL NOT NULL,
    stock_qty INTEGER NOT NULL,
    active INTEGER NOT NULL
);

-- One active cart per guest (registered user)
CREATE TABLE IF NOT EXISTS Carts (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guest_id INTEGER NOT NULL UNIQUE REFERENCES Users(id),
    created_date DATE NOT NULL
);

CREATE TABLE IF NOT EXISTS CartItems (
    cart_id INTEGER NOT NULL REFERENCES Carts(id),
    product_id INTEGER NOT NULL REFERENCES Products(id),
    quantity INTEGER NOT NULL,
    PRIMARY KEY (cart_id, product_id)
);

-- Purchase header/lines (records what was bought; prices snapshot at purchase time)
CREATE TABLE IF NOT EXISTS Purchases (
    id INTEGER PRIMARY KEY AUTOINCREMENT,
    guest_id INTEGER NOT NULL REFERENCES Users(id),
    reservation_confirmation TEXT,
    purchased_at TEXT NOT NULL,
    subtotal REAL NOT NULL,
    tax REAL NOT NULL,
    total REAL NOT NULL
);

CREATE TABLE IF NOT EXISTS PurchaseItems (
    purchase_id INTEGER NOT NULL REFERENCES Purchases(id),
    product_id INTEGER NOT NULL REFERENCES Products(id),
    quantity INTEGER NOT NULL,
    unit_price_at_purchase REAL NOT NULL,
    PRIMARY KEY (purchase_id, product_id)
);
