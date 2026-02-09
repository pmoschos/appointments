# AppointmentManagement (Android)

Εφαρμογή Android για **διαχείριση ραντεβού** με παρόχους υπηρεσιών, με **δύο ρόλους** (Normal User / Service Provider). Υποστηρίζει πολλαπλές κατηγορίες υπηρεσιών (Υγεία, Ευεξία, Τεχνικές, Εκπαίδευση, Αυτοκίνητο), **κλείσιμο & διαχείριση ραντεβού**, ιστορικό με φίλτρα και **μηνιαία analytics**, καθώς και **διγλωσσία (EL/EN)**.  
Backend: **Firebase Realtime Database + Auth (Google Sign-In) + Storage**.

---

## Περιεχόμενα
- [Χαρακτηριστικά](#χαρακτηριστικά)
- [Ρόλοι Χρηστών](#ρόλοι-χρήστων)
- [Τεχνολογίες](#τεχνολογίες)
- [Αρχιτεκτονική](#αρχιτεκτονική)
- [Δομή Project](#δομή-project)
- [Firebase RTDB Schema](#firebase-rtdb-schema)
- [Κύριες Ροές (Flows)](#κύριες-ροές-flows)
- [Localization](#localization)
- [UX & Error Handling](#ux--error-handling)
- [Design Patterns](#design-patterns)
- [Οδηγός Χρήσης](#οδηγός-χρήσης)
- [Παραδοχές Υλοποίησης](#παραδοχές-υλοποίησης)

---

## Χαρακτηριστικά

### Για Normal Users
- Περιήγηση σε **κατηγορίες** υπηρεσιών (grid)
- Προβολή **παρόχων** ανά κατηγορία
- Προβολή **υπηρεσιών παρόχου** (δίγλωσση περιγραφή, διάρκεια, κόστος, εικόνα)
- **Κλείσιμο ραντεβού** με επιλογή:
  - ημερομηνίας (επόμενες 14 ημέρες)
  - διαθέσιμου time slot (grid)
  - προαιρετικών σημειώσεων (δίγλωσσες)
- Διαχείριση ραντεβού:
  - Edit (αλλαγή ημερομηνίας/ώρας)
  - Cancel / Delete (με αποδέσμευση slot)
- Προβολή ραντεβού:
  - Day / Week / Month (custom calendar)
- History:
  - λίστα ραντεβού (past & current)
  - φίλτρα (Time Period, Category, Status)
  - στατιστικές κάρτες (αριθμός, χρόνος, κόστος, ανάλυση ανά κατηγορία)
- Profile:
  - εμφάνιση & επεξεργασία στοιχείων
  - αλλαγή φωτογραφίας (Storage)
  - αλλαγή γλώσσας
  - Terms & Conditions
  - Logout

### Για Service Providers
- Dashboard με στατιστικές κάρτες (σήμερα/εκκρεμή/ολοκληρωμένα)
- CRUD υπηρεσιών (Add / Edit / Delete / Activate-Deactivate)
- Παραγωγή διαθέσιμων slots (availability)
- Διαχείριση ραντεβού:
  - Today / Upcoming tabs
  - αλλαγή status (Confirmed, Completed, Cancelled, No Show)
- Provider History με φίλτρα & στατιστικά

---

## Ρόλοι Χρηστών

- **Normal User**: Κλείνει & διαχειρίζεται ραντεβού, ιστορικό, προφίλ.
- **Service Provider**: Διαχειρίζεται υπηρεσίες, ραντεβού, status και availability slots.

> Ο ρόλος επιλέγεται **μία φορά στην πρώτη σύνδεση** και δεν αλλάζει.

---

## Τεχνολογίες

| Τεχνολογία | Χρήση |
|---|---|
| Android Studio | IDE |
| Kotlin | Γλώσσα προγραμματισμού |
| Firebase Realtime Database | Αποθήκευση δεδομένων realtime |
| Firebase Authentication | Google Sign-In |
| Firebase Storage | Φωτογραφίες προφίλ / εικόνες υπηρεσιών |
| Material Design 3 | UI |
| Glide | Φόρτωση εικόνων |
| ViewBinding | Ασφαλής πρόσβαση σε UI στοιχεία |
| KalendarView (custom) | Προσαρμοσμένο ημερολόγιο Month view |

---

## Αρχιτεκτονική

Layered architecture:

```
┌─────────────────────────────────────┐
│ UI Layer (Activities, Fragments,    │
│ Bottom Sheets)                      │
├─────────────────────────────────────┤
│ Adapters (RecyclerView)             │
├─────────────────────────────────────┤
│ Model DTOs (UI models)              │
├─────────────────────────────────────┤
│ Repository Layer                    │
│ (AppointmentRepository,             │
│  AvailabilityRepository,            │
│  UserRepository)                    │
├─────────────────────────────────────┤
│ DatabaseHelper (Singleton)          │
├─────────────────────────────────────┤
│ Firebase RTDB / Auth / Storage      │
└─────────────────────────────────────┘
```

---

## Δομή Project

```
app/src/main/java/com/ai/appointments/
├── activities/           # 15 Activities
├── adapters/             # 12 RecyclerView Adapters
├── bottomsheetdialouges/ # 5 Bottom Sheet Dialogs (6 κλάσεις)
├── calender/             # 4 αρχεία Custom Calendar Widget
├── db/
│   ├── models/           # 16 Data Classes & Enums
│   ├── Repository/       # 4 Repositories
│   └── utils/            # DatabaseHelper, RoleType
├── fragments/
│   ├── intro/            # 2 Onboarding Fragments
│   ├── provider/         # 3 Provider Fragments
│   └── user/             # 4 User Fragments
└── model/                # 9 UI DTOs
```

**Σύνολα**
- Kotlin files: **76**
- XML layouts: **43**
- Strings: **2** (`values/strings.xml`, `values-el/strings.xml`)

---

## Firebase RTDB Schema

```
firebase-root/
├── users/
│   ├── normal/{uid}/                → NormalUser
│   └── providers/{uid}/             → ServiceProvider
├── services/{serviceId}/            → Service
├── appointments/{appointmentId}/    → Appointment
├── availability/{providerId}/{date}/{time}/ → Slot data
├── availability_slots/{providerId}/{date}/  → Alternative slot structure
├── indexes/
│   ├── appointments_by_provider/{providerId}/{appointmentId} → timestamp
│   ├── appointments_by_user/{userId}/{appointmentId} → timestamp
│   └── services_by_provider/{providerId}/{serviceId} → true
└── analytics/
    └── user_monthly/{userId}/{month}/ → MonthlyAnalytics
```

### Indexes (Secondary Indexing)
Για αποδοτικότητα, η ανάκτηση γίνεται:
1) fetch IDs από `indexes/...`
2) fetch details από `appointments/{id}`
3) client-side filtering (date/status/category)

### Batch Updates (Atomic multi-path ops)
Χρησιμοποιείται `updateChildren()` για ατομική ενημέρωση πολλών paths (π.χ. delete):

```kotlin
val updates = mutableMapOf<String, Any?>()
updates["appointments/$appointmentId"] = null
updates["indexes/appointments_by_provider/$providerId/$appointmentId"] = null
updates["indexes/appointments_by_user/$userId/$appointmentId"] = null
database.reference.updateChildren(updates)
```

---

## Κύριες Ροές (Flows)

### Authentication (Google Sign-In)
1. `Sign in with Google`
2. Google picker
3. Firebase Auth με `idToken`
4. Έλεγχος αν υπάρχει χρήστης στη βάση
5. Αν νέος χρήστης → dialog επιλογής ρόλου
6. Δημιουργία εγγραφής:
   - `users/normal/{uid}` ή `users/providers/{uid}`
7. Redirect στο αντίστοιχο dashboard

### Booking Appointment (Normal User)
1. Category → Provider → Service
2. Επιλογή ημέρας (14-day horizontal calendar)
3. Φόρτωση διαθέσιμων slots από RTDB
4. Επιλογή slot + notes
5. Confirm screen (σύνοψη)
6. Confirm:
   - δημιουργία `Appointment`
   - δέσμευση slot (AvailabilityRepository.bookSlot)
   - ενημέρωση indexes
   - success bottom sheet

### Edit Appointment
- Bottom sheet επαναπρογραμματισμού:
  - αποδέσμευση παλιού slot
  - δέσμευση νέου slot
  - ενημέρωση appointment + indexes
  - confirmation toast

### Cancel Appointment
- Αλλάζει `status = cancelled`
- Καταγράφεται `cancelledBy` + `reason`
- Αποδέσμευση slot
- Firebase `runTransaction()` για αποφυγή race conditions

### Delete Appointment
- Διαγραφή appointment + indexes + slot release
- Multi-path `updateChildren()` (atomic)

### Provider: Status Management
Statuses:
- `Confirmed` (default)
- `Completed`
- `Cancelled`
- `No Show`

---

## Localization

### UI Γλώσσες
- **English (en)** default
- **Ελληνικά (el)**

Η επιλογή γίνεται στο `LanguageActivity` και αποθηκεύεται σε `SharedPreferences`.  
Το `BaseActivity` εφαρμόζει locale μέσω `attachBaseContext()`:

```kotlin
override fun attachBaseContext(newBase: Context?) {
    val prefs = newBase?.getSharedPreferences("settings", Context.MODE_PRIVATE)
    val language = prefs?.getString("language", "en") ?: "en"
    val locale = Locale(language)
    val config = Configuration(newBase?.resources?.configuration)
    config.setLocale(locale)
    super.attachBaseContext(newBase?.createConfigurationContext(config))
}
```

### Δίγλωσσα Δεδομένα στη Βάση
- service names: `name / name_el`
- descriptions: `description / description_el`
- notes: `notes_en / notes`
- provider names & business info: `providerName / providerName_en`, `businessName / businessName_en`, κ.λπ.

---

## UX & Error Handling

- Material components: `BottomNavigationView`, `MaterialAlertDialogBuilder`, `BottomSheetDialogFragment`
- Loading states (progress + disabled actions)
- Toast messages για success / error / validation
- Null safety με Kotlin (`?.`, `?:`, `let`)
- Firebase failures με `addOnFailureListener`
- Transactions για critical writes (cancel / slot handling)
- Graceful degradation (π.χ. αν αποτύχει slot booking, δεν μπλοκάρει απαραίτητα τη δημιουργία)

---

## Design Patterns

| Pattern | Χρήση |
|---|---|
| Singleton | `DatabaseHelper`, Repositories (`object`) |
| Repository | `BaseRepository` → `AppointmentRepository`, `AvailabilityRepository`, `UserRepository` |
| ViewBinding | Παντού (Activities/Fragments/Adapters/Bottom Sheets) |
| DiffUtil / ListAdapter | 7 από 12 adapters |
| Callback / Listener | BottomSheets → parent components |
| DTO | DB models vs UI DTOs |
| Builder | `MaterialAlertDialogBuilder` |
| Observer | Firebase listeners |

---

## Οδηγός Χρήσης

### Εκκίνηση
- Splash screen (2s) → έλεγχος login → redirect σε Dashboard ή Onboarding/Login

### Onboarding
- 2 screens με `Next/Skip/Start Now`

### Normal User Tabs
- **Home**: grid κατηγοριών
- **Appointment**: Day/Week/Month views
- **History**: λίστα + φίλτρα + στατιστικά
- **Profile**: στοιχεία, γλώσσα, terms, logout

### Provider Tabs
- **Home**: στατιστικά + υπηρεσίες + generate slots
- **Appointment**: Today/Upcoming + status update
- **History**: φίλτρα + στατιστικά

---

## Παραδοχές Υλοποίησης

- Working hours ανά ημέρα: `{day: {start, end}}` (π.χ. 09:00–17:00)
- Slots: διάρκεια **30’** + **15’ buffer**
- Μη επικαλυπτόμενα slots: κάθε slot κρατείται μία φορά
- Νόμισμα: **EUR**
- Ρόλοι: επιλογή στην πρώτη σύνδεση (δεν αλλάζει)
- Seed data: κατηγορίες/υπηρεσίες/πάροχοι είναι προ-καταχωρημένα στη RTDB

---

## Screenshots
> Πρόσθεσε screenshots εδώ για καλύτερη παρουσίαση:
- `docs/screenshots/`
- ή μέσα στο README με `![alt](path)`

---

## License
Ανέβασε ένα `LICENSE` αρχείο (π.χ. MIT) ή συμπλήρωσε εδώ το καθεστώς χρήσης.
