# CRM Lead API Specification (Frontend)

> Version date: 2026-03-24
> 
> Scope: endpoints and behavior for Lead flows in current backend implementation.

---

## 1) Endpoints by feature

## Creation

### `POST /dispatcher/leads`
- Auth: `DISPATCHER`
- Request body (`DispatcherLeadCreateInput`):

```json
{
  "parentName": "John Doe",
  "phone": "+77001234567",
  "childAge": 10,
  "childName": "Alex Doe",
  "branchId": "11111111-1111-1111-1111-111111111111",
  "email": "parent@example.com",
  "comment": "Interested in evening group",
  "children": [
    { "childName": "Alex Doe", "childAge": 10 }
  ]
}
```

- Response: `201 Created`

```json
{
  "leadId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
}
```

---

### `POST /admin/leads/create`
- Auth: `ADMIN`
- Request body (`AdminLeadCreateInput`):

```json
{
  "name": "John Doe",
  "phone": "+77001234567",
  "email": "parent@example.com",
  "comment": "Call tomorrow",
  "branchId": "11111111-1111-1111-1111-111111111111"
}
```

- Response: `201 Created`

```json
{
  "leadId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"
}
```

---

## Kanban / listing

### `GET /leads/kanban`
- Auth: shared read-only endpoint (intended for all authenticated roles)
- Query params:
  - `branchId` (required, UUID)
- Response: `200 OK` (`Map<LeadStatus, List<LeadOutput>>`)

```json
{
  "NEW": [],
  "CONTACTED": [],
  "QUALIFIED": [],
  "TRIAL_SCHEDULED": [],
  "TRIAL_DONE": [],
  "WAITING_PAYMENT": [],
  "WON": [],
  "LOST": []
}
```

---

### `GET /admin/leads/kanban`
- Auth: `ADMIN`
- Query params:
  - `branchId` (required, UUID)
- Response: `200 OK` (`LeadKanbanOutput`)

```json
{
  "columns": {
    "NEW": [],
    "CONTACTED": [],
    "QUALIFIED": [],
    "TRIAL_SCHEDULED": [],
    "TRIAL_DONE": [],
    "WAITING_PAYMENT": [],
    "WON": [],
    "LOST": []
  }
}
```

---

### `GET /leads`
- Auth: depends on global security config (no method-level role annotation in controller)
- Query params:
  - `statuses` (optional, repeated)
  - `assignedAdminId` (optional UUID)
  - `branchId` (optional UUID)
  - `unassigned` (optional boolean)
  - `search` (optional string; parentName/phone)
  - `createdFrom` (optional `YYYY-MM-DD`)
  - `createdTo` (optional `YYYY-MM-DD`)
  - pageable: `page`, `size`, `sort`
- Response: `200 OK` (`Page<LeadOutput>`)

---

### `GET /leads/{leadId}`
- Response: `200 OK` (`LeadOutput`)
- Errors: `404 Not Found` if lead does not exist

---

## Qualification

### `PATCH /admin/leads/{leadId}/qualify`
- Auth: `ADMIN`
- Request body (`LeadQualificationInput`):

```json
{
  "children": [
    { "childName": "Alex Doe", "childAge": 10 }
  ],
  "preferredDays": "MON,WED,FRI",
  "experience": "BEGINNER",
  "notes": "Can train after 18:00"
}
```

- Response: `204 No Content`
- Behavior:
  - applies state transition event `QUALIFY`
  - updates structured qualification fields (`preferredDays`, `experience`, `notes`)
  - replaces `children` when `children != null`
  - keeps JSON qualification payload for compatibility

---

## Trial

### `POST /admin/leads/{leadId}/trial`
- Auth: `ADMIN`
- Request body (`ScheduleTrialInput`):

```json
{
  "groupId": "22222222-2222-2222-2222-222222222222",
  "coachId": "33333333-3333-3333-3333-333333333333",
  "trialDate": "2026-03-30T18:00:00"
}
```

- Response: `204 No Content`
- Behavior:
  - saves trial details
  - applies state transition event `SCHEDULE_TRIAL`

---

## Generic state machine event

### `POST /admin/leads/{leadId}/events`
- Auth: `ADMIN`
- Request body (`LeadEventInput`):

```json
{
  "event": "QUALIFY"
}
```

Allowed values (based on state machine):

- `CONTACT`
- `QUALIFY`
- `SCHEDULE_TRIAL`
- `COMPLETE_TRIAL`
- `REQUEST_PAYMENT`
- `CONFIRM_PAYMENT`
- `REJECT`

- Response: `200 OK` (`LeadEventOutput`)

```json
{
  "leadId": "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa",
  "status": "QUALIFIED"
}
```

- Behavior:
  - delegates to state machine (`LeadService.processEvent`)
  - returns resulting `LeadStatus`
  - validates transition and returns `400` when transition is denied

---

## Conversion

### `POST /admin/leads/{leadId}/convert`
- Auth: `ADMIN`
- Request body: none
- Response: `200 OK`

```json
{
  "clientId": "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb"
}
```

- Behavior:
  - allowed only for `WON`
  - if already converted, returns existing `clientId`
  - validates readiness via domain rule (`isReadyForConversion()`)
  - keeps legacy fallback (`childName`/`childAge`) if structured children are empty

---

## Assignment

- In service layer, assignment exists (`assignLead`), but **no public Lead assignment endpoint is currently exposed**.

---

## Status transitions

- Generic transition service method exists (`processEvent`), but **no public Lead event endpoint is currently exposed**.
- Current transitions are triggered by dedicated endpoints:
  - `qualify` -> `QUALIFY`
  - `trial` -> `SCHEDULE_TRIAL`

Kanban behavior:

- Kanban returns **all leads for branch by default**.
- Filtering by `assignedAdminId/includeUnassigned` is removed.

---

## 2) LeadOutput contract

Current `LeadOutput` fields returned by API:

```json
{
  "id": "UUID",
  "parentName": "string",
  "phone": "string",
  "email": "string|null",
  "childName": "string|null",
  "childAge": 10,
  "source": "OTHER",
  "status": "NEW|CONTACTED|QUALIFIED|TRIAL_SCHEDULED|TRIAL_DONE|WAITING_PAYMENT|WON|LOST",
  "assignedAdminId": "UUID|null",
  "comment": "string|null",
  "qualificationData": {},
  "children": [
    {
      "childName": "string",
      "childAge": 10
    }
  ],
  "createdAt": "2026-03-23T10:00:00",
  "updatedAt": "2026-03-23T10:30:00"
}
```

Requested fields vs current response:

- Present now: `id`, `parentName`, `phone`, `email`, `children`, `status`, `assignedAdminId`, `createdAt`
- In domain but not exposed in `LeadOutput` yet: `branchId`, `trialDate`, `clientId`, `preferredDays`, `experience`, `notes`

---

## 3) LeadStatus flow

State machine supports:

- `NEW -> CONTACTED` (`CONTACT`)
- `CONTACTED -> QUALIFIED` (`QUALIFY`)
- `QUALIFIED -> TRIAL_SCHEDULED` (`SCHEDULE_TRIAL`)
- `TRIAL_SCHEDULED -> TRIAL_DONE` (`COMPLETE_TRIAL`)
- `TRIAL_DONE -> WAITING_PAYMENT` (`REQUEST_PAYMENT`)
- `WAITING_PAYMENT -> WON` (`CONFIRM_PAYMENT`)
- `NEW/CONTACTED/QUALIFIED -> LOST` (`REJECT`)

Current REST-triggered transitions:

- `POST /admin/leads/{leadId}/events` -> generic state-machine transition
- `PATCH /admin/leads/{leadId}/qualify` -> `QUALIFY`
- `POST /admin/leads/{leadId}/trial` -> `SCHEDULE_TRIAL`

`convert` does not move status; it requires already `WON`.

---

## 4) Business rules

1. Duplicate phone restriction:
   - Active lead with same phone is not allowed on create.

2. Conversion only from `WON`:
   - otherwise `BadRequest`.

3. Qualification required before trial:
   - enforced by state machine transition rules.

4. Children required before conversion:
   - `isReadyForConversion()` requires non-empty children with valid `childName` and `childAge`.
   - legacy fallback can populate children from `childName`/`childAge` when needed.

---

## 5) Kanban API response shape

`GET /leads/kanban`

```json
{
  "NEW": ["LeadOutput"],
  "CONTACTED": ["LeadOutput"],
  "QUALIFIED": ["LeadOutput"],
  "TRIAL_SCHEDULED": ["LeadOutput"],
  "TRIAL_DONE": ["LeadOutput"],
  "WAITING_PAYMENT": ["LeadOutput"],
  "WON": ["LeadOutput"],
  "LOST": ["LeadOutput"]
}
```

`GET /admin/leads/kanban`

```json
{
  "columns": {
    "NEW": ["LeadOutput"],
    "CONTACTED": ["LeadOutput"],
    "QUALIFIED": ["LeadOutput"],
    "TRIAL_SCHEDULED": ["LeadOutput"],
    "TRIAL_DONE": ["LeadOutput"],
    "WAITING_PAYMENT": ["LeadOutput"],
    "WON": ["LeadOutput"],
    "LOST": ["LeadOutput"]
  }
}
```

---

## 6) Frontend interaction flow

1. Create lead
   - Dispatcher: `POST /dispatcher/leads`
   - Admin: `POST /admin/leads/create`

2. Assign lead
   - No endpoint yet (needs backend API)

3. Qualify lead
   - `PATCH /admin/leads/{leadId}/qualify`

4. Schedule trial
   - `POST /admin/leads/{leadId}/trial`

5. Move lead status via event endpoint
   - `POST /admin/leads/{leadId}/events` with `event`

6. Move lead in kanban
   - Load board by `GET /leads/kanban?branchId=...`
   - Currently move actions only via exposed transitions (`qualify`, `trial`)
   - Full drag-drop can be wired to event endpoint

7. Convert to client
   - `POST /admin/leads/{leadId}/convert`

---

## 7) Suggested UI components

- Kanban board (columns by `LeadStatus`)
- Lead card (contact, status, assignee, children preview)
- Lead details drawer/page
- Qualification form (`children`, `preferredDays`, `experience`, `notes`)
- Trial scheduling modal
- Convert button (`WON` only)
- Status action menu (future, if generic event endpoint is added)

---

## 8) Notes for frontend

- Prefer `children` over legacy `childName/childAge`.
- Use `GET /leads/kanban` as default Kanban source for shared role access.
- Expect validation errors (`400`) for invalid transitions/payloads.
- Expect `404` for missing leads.
- For admin operations, send authenticated JWT with role `ADMIN`.

