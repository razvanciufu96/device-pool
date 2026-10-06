# Notes

## Decisions

**The core guarantee: no double bookings**
- The time ranges include their start and exclude their end (`[start, end)`), so a booking ending at 14:00 and one starting at 14:00 don't clash.
- A booking locks the device row (`SELECT … FOR UPDATE`), *then* checks for overlaps, *then* inserts. Two concurrent bookings of the same device run one after the other; bookings of different devices don't block each other.
- This is backed by a test that fires 10 bookings for the same slot at once and expects exactly one to succeed. I checked that the test fails when the lock is removed (all 10 succeed).
- With PostgreSQL I would add a database-level `EXCLUDE USING gist` constraint as a second safety net. H2 has no equivalent, so the guarantee lives in the service. That's the main trade-off of choosing H2.

**Interpretations**
- "Available" depends on *when*. The device list takes a time window: the default is "now", and the frontend also offers "tomorrow" (the team lead's two questions). A device counts as free tomorrow only if it has no booking at all that day; otherwise it shows as partly booked, with its time slots.
- **Cancel** removes a future booking. On a running booking it ends the reservation now (the device was brought back early). Rows are never deleted, so the history of who had which device is kept.
- Bookings can't start in the past (5-minute grace for form latency) and are capped at 14 days, so nobody can block a device indefinitely. Both are guesses; see the open questions.
- Times are stored and sent as UTC instants. The browser shows local time.

**Wish: C, damage tracking**
- I chose C because it **contradicts nothing** and builds on data we already have: the reservation history *is* "who had it last".
- **A (leads take devices away)** directly contradicts the Head of QA's first statement: "people need to be able to rely on a reservation". If one person can override bookings, no booking is reliable. If production incidents really need it, it should be an explicit, logged action that notifies the person affected, not a quiet override. I'd rather discuss it than build it in a hurry.
- **B (grab without booking)** undermines both the core rule and C. A device taken without a booking is invisible: someone else can book it for that time, and "who had it last" becomes wrong. The developer's real need is *speed*, which is better met by making booking one click. The booking dialog's "Next hour" button goes in that direction.
- Rules for C: anyone can report damage. The device is then taken out of the bookable pool until the facility role marks it repaired. "Last held by" shows the last 3 holders before the report, because damage isn't always noticed by the next person.
- `device.damaged` is a stored copy of "has open damage reports". That way booking can check it on the row it already locks, and reporting damage takes the same lock.

**Technical**
- **H2 file database:** the app runs with only JDK + Node, as required, and data survives restarts.
- **Flyway migrations** own the schema, and Hibernate only validates against it, so the constraints and indexes are explicit in SQL.
- **Fake auth** is an `X-User-Id` header that's trusted as-is, behind one small service (`CurrentUserService`) so real auth could replace it in one place.
- **Package-by-feature** (`reservation`, `damage`, `device`, `user`): small and easy to navigate.
- **No UI component library:** less to explain, and the UI is small.
- **Docker Compose** with nginx serving the frontend and proxying `/api`. The same "frontend only calls `/api`" setup works with the Vite dev proxy.

## What I cut

| Cut | Risk |
|---|---|
| Real authentication and authorization | Anyone can act as anyone. The app is unusable outside a trusted demo. |
| Admin UI for devices and users (seed data only) | Adding a device means a SQL migration. |
| Editing a reservation (only cancel and rebook) | A little friction, and the slot could be lost in between. |
| Notifications (email/Slack) for damage reports and reminders | The facility manager has to look at the page. |
| Calendar/timeline view | "Free tomorrow" is a list, not a visual schedule. |
| Leads' view of all reservations (only "mine") | Leads can see who holds a device, but have no overview page. |
| Pagination, and the per-report query for "last holders" | Fine for a pool of tens of devices, not for thousands of reports. |
| Frontend tests (I tested the frontend manually with a headless-browser click-through, not committed) | UI regressions aren't caught automatically. |
| Time zone handling beyond "browser local" | Teams in different time zones see times correctly, but "tomorrow" means the viewer's tomorrow. |

## Open questions for the product owner

1. Wish A: how often does "a release is burning" actually happen? Would it be enough to show the lead who has the device so they can ask, plus a logged override if that fails?
2. Wish B: is the problem the *time* it takes to book? If booking took 5 seconds from a phone (QR code on the device?), would that remove the need to skip it?
3. Should there be a maximum booking length, and is 14 days right? Different for laptops?
4. When a device is reported damaged, what should happen to *existing future* reservations? Right now they stay; should their owners be notified, or the reservations cancelled?
5. Is a device "free tomorrow" only if it's free all day, or is a free 2-hour window good enough? What are working hours?
6. Does anyone besides facility need to resolve damage reports, for example the person who reported a false alarm?
7. Real identity: is there an SSO (Entra ID, Google) we should integrate with? Where do roles come from?
8. Do we need check-in/check-out (physically picked up and returned), or is the reservation enough as a record?

## How I used AI

**Tools and setup**
- Claude Code (Opus 5.5) in the terminal, used as an autonomous agent inside the repo. It wrote code, ran Maven/npm/Docker and the tests, and drove a headless browser to click through the UI.
- I worked in 5 steps (setup → backend → frontend → wish C → Docker/docs). After each step I reviewed the staged changes before committing, so every commit is one reviewed step. The AI is listed as co-author.

**What I decided vs. what I delegated**
- I decided before any code was written: Vue over React/Angular, H2 over Postgres (the "JDK + Node only" constraint), wish C, Docker Compose as an extra rather than a requirement, and the step-by-step review workflow.
- I delegated most of the typing: scaffolding, entities, endpoints, Vue views, Dockerfiles, and first drafts of README/NOTES (this file included, which I then reviewed).
- I made sure the important decisions are mine and that I can explain them: the locking approach for double bookings, half-open intervals, keeping history instead of deleting, and the arguments against wishes A and B.

**Where the AI was wrong, or I overrode it**
- **Cancel returned HTTP 500** (`LazyInitializationException`: the controller read the device name after the transaction had closed). All 12 backend tests were green, because none of them cancelled through the HTTP API. It was only caught by clicking through the real UI. It was fixed with a regression test that I verified fails without the fix (separate commit `05abebf`). Lesson: green tests that the same AI wrote prove less than they seem to.
- Spring Initializr metadata gave the Boot version `4.1.1.RELEASE`, which doesn't exist on Maven Central, so the first build failed.
- Small things caught in screenshots/review: case-sensitive sorting ("iPhone" after "Xiaomi"), a table cell with `display: flex` that broke the row borders, and Vue Router 4 chosen where the brief asks for a currently supported major version (switched to 5).
- When I said "use docker", the agent couldn't tell whether I meant replacing the local setup or adding to it. It asked; I decided Docker is an addition, so the app still runs with only JDK + Node.

**How I checked the work, instead of trusting it**
- The concurrency test was verified by deliberately removing the lock: all 10 parallel bookings succeed, and the test fails.
- End-to-end click-throughs in a headless browser, against both the dev setup and Docker Compose.

**What I'd double-check before production**
- Authentication: the `X-User-Id` header is trusted blindly. Every endpoint's authorization rules need review.
- Move to Postgres and add an `EXCLUDE` constraint so the database itself enforces no overlaps; load-test the locking.
- Transaction boundaries in every endpoint (that's where the 500 came from). `open-in-view` is off on purpose, so lazy loading fails loudly.
- Time zones: demo data uses the server's zone, and "tomorrow" is the browser's tomorrow.
- Disable the H2 console. Add input limits, rate limiting, and logging/audit of who cancelled or resolved what.
- Flyway warns that H2 2.4 is newer than the version it's verified against.
