# Stay & Shop — implementation overview

This folder is a **Java SE** desktop app: a **hotel stay** side (search rooms, hold availability, reserve) and a **store** side (catalog, cart, checkout, combined bill with stay charges), with **guest**, **clerk**, and **admin** experiences in Swing. Everything durable runs through **SQLite** under `Implementation/`.

The sections below describe **what we built**, **how the pieces talk to each other**, and **why** we split responsibilities the way we did.

---

## What the program does

- **Guests** can create an account, sign in, search availability, complete a reservation flow, browse products, manage a cart, purchase while on an active stay, and open a **combined bill** that adds up their reservations and their store purchases.
- **Clerks** get a separate shell for staff workflows (reservations, room entry, and related screens wired in `MasterUI` / `ClerkUI` / `ClerkReservationsUI`).
- **Admins** get an admin shell to provision staff (e.g. `AddCA_UI` via `UserController`); users and roles are modeled as subclasses of `User` and loaded or saved through `UserService` and `SqliteUserPersistence`.

Room data, reservation rows, users, products, carts, and purchase headers/lines all live in one database file so behavior survives restarts.

---

## How it is wired together

**Composition root — `Driver.Driver`**  
`main` chooses the DB path (`data/database.db` next to the working directory), constructs one instance each of:

- `RoomService`, `ReservationService`, `UserService` (each opens the same SQLite file and applies schema where needed),
- `SqliteStorePersistence` + `SqliteReservationPersistence` for the store and reservation tables,
- `ShoppingService` (store + reservation persistence + tax rate),
- controllers: `UserController`, `SearchController`, `ReservationController`, `ShoppingController`,
- a single shared `UserSession` (current user + optional “pending reservation” before login),
- `MasterUI`, which receives all controllers and the session and builds the frame and `CardLayout` shells.

That keeps **object construction and dependency hookup in one place** instead of scattering `new` across Swing panels.

**Request path (typical)**  
Swing components in `UI/` call methods on `Controllers/*`. Controllers are thin: they forward to `Domain/Services/*` and pass `UserSession` when the operation depends on who is signed in. Services enforce rules (dates, overlap, role checks via `UserSession.requireLoggedInGuest()` and friends), compose domain objects, and call `TechnicalServices/Persistence/Sqlite*Persistence` for JDBC. **No screen talks to JDBC directly.**

**Session and navigation**  
`UserSession` holds the logged-in `User` (polymorphic: `Guest`, `Clerk`, `Admin`) and can stash a `PendingReservation` so a flow that started on the public shell can resume on the guest shell after login. `MasterUI` reads that state and switches cards (`PublicUI`, guest shell, clerk shell, admin shell) and refreshes child panels when the user changes.

**Shopping vs reservations**  
`ShoppingService` depends on both `SqliteStorePersistence` and `SqliteReservationPersistence`: checkout needs the store for cart/stock/purchase rows, and the reservation layer to decide whether the guest has an **active stay today** before allowing a purchase. `ShoppingController` also receives `ReservationService` so `buildCombinedBill` can pull that guest’s reservations and merge totals with purchases in one `CombinedBill` object for `CombinedBillUI`.

---

## Persistence and the combined bill

Schema lives in `Implementation/src/main/resources/schema.sql` and is applied through `SchemaInstaller` when connections are opened.

- **Reservations** store room charges as written at confirm time; we do **not** rewrite reservation totals when someone buys from the store.
- **Purchases** are their own rows (`Purchases`, `PurchaseItems`) keyed by **guest user id**, optionally recording which reservation confirmation was active at checkout for traceability.
- **Combined bill** is therefore a **read-time aggregation**: `ShoppingService.buildCombinedBill` sums reservation costs and purchase totals for the logged-in guest and wraps them in `Domain.Shared.CombinedBill` for the UI. That keeps two bounded contexts (stay ledger vs store ledger) from corrupting each other while still presenting one number to the guest.

Carts (`Carts`, `CartItems`) are also persisted so a cart survives process restarts until checkout clears it.

---

## Why we structured it this way (GRASP, in our terms)

- **Controller** — `SearchController`, `ReservationController`, `ShoppingController`, and `UserController` exist so the UI has a stable, coarse-grained API (“search”, “confirm reservation”, “purchase”) instead of every panel reaching into services and persistence.
- **Information expert** — overlap and reservation lifecycle rules sit in `ReservationService` and reservation persistence; money and stock rules for checkout sit in `ShoppingService` + `SqliteStorePersistence.purchaseItems` inside a transaction.
- **Low coupling / high cohesion** — Swing stays in `UI/`; SQL stays in `TechnicalServices/Persistence/`; the middle is domain language (`Reservation`, `Cart`, `Purchase`, services). Changing a screen layout should not force a schema change, and vice versa.
- **Pure fabrication** — SQLite adapters are invented objects whose job is persistence, not “real world” hotel concepts, so domain types do not carry JDBC.
- **Protected variations** — `PasswordHasher` centralizes how stored passwords are verified and upgraded; `SchemaInstaller` centralizes how we bootstrap tables from the resource file.

Polymorphism on `User` lets `LoginUI` branch on role without a giant string switch on job titles, and lets services ask `UserSession` for a `Guest` when a use case is guest-only.

---

## Build and run

Use **Maven** from the `Implementation` directory (the one that contains `pom.xml`):

```bash
mvn
```

The POM default goal compiles and runs `Driver.Driver` via the exec plugin. Use a JDK compatible with the `<release>` value in `pom.xml`.

To wipe local state, delete `data/database.db` (or the whole `data` directory next to the working directory) and start again.

---

## Where to look in the tree

| Area | Path under `Implementation/` |
|------|--------------------------------|
| Swing UI | `src/main/java/UI/` |
| Controllers | `src/main/java/Controllers/` |
| Domain model + services | `src/main/java/Domain/` |
| SQLite + security helpers | `src/main/java/TechnicalServices/` |
| `main` / wiring | `src/main/java/Driver/Driver.java` |
| DDL | `src/main/resources/schema.sql` |
| Runnable use-case style mains | `src/main/java/Testing/` |

`Driver` also seeds a demo guest user if that username is not present, so a fresh database is usable immediately for guest flows; adjust or remove that seed for your own deployment.
