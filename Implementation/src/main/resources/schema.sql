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
    masked_card_number TEXT NOT NULL,
    total_cost REAL NOT NULL
);
