# KinCount — Android App Plan & Design

> A voice-first, lightweight personal expense & income tracker for Android — for one person or a whole family.
> Speak your transaction → GLM parses it → auto-categorized with date/time → stored on your phone, synced to **your own Google Sheet**. No backend to maintain.
> Version: 1.2 · Date: 2026-10-12 · Status: Ready for implementation — all assumptions confirmed, app renamed **KinCount** (formerly ExpenseManager)
> Changelog v1.2: **Firebase replaced by a user-owned Google Sheet** (admin's Drive) — Room local-first + sync engine, Google Sign-In with deferred consent (local-only until granted), family = one shared Sheet with app-level (trust-based) permissions; **LLM provider/model/API key are admin-managed** and sync to members via a Config tab; app renamed **KinCount**, z.ai default provider, mixed Bangla/English speech (locale default `bn-BD`), ৳ BDT confirmed. Changelog v1.1: family sharing with admin-granted permissions; explicit manual add/edit fallback when voice/GLM fails; instant delete with undo.

---

## 1. Vision & Goals

**One-sentence pitch:** Press the mic, say *"spent 250 taka on lunch yesterday"* — the app figures out it's an expense of 250, in **Food & Dining**, dated yesterday, and saves it. Use it solo, or as a family where the admin can let members (e.g. your wife) see everyone's entries. Your data lives on your phone and in **your own Google Sheet** — not on anyone's server.

| Goal | Target |
|---|---|
| Primary input | Voice (speech → GLM → structured transaction), with manual form as first-class fallback |
| Categorization | Automatic via GLM against existing categories; user can always correct |
| Manual control | Every entry can be **manually added, edited** (always, and especially when voice/GLM fails) and **deleted immediately** (one action, no dialogs, with undo) |
| Family use | Multiple accounts share **one Google Sheet owned by the admin**; admin grants "see everyone" per member (app-level); everyone else sees only their own data |
| Default view | Always **"My entries"** first; permitted users can toggle to **"Whole family"** |
| Daily view | All transactions for any day, with day totals |
| Dashboard | Monthly total expense, monthly total income, breakdown by category |
| Categories | ~20 sensible defaults pre-seeded; user can add/edit |
| Cost | ৳0 — Google Sheets/Drive API (free) + GLM Flash (free) |
| Data ownership | Data resides **user-side**: Room DB on device + a Google Sheet in the admin's own Drive; no Firebase, no server |
| Weight | Release APK < 12 MB, cold start < 1.5 s, no bloat (no ads, no analytics) |

### Non-goals (v1)
Budgets, recurring transactions, multi-currency accounts, bank sync, CSV/PDF export (JSON export only), home-screen widget, photos/receipts, family roles beyond admin/member, per-category permissions, chat/notifications between members, **server-enforced per-member privacy** (accepted trust model — see D13), **automatic admin transfer** (manual Drive handover until v2). (Several are easy v2 items.)

---

## 2. Key Decisions (veto points — read this section first)

| # | Decision | Choice | Why | Alternative rejected |
|---|---|---|---|---|
| D1 | Platform | **Native Android, Kotlin** | Lightest APK, best OS speech-API integration, single platform requested | Flutter (~2× APK size), React Native (heavier, JS bridge) |
| D2 | UI toolkit | **Jetpack Compose + Material 3** | Modern, less boilerplate, one declarative stack | XML Views (marginally smaller, much more code) |
| D3 | Cloud storage | **A user-owned Google Sheet in the admin's Drive, via Google Sheets API** | ৳0 forever, zero backend to maintain, data lives user-side; the Sheet doubles as a human-readable backup/archive you can pivot-table at year's end | Firestore (dropped v1.2: needs a Firebase project + rules upkeep; user prefers data on own side), Supabase, MongoDB Atlas |
| D4 | Local data | **Room is the source of truth; the Sheet is the sync/share/backup layer** | Sheets cannot serve interactive screen reads (latency ~1–3 s, read quotas); offline-first by construction — app is fully usable with sync off | Firestore offline cache (dropped with D3); Sheet-only storage (unusable UX) |
| D5 | Auth | **Google Sign-In (Credential Manager) + OAuth scopes `spreadsheets` and `drive.file` via `AuthorizationClient`** | Free, on-device; the same login provides Sheet access; each member's Gmail = their identity | Email/password (unneeded now — Google login is the auth), shared family login (no per-member attribution) |
| D6 | Speech→text | **Android `SpeechRecognizer`** | Free, built into the OS (Google speech service), zero APK cost | Whisper on-device (large model), cloud STT (paid) |
| D7 | Text→transaction | **GLM chat completions API via z.ai (confirmed default)**, default model **`glm-4.7-flash` (free)** | Free tier, good multilingual + structured-output support; bigmodel.cn stays as a Settings toggle | On-device NLU (weak for Banglish/free-form speech) |
| D8 | LLM config & API key | **Admin-managed in Settings** — the admin selects the LLM (provider + model) and pastes the key. On-device copy in `EncryptedSharedPreferences`; when the space has a Sheet, the config syncs via a **Config tab** (family members receive it read-only: "Managed by ⟨admin⟩"). Solo/local users are their own admin | One key, entered once — members never register for anything; zero backend to maintain | Per-member keys (dropped 2026-10-12: onboarding friction for family members), Cloud Function proxy (hides key but adds infra) |
| D9 | Numbers/currency | Single configurable currency, **default ৳ (BDT) — confirmed** | Single-currency app; guessing BDT from context — flag if wrong | Multi-currency (overkill) |
| D10 | Language | Speech locale configurable (**default `bn-BD`**, `en-US` toggle) — **mixed Bangla/English confirmed**; GLM prompt handles Bangla, English, and Banglish transcripts | Bangla-first locale transcribes code-mixed speech best (English words survive; en-US mangles Bangla); GLM's multilingual strength cleans up the rest | English-only parsing |
| D11 | Min SDK | **26 (Android 8.0, ~97% devices)** | Less compat baggage, modern APIs | 21–24 (tiny device gain, more shims) |
| D12 | Multi-user model | **A family = one shared spreadsheet.** Admin creates it; admin adds a member's Gmail (Drive permission, role Editor); member joins by pasting the sheet link. Solo = one personal Sheet (or no Sheet at all if consent denied) | One sheet = one code path for solo + family; Google Drive does the sharing plumbing | Firestore "spaces" (dropped with D3), per-member sheets + aggregation (messy) |
| D13 | Family permissions | **App-enforced only — explicit trust model (user-accepted 2026-10-12).** `canViewAll` filters rows in-app; Google sharing is file-level, so anyone with Editor access *can* open the Sheet in Google Sheets and read every row. In-app filtering is UX, not a security boundary | Zero backend; the only server-side privacy possible here is file-level, which Google provides; family = full trust | Server-enforced rules (impossible without a backend — that's the point of this architecture) |
| D14 | Editing/deleting others' entries | **Nobody edits or deletes another member's entry** — not even admin (v1). The app refuses to push changes to rows whose `owner` ≠ signed-in Gmail; admin only manages members | Privacy-preserving convention; keeps the log clean | Admin edit-all (scope creep) |
| D15 | Delete UX | **Immediate delete** — swipe or trash icon, no confirmation dialog; 5-second **Undo** snackbar (soft delete `deleted=true`) | "Delete immediately" as requested; undo protects against slips and works offline | Confirmation dialog (slower), hard delete (no undo possible) |
| D16 | Sync protocol | **Append-only log + last-write-wins (LWW).** Every create/edit/delete appends a row keyed by client-generated UUID + `updatedAt` (ISO-8601 UTC, fixed-width → lexicographically sortable); current state = newest row per UUID; pulls dedupe client-side; pushes are idempotent (a retried append's duplicate collapses on next pull) | Safe multi-device concurrency with zero row-index bookkeeping and no lost-update handling in the Sheet itself | In-place row updates (fragile indexes, racy writes) |
| D17 | Sheets client | **Hand-rolled thin REST client on OkHttp + kotlinx-serialization (~6 endpoints); `valueInputOption=RAW` on writes, `valueRenderOption=UNFORMATTED_VALUE` on reads** | Keeps APK < 12 MB (official `google-api-services-sheets` jar adds several MB + ProGuard pain); RAW/UNFORMATTED gives exact round-trips for numbers and ISO strings | google-api-client stack (heavy) |
| D18 | OAuth posture | **Consent screen: External → "In production" (unverified)**, with an in-app explainer shown before Google's "unverified app" warning | "Testing" mode expires refresh tokens every 7 days → weekly re-consent hell; full Google verification is weeks of process for a family app | Testing mode (token churn), verification (overkill) |

**Confirmed with the user (2026-10-12 — no open assumptions remain):**
1. Currency default ৳ BDT ✓
2. Speech is **mixed Bangla/English** → speech locale defaults to `bn-BD` (Settings toggle `en-US`); GLM prompt handles Bangla/English/Banglish ✓
3. GLM provider: **z.ai** (default endpoint; bigmodel.cn remains a Settings toggle) ✓
4. App name: **KinCount** — package `com.nahid.kincount` ✓
5. Family ≤ ~10 members, single admin; admin transfer = manual v2 ✓
6. Move-solo-entries-on-join; leaving keeps local entries and drops family sync ✓
7. Every family member has a Gmail ✓

---

## 3. Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                            Android App                              │
│                                                                     │
│  ┌──────────┐   transcript   ┌────────────┐   HTTPS+JSON            │
│  │  Mic UI  │ ─────────────► │  GLM       │ ─────────────►┐        │
│  │ (Speech- │                │  Parser     │ ◄─────────────┤        │
│  │ Recog-   │                │ (validate,  │  parsed JSON  ▼        │
│  │ nizer)   │                │  fallback)  │           GLM API      │
│  └──────────┘                └─────┬──────┘        glm-4.7-flash   │
│        │ voice/GLM fails →         ▼                  (free)        │
│        │ manual form (prefilled)   Confirm screen (edit/category    │
│        │                           fix; instant-delete later)      │
│        ▼                                 Save (local, instant)      │
│  ┌────────────────────────────────────────────┐                     │
│  │ TransactionRepository / CategoryRepository │                     │
│  │  · CRUD → Room DB ★ source of truth        │                     │
│  │  · view filter: MINE / FAMILY (app-level)  │                     │
│  └──────────────┬─────────────────────────────┘                     │
│                 │ Room Flow (rows marked PENDING)                   │
│  ┌──────────────▼───────────┐   push/pull    ┌────────────────┐    │
│  │ SyncEngine (+ WorkManager)│ ◄────────────► │ SheetsClient   │    │
│  │  · batch-append pending  │                │ thin REST      │    │
│  │  · pull → dedupe → LWW   │                │ OkHttp + kxs   │    │
│  │  · triggers: save-debounce│               └───────┬────────┘    │
│  │    / foreground / manual │                        │ HTTPS       │
│  └──────────────┬───────────┘                        ▼             │
│  ┌──────────────▼───────────────────────────┐        Google Sheets │
│  │ GoogleAuth                               │        API + Drive   │
│  │  · Credential Manager → identity (Gmail) │        API           │
│  │  · AuthorizationClient → scopes          │   (admin's Drive;    │
│  │    spreadsheets + drive.file             │    shared w/ family  │
│  │  · consent denied → LOCAL-ONLY mode ⚠    │    as Editors)      │
│  └──────────────────────────────────────────┘                      │
│                 │ StateFlow                                        │
│  ┌──────────────▼──────────────────────────────────────────────┐   │
│  │  UI — Jetpack Compose (single Activity)                     │   │
│  │ Dashboard │ Daily │ Add(Voice/Form) │ Family │ Categories │ Settings │
│  │  + sync-status icon: ⚠ local-only / ⟳ N pending / ☁ synced │   │
│  │  + Mine/Family filter chip everywhere it applies            │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

**Principles**
- **Offline-first, local-first:** every screen reads Room; writes land locally in milliseconds and are marked `PENDING`. Sync is a background courtesy — the app is 100% functional with sync off, denied, or failing. Only voice recognition, GLM parsing, and sync need network.
- **Consent is deferred and reversible:** no Google permission is demanded up front. Without consent the app runs local-only with a warning banner; tapping "Sync to Google Sheets" any time requests the scopes and back-fills everything then (exact flow requested by the user).
- **Manual fallback is a first-class path, not an error state:** if voice fails, GLM fails, or JSON is unparseable, the typed form opens **prefilled with the transcript and any regex-detected amount** — the user can always add or edit any entry by hand.
- **LLM never writes directly:** parsed output lands on a *confirmation screen*; the user taps Save. Wrong entries after saving are one swipe from gone.
- **Permissions are honest, not theatrical:** the app filters Mine/Family views and refuses to write others' rows, and the Family screen says plainly that members with Sheet access can open the Sheet directly and read all rows (D13). Google enforces file-level access; everything finer is convention.
- **No DI framework:** a tiny manual `AppContainer` in the `Application` class. No Hilt. KSP exists only for Room.

**Permissions (only 2):** `RECORD_AUDIO`, `INTERNET`. (Google auth flows through Play services — no extra manifest permissions.)

---

## 4. Data Model (Room + Google Sheet)

### 4.1 Local schema (Room — the source of truth)

```
transactions
{
  id:         UUID (client-generated)     // ★ stable across devices
  type:       "EXPENSE" | "INCOME"
  amount:     250.0                       // major units, always positive
  categoryId: "cat_food"
  note:       "Lunch"
  rawInput:   "spent 250 taka on lunch yesterday"
  owner:      "nahid@…"                   // ★ signed-in member's Gmail
  ownerName:  "Nahid"                     // denormalized for lists
  timestamp:  1696982400000               // ★ the transaction's date/time (epoch millis)
  createdAt:  …, updatedAt: …             // updatedAt drives LWW sync
  deleted:    false                       // soft delete → instant undo
  syncState:  PENDING | SYNCED            // push queue flag
}

categories
{ id: UUID, name, type: EXPENSE|INCOME, emoji, isDefault, sortOrder,
  createdBy, updatedAt, deleted, syncState }

members                        // cached from the Sheet's Members tab
{ email (PK), name, role: admin|member, canViewAll, joinedAt }

sync_prefs
{ signedInEmail, consentGranted, spreadsheetId, currentSpace: LOCAL|PERSONAL|FAMILY,
  lastSyncAt, sheetName }
```

### 4.2 Sheet schema (the sync/share/backup layer)

One spreadsheet per space, created by the app in the owning admin's Drive:
- Personal: **"KinCount"** — created on first consent (or reused — see discovery below)
- Family: **"KinCount — ⟨family name⟩"**

Four data tabs + one hidden `_meta` tab. Row 1 is a fixed header row; `_meta!A1` holds `schemaVersion`. On every pull the client fingerprints the header row — if a human renamed/moved columns, sync pauses with a repair prompt instead of corrupting data.

```
Tab "Transactions" (append-only log — one row per change, see §4.3)
id | updatedAt | deleted | owner | ownerName | type | amount | category | note | timestamp | rawInput
(UUID)  (ISO-8601 UTC, fixed-width, lexicographically sortable)          (ISO-8601)   (epoch millis as text)

Tab "Categories"
id | updatedAt | deleted | name | type | emoji | isDefault | sortOrder | createdBy

Tab "Members"
email | name | role | canViewAll | joinedAt | updatedAt

Tab "Config" (key–value; admin-written in family spaces, LWW like everything else)
key | value | updatedAt
llmProvider = z.ai | bigmodel.cn · llmModel = glm-4.7-flash · llmApiKey = <admin's key>

Tab "_meta" (hidden)
schemaVersion, header fingerprints
```

- Owner identity is the **Gmail address** — it is the same key used for Drive sharing, so no ID mapping is needed.
- Amounts written with `valueInputOption=RAW`, read with `valueRenderOption=UNFORMATTED_VALUE` → exact round-trip, no Sheets auto-coercion (D17).
- Family members are also real **Drive Editors** on the spreadsheet (that's how they get API access — D12). The Members tab records roles/permissions for the app; Drive permissions control access.
- In a family space **only the admin's app writes Config rows**; members pull them. This is the channel for the admin-managed LLM settings (D8) — the key is therefore readable by anyone with Sheet access, accepted under the same trust model (D13) and rotatable by the admin anytime.

### 4.3 Sync protocol (append-only log + LWW)

**Push** (SyncEngine, triggers: debounced after save/delete ~5 s · app foreground · periodic WorkManager ~15 min when PENDING rows exist · "Sync now" · pull-to-refresh):
1. Collect `syncState=PENDING` rows (transactions + categories separately).
2. One `values.append` per tab with the whole batch (2D array) — one API call for N changes.
3. On success mark rows `SYNCED`. On failure retry with backoff — safe because appends are idempotent under dedupe.

**Pull** (same triggers, after push):
1. `values.get` each tab (full-column read, skip header).
2. Parse tolerantly (wrong type in a cell, short row, empty row → skip that row, never crash).
3. Group by `id`, keep the row with the max `updatedAt` per id → that's the current state.
4. Upsert into Room when remote `updatedAt` > local (LWW). A retried push shows up as duplicate rows with identical `id`+`updatedAt` → collapses to one (idempotency).
5. Local PENDING rows that appear remotely with the same `updatedAt` are acknowledged → `SYNCED`.

**Conflicts:** two devices edit the same entry offline → both append → highest `updatedAt` wins (device clocks assumed roughly NTP-synced; skew of seconds is acceptable for a family ledger). The loser's version remains visible in the Sheet's own version history.

**Compact sheet** (Settings → admin-only): rewrites the Transactions tab with exactly one row per live (non-deleted or currently-deleted-head) id — clears years of append-log noise. Destructish but reconstructible; requires typing a confirmation.

**Sheet discovery:**
- Solo, new consent: list app-created spreadsheets via `drive.file` (`files.list` with the app as creator) → reuse "KinCount" if found, else create. (Reinstall/new-device recovery is automatic.)
- Family: the admin's app creates the sheet; members **paste the join link** (the spreadsheet URL) in Family → Join. The app extracts the ID, verifies read access, and checks the member's Gmail exists in the Members tab.

### 4.4 Trust boundary (what is enforced where — D13, read before shipping)

| Guarantee | Enforced by | Notes |
|---|---|---|
| Who can open/write the Sheet at all | **Google (Drive permissions)** | Real, server-side, file-level |
| Member sees only their own rows *in the app* | **App filtering** | UX only — not a security boundary |
| `canViewAll` grant/revoke | **App (Members tab)** | Takes effect on each member's next sync, not instantly |
| Nobody edits/deletes another's entry | **App convention (D14)** | App refuses to push rows with `owner` ≠ signed-in Gmail |
| A member reading the raw Sheet in Google Sheets | **Nothing — by design** | Accepted trade-off (2026-10-12): family = full trust |
| Admin's LLM key readable via the Config tab | **Nothing — by design** | Same trust model (D13); free-tier key, rotatable by admin in Settings (propagates on next sync) |

The Family screen displays a one-line honesty note: *"Members with Sheet access can open the Google Sheet directly and see all rows."*

### 4.5 Queries (all local SQL — this section got much simpler)
- **My entries (default):** `WHERE deleted=0 AND owner=:me ORDER BY timestamp DESC`.
- **Whole family** (offered when `canViewAll`): same without the owner filter.
- **Dashboard:** the filter's rows for the month → aggregate client-side into `MonthSummary` (totals, per-category, per-member in family view). Room does in milliseconds what Firestore needed composite indexes for.
- **Freshness:** data is as fresh as the last sync; pull-to-refresh on Daily/Dashboard triggers a sync then re-queries. Permission changes propagate on next sync (no live listeners — accepted).

### 4.6 Free-tier budget check (family of 5, heavy use)
5 members × ~40 transactions/day ≈ 200 appends/day + periodic pulls ≈ well under 1% of the Sheets API per-user limits (~60 writes/min, ~300 reads/min). Sheet storage: MBs vs 15 GB free Drive. Solo user: trivially fine. ✅

---

## 5. GLM Integration Design (the heart of the app)

### 5.1 Endpoint
- Base URL configurable in Settings:
  - z.ai (default, confirmed): `https://api.z.ai/api/paas/v4/chat/completions`
  - bigmodel.cn: `https://open.bigmodel.cn/api/paas/v4/chat/completions`
- Auth: `Authorization: Bearer <user key>` · `response_format: {"type": "json_object"}` · `temperature: 0.1`
- **The key is admin-managed (D8):** the admin pastes it once in Settings; in a family space it rides the Config tab to every member (Settings shows "Managed by ⟨admin⟩", read-only). A fresh member receives it in the very sync that verifies their membership, so voice parsing works immediately. Solo/local users manage their own.

### 5.2 Prompt contract

**System prompt (built at call time, categories injected):**

```
You parse personal-finance voice transcripts into transactions.
Current datetime: {now ISO-8601 with tz}. Currency: {BDT}.

Available EXPENSE categories: Food & Dining, Groceries, ... , Other
Available INCOME categories:  Salary, Business, ..., Other Income

Return STRICT JSON:
{
  "transactions": [
    {
      "type": "expense" | "income",
      "amount": <positive number>,
      "category": <exact name from the list> | null,
      "suggestedCategory": <string, only if no list match>,
      "note": "<short clean description, <= 5 words>",
      "datetime": "<ISO-8601 resolved from relative words like
                   'yesterday/last friday'; null means now>"
    }
  ]
}

Rules:
- One transaction per purchase mentioned; return 1..N items.
- Words for numbers ("two hundred fifty") → numerals.
- Transcript may be Bangla, English, or mixed (Banglish); "টাকা", "taka", "৳" all denote the currency; numbers may be spoken as Bangla or English words.
- Ambiguous side (e.g. "paid salary") → infer from category list semantics.
- No amount detectable → omit that item; never invent amounts.
```

**User message:** the raw transcript. The parsed transaction's `owner` is always the **signed-in Gmail**, set by the client — the GLM has no say in ownership.

### 5.3 Response handling pipeline

```
transcript ─► GLM call ─► JSON schema validate
                             ├─ valid   ─► category name-match (case-insensitive)
                             │             ├─ matched    ─► Confirm screen
                             │             └─ suggested ─► Confirm screen w/ "create '<x>'?" chip
                             ├─ invalid JSON ─► 1 retry ("replay as valid JSON only")
                             └─ retry fails / timeout / no network
                                          ─► MANUAL FORM, prefilled:
                                             transcript in the note field,
                                             regex-extracted amount as a hint
```

- The manual form is the same screen used for adding/editing by hand — **identical capability, no dead ends**: any field editable, category dropdown, date/time picker, expense/income switch.
- `note` from GLM is a *suggestion*; everything on the confirm screen is editable (amount stepper, category dropdown, date/time picker).
- Timeout 10 s, one retry; errors surface as a subtle banner, never a crash.

### 5.4 Cost
Free Flash models (`glm-4.7-flash` / `glm-4.5-flash` on z.ai; `glm-4-flash` on bigmodel.cn) — ¥0/$0 per token. A ~600-token round trip × 30 calls/day/member is comfortably inside free rate limits. One shared admin key = one shared rate limit — a family of five stays far under it; an occasional 429 retries once after a short backoff. Only the admin can switch to a paid model (`glm-4.6`) — it bills *their* key.

---

## 6. Screens & UX (single Activity, bottom nav: 3 tabs + toolbar screens)

### 6.1 Dashboard (default tab) — monthly summary
```
┌───────────────────────────────┐
│  ‹  October 2026  ›      ☁⟳⚠ │  month switcher + sync-status icon
│  [👤 Mine] [👨‍👩‍👧 Family]       │  ★ filter chip — shown only if the user
│                               │    has view-all (admin or granted); "Mine" default
│  ┌───────────┬───────────┐    │
│  │ Expenses  │ Income    │    │
│  │ ৳ 12,450  │ ৳ 45,000  │    │
│  └───────────┴───────────┘    │
│  Net: + ৳ 32,550              │
│  ───────────────────────────  │
│  By category (expense):       │
│  🍔 Food & Dining   ৳3,200 ████░░ 26%
│  🚌 Transport       ৳1,800 ██░░░░ 14%
│  🛒 Groceries       ৳1,500 ██░░░░ 12%
│  ───────────────────────────  │
│  By member (family view only):│
│  😐 Nahid ৳9,200 · 😊 Mim ৳3,250│
└───────────────────────────────┘
```
- When local-only (no consent), a banner sits atop the list: *"Data stays on this phone. **[Sync to Google Sheets]** [×]"* — tapping runs the consent flow (§6.6).
- Pull-to-refresh = sync now + re-query.

### 6.2 Daily — the day ledger
```
┌───────────────────────────────┐
│  Sat, Oct 11      ‹ today ›   │  date arrows + picker
│  [👤 Mine] [👨‍👩‍👧 Family]       │  same filter, same state everywhere
│  Spent ৳ 840 · Earned ৳ 0     │
│  ───────────────────────────  │
│  🍔 Food & Dining             │
│  │    Lunch (Nahid)       −250  │  ← owner name shown in family view
│  🚌 Transport                 │
│     Bus fare (Mim)       −30  │
│  ───────────────────────────  │
│  swipe LEFT any row  ⟶  DELETED immediately
│  └─ snackbar: "Deleted ✓  [Undo]" (5 s, restores instantly)
└───────────────────────────────┘
```
- **Delete = immediate.** Swipe left or trash icon → entry is gone from the list at once; no confirmation dialog. Undo snackbar for safety (soft delete `deleted=true`, restored by setting it back). Tap a row → edit screen (full manual editing of any field).

### 6.3 Add — voice-first with mic FAB
```
 step 1: listening        step 2: confirm (the safety net)
┌───────────────────────┐ ┌───────────────────────────┐
│        🎤 ▂▄▆▂▄       │ │ Parsed 1 transaction:    │
│  "spent 250 on lunch  │ │  Expense  ৳250           │
│   yesterday…"         │ │  🍔 Food & Dining   [▼]  │
│                       │ │  Note: Lunch        [✎]  │
│  [stop]   [keyboard]  │ │  Fri, Oct 10, 1:30pm [✎] │
└───────────────────────┘ └───────────────────────────┘
  always-available          │      [ Save ]  [ Cancel ]│
  typed-input fallback      └───────────────────────────┘
```
- Mic FAB is centered on the bottom bar (thumb reach) and also on Dashboard.
- **Voice fails / GLM fails / offline → manual form opens automatically**, prefilled with the transcript and any detected amount — the entry is always completable by hand (manual is a first-class path). Saving is instant (local) regardless of sync state.
- Multiple transactions in one utterance → confirm screen lists all, each editable.
- Unmatched category → chip: *"Create category 'Pet Care'?"*

### 6.4 Family (from Dashboard toolbar)
```
┌───────────────────────────────┐
│  Nahid's Family               │
│  Sheet: "KinCount — Nahid's   │
│  Family"  [open ↗]            │  ← opens the actual Google Sheet
│  ───────────────────────────  │
│  😐 Nahid (you)   admin       │
│     sees all entries          │
│  😊 Mim          member       │
│     [ Can see everyone's      │
│       entries  ●——○ ]         │  ← admin's grant/revoke (app-level)
│     [ Remove from family ]    │  ← also revokes Drive access
│  ───────────────────────────  │
│  Add member (needs internet): │
│  [ wife@gmail.com        ] ➕ │  → grants Editor on the Sheet
│  Then send them the link:     │
│  [📋 copy]  [▶ share]         │
│  ───────────────────────────  │
│  [ Compact sheet ]            │  admin-only maintenance (§4.3)
│  [ Dissolve family ]          │  types family name to confirm
│  ───── or (solo user) ─────   │
│  [ Create a family ]          │  creates the family Sheet
│  [ Join with a link… ]        │  paste the sheet link from admin
└───────────────────────────────┘
  ⓘ Members with Sheet access can open the Google Sheet
    directly and see all rows.
```
- **Add member = two steps for the admin:** enter the member's Gmail (app calls Drive `permissions.create`, role=writer, then appends the Members row) and share the join link with them by any channel (copy/WhatsApp). Member taps/pastes the link in *Join* → app verifies access + finds their Gmail in Members → joined, `canViewAll=false` → sees **only their own** data.
- New member joining with existing solo entries → prompt: *"Move your 132 entries into the family? [Move] [Keep separate]"* (their PENDING rows simply push to the family Sheet with `owner`=them).
- Removed/leaving → the app detects the lost Drive permission on next sync (403), switches to personal/local mode, **keeps everything on the phone** (it was always local), and offers Export JSON. Leaving is one tap in Settings.
- Dissolve = admin deletes the Sheet (`files.delete`) — destructive, requires typing the family name (destroys the shared copy, not phone-local copies).

### 6.5 Categories & Settings (from Dashboard toolbar)
- **Categories:** shared for the space, grouped list (Expense/Income), any member adds (name + emoji + type); admin (or creator) renames/hides. Deleting a used category is blocked (history integrity). All changes ride the same sync.
- **Settings:** Google account + consent state (**Enable/Disable cloud sync**) · **Sync now** · **LLM section (admin-managed, D8):** provider (z.ai / bigmodel.cn) · model · API key (masked, **Test connection**) — editable for solo users and the family admin; members see it read-only as "Managed by ⟨admin⟩" · currency · speech locale · **Compact sheet** (admin) · **Export JSON** · sign out.

### 6.6 Sync & consent UX (the local-first safety net)
- **Sync-status icon** (toolbar, everywhere): **⚠** local-only — tap → consent flow; **⟳ N** — N changes pending; **☁** — synced (tap → last-sync time, Sync now, Open Sheet).
- **Consent flow:** in-app explainer first — *"Google will say this app is unverified — it's your own app; tap Advanced → Go to KinCount."* Then `AuthorizationClient.authorize(scopes)`. On grant: create/reuse the Sheet → full catch-up push. On deny: back to local-only banner, no nagging.
- Consent can be revoked in Google settings at any time; the app detects auth failures and drops to local-only mode with the banner — data is never at risk (Room first, always).

---

## 7. Default Categories (seeded locally into every install)

| Expense | | Income | |
|---|---|---|---|
| Food & Dining | 🍔 | Salary | 💼 |
| Groceries | 🍒 | Business | 🏪 |
| Transport | 🚌 | Freelance | 💻 |
| Fuel | ⛽ | Investment | 📈 |
| Rent & Home | 🏠 | Gift Received | 🎁 |
| Utility Bills | 💡 | Refund | ↩️ |
| Mobile & Internet | 📱 | Bonus | 🏆 |
| Health | 💊 | Other Income | 💰 |
| Education | 📚 | | |
| Clothing | 👕 | | |
| Entertainment | 🎬 | | |
| Personal Care | 🧴 | | |
| Shopping | 🛍️ | | |
| Travel | ✈️ | | |
| Gifts & Donations | 🎁 | | |
| Family | 👨‍👩‍👧 | | |
| Other | ❓ | | |

---

## 8. Tech Stack & Dependencies (deliberately short)

| Layer | Choice |
|---|---|
| Language / build | Kotlin 2.x, Gradle KTS, R8 minify + resource shrink |
| UI | Jetpack Compose (BOM), Material 3, Navigation-Compose, ViewModel + StateFlow |
| Local data | **Room (via KSP)** — entities, DAOs, source of truth |
| Sync | **WorkManager** — periodic + debounced one-shot sync jobs |
| Google auth | **Credential Manager (`androidx.credentials` + `googleid`)** for identity; **`play-services-auth` `AuthorizationClient`** for scope grants (tokens live in Google Play services, not app storage) |
| Sheets client | **Hand-rolled thin REST on OkHttp + kotlinx-serialization** — ~6 endpoints: `spreadsheets.create`, `values.get`, `values.append`, `drive.permissions.create/delete`, `files.list/delete` (D17) |
| Network | OkHttp + kotlinx-serialization-json (shared by GLM + Sheets clients) |
| Secrets | androidx.security:security-crypto (GLM key only) |
| Speech | Android framework `SpeechRecognizer` (zero deps) |
| **Not included** | Hilt, Coil, Retrofit, **google-api-client / google-api-services-sheets jars**, Firebase (gone entirely), Crashlytics, material-icons-extended (core icons only — saves MBs) |

Project structure:
```
app/src/main/java/com/nahid/kincount/
├── KinCountApp.kt              // Application + AppContainer (manual DI)
├── MainActivity.kt             // single activity, NavHost
├── data/
│   ├── model/                  // Transaction, Category, MonthSummary, ParsedTx, Member, SyncState
│   ├── local/                  // Room: AppDatabase, DAOs, entities
│   ├── sheets/                 // SheetsApi (interface), SheetsClient (REST impl), SheetModels
│   ├── sync/                   // SyncEngine (push/pull/dedupe/LWW/ack), SyncWorker, SyncStatus
│   ├── auth/                   // GoogleAuth (Credential Manager + AuthorizationClient), ConsentState
│   ├── repo/                   // TransactionRepository (owner filter), CategoryRepository,
│   │                           // FamilyRepository (add member/join/grant/revoke/remove)
│   ├── glm/                    // GlmClient, GlmParser (prompt + validation + fallback)
│   └── prefs/                  // SettingsStore (encrypted), SyncPrefs, ViewFilterStore
├── speech/                     // SpeechRecognizerHelper (RecognizingListener)
├── ui/
│   ├── dashboard/  ├── daily/  ├── add/  ├── family/  ├── categories/  ├── settings/
│   ├── sync/                  // status icon + local-only banner + consent explainer
│   └── theme/
└── util/                       // Money format, DateTime helpers, AmountRegex
```

---

## 9. Milestones

| # | Milestone | Contents | Done when |
|---|---|---|---|
| M0 | Skeleton | `git init`, Gradle project, Compose app w/ nav + theme, manual DI | App builds & runs showing empty tabs |
| M1 | Data core (solo, local-only) | Room schema, repositories, category seeding, **manual add/edit/delete** form (delete = swipe + undo), Daily list + day totals | Transaction saved to Room & listed in Daily; survives restart & airplane mode; swipe-deleted instantly with Undo |
| M2 | Google sync | Credential Manager sign-in, AuthorizationClient consent (+ explainer), SheetsClient (create/reuse/list), **SyncEngine** (append log, pull-dedupe-LWW, ack), WorkManager triggers, sync-status icon + local-only banner | Entry saved on phone A appears in the Google Sheet; reinstall/phone B signs in → data pulls back; deny consent → banner + local-only works; airplane add → online → synced |
| M3 | Voice + GLM | SpeechRecognizerHelper, GlmClient + parser + fallback, mic FAB, confirm screen, multi-item support; **manual form auto-opens on any voice/GLM failure, prefilled** | Saying "spent 250 on lunch" → saved correctly; airplane-mode voice → prefilled manual path, still savable |
| M4 | Dashboard | Month switcher, totals, per-category bars, net, Mine/Family-aware aggregation | Matches hand-computed month totals |
| M5 | Categories + Settings | Category CRUD (synced), **LLM config: provider/model/key entry + Test connection, Config-tab sync, read-only member view (D8)**, currency/locale config, Sync now, Export JSON | New category usable in parse prompt immediately; admin's key change reaches a member's device on next sync |
| M6 | **Family & permissions** | Create family Sheet, add member by Gmail (Drive permission) + join-link flow, member Join-by-link, member list, grant/revoke view-all (app-level), Mine/Family filter chip on Daily + Dashboard, per-member dashboard section, move-entries-on-join, remove/leave with graceful 403 fallback, dissolve, compact sheet | Wife joins via link → sees only her rows in-app; admin grants → Family toggle appears for her after sync; revoke → snaps back on next sync; removed member's app falls back to local mode keeping data |
| M7 | Hardening & release | R8 shrink, icon/splash, unit + sync-engine tests green, signed release APK | APK < 12 MB, installed & dogfooded by 2 family members |

Estimated total: ~9 focused implementation sessions.

---

## 10. Testing Strategy

- **Unit (JVM):** GLM JSON parser (valid/invalid/missing fields, number words), amount regex fallback, month aggregation math, category name-matching, join-link ID extraction. → `GlmParserTest`, `MonthSummaryTest`
- **Sync engine (JVM, against a fake `SheetsApi`):** LWW conflict resolution; duplicate rows from a retried push collapse to one; malformed/short/foreign rows skipped without crash; header-fingerprint mismatch pauses sync; ack marks PENDING rows SYNCED; 403 → space falls back to local mode. → `SyncEngineTest`
- **Instrumented:** Room DAO round-trips (entities ↔ Sheet row models), SpeechRecognizerHelper state machine, move-entries-on-join copy, end-to-end sync against a throwaway test spreadsheet (manual, not CI).
- **Manual script (per release):** deny consent → banner → grant → catch-up sync; two devices edit the same entry offline → LWW merge on sync; member removed → graceful fallback + export; compact sheet then verify second device still pulls correctly; airplane-mode add → online sync; misparse → edit → save; kill app mid-sync → no partial state (Room transactional upserts).

---

## 11. Risks & Mitigations

| Risk | Impact | Mitigation |
|---|---|---|
| GLM misparse (wrong category/amount) | Wrong data | Confirm screen before any write; everything editable; manual edit anytime; `rawInput` kept for audit |
| Voice/GLM unavailable (offline, no speech service) | Blocked entry | Manual form always available and auto-offered, prefilled; keyboard-first UX on devices without speech service |
| Accidental delete | Lost entry | Soft delete + 5 s Undo snackbar (works offline too — it's a local write) |
| **Family privacy is app-level only (D13)** | Member reads others' rows via the raw Sheet | Accepted trade-off (2026-10-12); in-app honesty note on Family screen; Drive remove cuts all access instantly |
| Sheet damaged by hand (renamed headers, deleted rows) | Sync corruption | Header fingerprint check pauses sync with repair prompt; tolerant row parser; Google Sheets version history can restore; Compact rebuilds canonical state |
| Consent revoked / token problems | Sync stops | Room-first means zero data risk; app detects auth failure → local-only banner → re-consent flow; consent screen in production mode avoids 7-day token expiry (D18) |
| "Unverified app" warning scares family members | Failed onboarding | In-app explainer *before* Google's screen with exact tap path (Advanced → Go to KinCount) |
| Device clock skew across family phones | Odd LWW outcomes | Devices auto-sync clocks; skew of seconds acceptable for a family ledger; Sheet history preserves the loser |
| Sheets API quota hit (burst syncing) | Sync delayed, never data loss | Batched appends (one call per tab), WorkManager exponential backoff; local writes never blocked |
| Member leaves/removal | Data access | Data was always local to each phone (Room); JSON export anytime; family keeps the shared Sheet |
| Member's phone lost | Their unsynced entries | Periodic background sync minimizes window; Sheet is the backup |
| Sheets/Drive API or pricing shifts | Rework | APIs are stable & free for this scale; the Sheet itself is the export format; JSON export as escape hatch |
| Admin's API key leaks (readable by members via the Config tab, or extracted from any device) | Key abuse | Accepted under the family trust model (D13); it's a free-tier key the admin can rotate in Settings anytime (propagates to members on next sync); encrypted on-device storage |

---

## 12. Setup Prerequisites (user actions, needed at M2)

1. **Google Cloud (replaces Firebase setup entirely):** create a free project at [console.cloud.google.com](https://console.cloud.google.com) → enable **Google Sheets API** and **Google Drive API** → OAuth consent screen: External, add the two scopes (`spreadsheets`, `drive.file`), **publish to In production** (unverified is fine for family use — D18) → create an **OAuth client ID (Android)** with package `com.nahid.kincount` + SHA-1 of debug **and** release keystores → paste the client ID into app build config. No `google-services.json` anymore.
   Every family member needs a Google account; the family Sheet lives in the admin's Drive (15 GB free ≈ a lifetime of rows).
2. **GLM (admin only):** register at [z.ai](https://z.ai) (default; [open.bigmodel.cn](https://open.bigmodel.cn) optional) → create API key → paste into app Settings — it propagates to members via the Config tab (no need before M3).
3. **Android Studio** (any recent version) with SDK 35 for building; a physical phone per family member is ideal for realistic mic testing.

---

*End of plan v1.2. Backend decisions (D3–D5, D12–D13, D16–D18) approved and all assumptions confirmed 2026-10-12 — implementation starts at M0.*
