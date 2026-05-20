# CRM Lead API Specification (Frontend)

> Version date: 2026-03-25
> 
> Scope: endpoints and behavior for Lead flows in current backend implementation.

---

## 0) Operating Flow (Step 1)

This section defines operational rules for admins in branch CRM flow.

### 0.1 Roles in flow

- **Assignee (`assignedAdminId`)**: accountable owner of lead.
- **Actor**: admin who actually performs an action now (can be different from assignee, but must have branch access).
- **Branch scope**: any action is allowed only if admin has access to lead branch.

### 0.2 Stage definition (Done criteria + SLA target)

1. `NEW`
   - Meaning: lead created, not contacted yet.
   - Done criteria to exit: first contact attempt logged via `CONTACT`.
   - SLA target: first attempt in **15 minutes** during working hours.

2. `CONTACTED`
   - Meaning: parent was reached and initial context collected.
   - Done criteria to exit: qualification completed via `QUALIFY`.
   - SLA target: move to `QUALIFIED` in **24 hours**.

3. `QUALIFIED`
   - Meaning: child/profile requirements captured, ready for trial planning.
   - Done criteria to exit: trial slot scheduled via `SCHEDULE_TRIAL`.
   - SLA target: schedule trial in **48 hours**.

4. `TRIAL_SCHEDULED`
   - Meaning: trial date/time fixed.
   - Done criteria to exit:
     - `COMPLETE_TRIAL` -> `TRIAL_DONE`, or
     - `NO_SHOW` -> `LOST`.
   - SLA target: status update same day (max **4 hours** after trial start).

5. `TRIAL_DONE`
   - Meaning: trial completed successfully.
   - Done criteria to exit:
     - `REQUEST_PAYMENT` -> `WAITING_PAYMENT`, or
     - `POST_TRIAL_REJECT` -> `LOST`.
   - SLA target: payment request in **24 hours**.

6. `WAITING_PAYMENT`
   - Meaning: offer sent, waiting payment confirmation.
   - Done criteria to exit:
     - `CONFIRM_PAYMENT` -> `WON`, or
     - `REJECT` -> `LOST`.
   - SLA target: final resolution in **7 calendar days**.

7. `WON` / `LOST`
   - Final statuses.
   - `WON`: ready for convert to client flow.

### 0.3 Operational rules

- Any branch admin can progress lead; ownership is not a blocker.
- `assignedAdminId` remains responsibility marker; actions by other admins must be reflected in activity actor.
- Reassign lead when owner is unavailable more than one working day.
- Do not keep leads in intermediate stage without next-step date/plan.

### 0.3.1 Ownership model (Step 2)

Definitions:

- **Owner**: `assignedAdminId` in lead card. Accountable for outcome and next action plan.
- **Executor**: admin who performed concrete action (status move / trial scheduling / conversion).

Rules:

1. Any admin with branch access may execute lead actions.
2. Owner is still accountable for SLA, even if action was executed by another admin.
3. Executor must be visible in lead activity history.
4. Owner change is explicit only via `PATCH /admin/leads/{leadId}/assign`.

Handoff policy:

1. If owner is absent for >1 working day, lead must be reassigned.
2. If lead breached SLA and owner is unavailable, nearest available branch admin executes immediate next action and records progress; reassignment should follow in same working day.
3. High-priority stages (`NEW`, `TRIAL_SCHEDULED`, `TRIAL_DONE`) should not wait for original owner.

UI expectations:

- Lead card shows current owner (`assignedAdmin`).
- Activity timeline shows executor (`actorName`) for each event.
- If executor != owner, timeline still shows action as valid (not exception).

### 0.4 Escalation policy

- Breach of `NEW`/`CONTACTED` SLA -> escalate to branch head same day.
- Repeated no-progress (2+ breaches per admin/week) -> manual review of workload and lead distribution.

### 0.5 Metrics to monitor weekly

- Time to first contact (`NEW -> CONTACTED`)
- Qualification lead time (`CONTACTED -> QUALIFIED`)
- Trial scheduling lead time (`QUALIFIED -> TRIAL_SCHEDULED`)
- Trial show rate (`TRIAL_SCHEDULED -> TRIAL_DONE` vs `NO_SHOW`)
- Trial-to-payment conversion (`TRIAL_DONE -> WAITING_PAYMENT -> WON`)
- Share of cross-owner executions (executor != owner) and SLA impact

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
  "branchId": "11111111-1111-1111-1111-111111111111",
  "email": "parent@example.com",
  "comment": "Interested in evening group",
  "children": [
    {
      "childName": "Alex Doe",
      "childAge": 10,
      "gender": "MALE",
      "experience": "BEGINNER"
    }
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
  "children": [
    {
      "childName": "Alex Doe",
      "childAge": 10,
      "gender": "MALE",
      "experience": "BEGINNER"
    }
  ],
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
- Auth: `ADMIN`
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
- Auth: `ADMIN`
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
- Auth: `ADMIN`
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
    {
      "childName": "Alex Doe",
      "childAge": 10,
      "gender": "MALE",
      "experience": "BEGINNER"
    }
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
  - replaces `children` when `children != null` (clear existing + recreate)
  - keeps JSON qualification payload for compatibility

---

## Trial

### `POST /admin/leads/{leadId}/trial`
- Auth: `ADMIN`
- Request body (`ScheduleTrialInput`):

```json
{
  "childId": "44444444-4444-4444-4444-444444444444",
  "groupId": "22222222-2222-2222-2222-222222222222",
  "coachId": "33333333-3333-3333-3333-333333333333",
  "slot": {
    "date": "2026-03-30",
    "startTime": "18:00:00"
  },
  "comment": "First trial for child"
}
```

- Response: `204 No Content`
- Behavior:
  - validates lead exists
  - validates `childId` belongs to `lead.children[].id`
  - validates slot exists in schedule
  - validates coach/group availability and conflicts
  - attaches trial to existing schedule slot
  - applies state transition event `SCHEDULE_TRIAL`

---

### `GET /admin/groups/{groupId}/available-slots?date=YYYY-MM-DD`
- Auth: `ADMIN`
- Query params:
  - `date` (required, `YYYY-MM-DD`)
- Response: `200 OK` (`List<AvailableSlotOutput>`)

```json
[
  {
    "date": "2026-03-30",
    "startTime": "18:00:00",
    "endTime": "19:00:00"
  }
]
```

---

### `GET /admin/coaches/{coachId}/available-slots?date=YYYY-MM-DD`
- Auth: `ADMIN`
- Query params:
  - `date` (required, `YYYY-MM-DD`)
- Response: `200 OK` (`List<AvailableSlotOutput>`)

```json
[
  {
    "date": "2026-03-30",
    "startTime": "18:00:00",
    "endTime": "19:00:00"
  }
]
```

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
- `NO_SHOW`
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
  - uses structured `children` only (no legacy single-child fallback)

---

## Assignment

### `PATCH /admin/leads/{leadId}/assign`
- Auth: `ADMIN`
- Request body (`LeadAssignInput`):

```json
{
  "assignedAdminId": "55555555-5555-5555-5555-555555555555"
}
```

- Response: `204 No Content`
- Behavior:
  - validates request admin exists (from JWT)
  - validates target `assignedAdminId` exists
  - validates lead exists
  - assigns lead to provided admin

---

## Status transitions

- Current transitions are triggered by dedicated endpoints:
  - `events` -> any allowed transition by state machine event
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
  "source": "OTHER",
  "status": "NEW|CONTACTED|QUALIFIED|TRIAL_SCHEDULED|TRIAL_DONE|WAITING_PAYMENT|WON|LOST",
  "assignedAdminId": "UUID|null",
  "comment": "string|null",
  "qualificationData": {},
  "children": [
    {
      "id": "UUID",
      "childName": "string",
      "childAge": 10,
      "gender": "MALE|FEMALE|OTHER|null",
      "experience": "string|null"
    }
  ],
  "trial": {
    "id": "UUID",
    "leadId": "UUID",
    "childId": "UUID",
    "groupId": "UUID|null",
    "coachId": "UUID|null",
    "trialDate": "2026-03-30",
    "startTime": "18:00:00",
    "endTime": "19:00:00",
    "comment": "string|null",
    "status": "SCHEDULED|COMPLETED|CANCELED"
  },
  "createdAt": "2026-03-23T10:00:00",
  "updatedAt": "2026-03-23T10:30:00"
}
```

Requested fields vs current response:

- Present now: `id`, `parentName`, `phone`, `email`, `children`, `status`, `assignedAdminId`, `createdAt`
- `children` now includes stable `id` for trial scheduling payload `childId`
- `trial` is nested and is `null` until trial scheduling is done
- In domain but not exposed in `LeadOutput` yet: `branchId`, `clientId`, `preferredDays`, `experience`, `notes`

`LeadChildInput` / `children[]` validation:

- `childName` is required
- `childAge` is required and must be in range `3..18`
- `gender` is optional (`MALE|FEMALE|OTHER`)
- `experience` is optional (max 100 chars)

---

## 3) LeadStatus flow

State machine supports:

- `NEW -> CONTACTED` (`CONTACT`)
- `CONTACTED -> QUALIFIED` (`QUALIFY`)
- `QUALIFIED -> TRIAL_SCHEDULED` (`SCHEDULE_TRIAL`)
- `TRIAL_SCHEDULED -> TRIAL_DONE` (`COMPLETE_TRIAL`)
- `TRIAL_DONE -> WAITING_PAYMENT` (`REQUEST_PAYMENT`)
- `WAITING_PAYMENT -> WON` (`CONFIRM_PAYMENT`)
- `WAITING_PAYMENT -> LOST` (`REJECT`)
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

4. Trial slot requirements:
   - `childId`, `slot.date`, `slot.startTime` are required.
   - at least one of `groupId` or `coachId` is required.
   - selected slot must exist and be available.

5. Children required before conversion:
   - `isReadyForConversion()` requires non-empty children with valid `childName` and `childAge`.
   - no legacy fallback from top-level child fields.

6. Full children editing on qualification:
   - when `children` is provided in qualification payload, backend recreates list from scratch.
   - child IDs in response can change after such replacement.

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
   - both endpoints require non-empty `children[]`

2. Assign lead
   - `PATCH /admin/leads/{leadId}/assign`

3. Qualify lead
   - `PATCH /admin/leads/{leadId}/qualify`

4. Schedule trial
   - load candidate slots:
     - `GET /admin/groups/{groupId}/available-slots?date=...`
     - `GET /admin/coaches/{coachId}/available-slots?date=...`
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
- Status action menu (uses `POST /admin/leads/{leadId}/events`)

---

## 8) Notes for frontend

- `children[]` is the only child source in create/output payloads.
- For trial scheduling, always use `children[].id` from `LeadOutput` as request `childId`.
- Use `GET /leads/kanban` as default Kanban source for shared role access.
- Expect validation errors (`400`) for invalid transitions/payloads.
- Expect `404` for missing leads.
- For admin operations, send authenticated JWT with role `ADMIN`.
