# Changelog

## 2.0.0

### Bugs fixed (compared with the original BankingSystem-master)

- **The source did not compile** (4 errors: private fields accessed from `Bank` and `StudentAccount`,
  missing `InvalidAmount` import). Now builds cleanly with `-Xlint:all`.
- **"Add Student Account" created a Current account** (it called the wrong `addAccount` overload) and
  added it **twice**, once before the confirmation dialog. Now creates a real Student account, once.
- Student accounts could never be opened below 2,000 although the intended minimum was 100.
- The error text said "Minimum Limit 5000" for Savings and Student accounts. Messages are now generated
  from the real minimum of each type.
- Account numbers were random 5-digit values and could **collide**; now sequential and unique, never reused.
- The bank was limited to **100 accounts** (crash with `ArrayIndexOutOfBoundsException` on the 101st). No limit now.
- Typing letters in a number field threw an unhandled `NumberFormatException`; empty names passed validation
  (`getText()` is never `null`). All input is validated and shown as a clear message.
- Deposit of 0 and withdraw limit boundary (`<` instead of `<=`) were inconsistent. Rules are now exact and tested.
- Data was saved **only** when pressing Exit (a crash lost everything) and **all I/O errors were swallowed**: a
  read error silently started an empty bank that then overwrote the real data. Now saved after every
  operation, atomically, with a backup, and errors are reported.
- The account list window was not refreshed after deposit/withdraw (stale balances).
- The login ignored the username field (forced `admin`) and compared a plain-text password. Replaced by real
  login with hashed passwords.
- The main window could not be closed with the X button; the account windows used absolute positioning
  (`setLayout(null)`) and could not be resized. Replaced by proper layouts.
- `toString()` printed Java class names (`Type:class Bank.SavingsAccount`).
- Stale `.class` files inside `src/`, a `bin/` folder, IDE files, and a README that said `javac Main.java`
  (there is no `Main.java`) were removed.

### Added (features promised by the README / project report but missing in the code)

- Transfer between accounts (atomic: both sides change or neither)
- Transaction history and balance enquiry per account
- Search, edit account, delete account
- System look and feel, sortable table, status bar with totals
- Change-password dialog, login lock-out after 3 failed attempts

### Changed

- Java 21, Maven project layout (`src/main/java`, `src/test/java`), packages renamed to the Java convention
  (`com.bankingsystem.*`), 32 JUnit 5 tests.
- Money uses `BigDecimal` instead of `double`.
- Plain-text data file instead of Java object serialization (safer, readable, survives code changes).
- The three near-identical "Add ... Account" windows were merged into one `AccountDialog`, and the separate
  Deposit / Withdraw windows into one `TransactionDialog` (which also does transfers).
