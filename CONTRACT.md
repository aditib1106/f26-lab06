# Contract Worksheet

One section per milestone. Fill each one in as you go, in order. Write each
prediction before you run anything. That is the part a TA asks about.

Keep it short and specific. Point at methods, call sites, and error text.

---

## Milestone 1: The notes overload

### Prediction (write this before you run the build, and you can deliberate with your agent)

**Will the consumer, untouched, still compile and pass?** Yes or no.

**Prediction: Yes.**

**Why.** What does the compiler do with the consumer's existing call sites once
the new overload exists?

The new overload has five parameters (`roomId, start, end, waitlistKey, notes`).
The consumer's two call sites, `FrontDesk.bookWalkIn` (`createBooking(roomId,
startMinute, endMinute, null)`) and `FrontDesk.joinWaitlist` (`createBooking(
roomId, startMinute, endMinute, guestName)`), pass four arguments. Overload
resolution filters by arity first, so only the existing four-argument method is
applicable and both call sites resolve to it exactly as before. The literal
`null` cannot be ambiguous because there is only one four-argument candidate.
`getNotes()` is a new getter the consumer never calls. Nothing in the javadoc
contract becomes false for an existing caller, so behavior is unchanged too.

### What happened

**The result.** What the build printed for each module.

Compiled with `javac -Xlint:all` (api, then consumer against it):

```
api:      no errors, no warnings
consumer: no errors, no warnings   (FrontDesk.java compiled unchanged)
```

Maven output for this commit (`mvn -B test`): _paste here: expect api 5 tests,
consumer 7 tests, all green._

**If your prediction was wrong,** say what you missed.

My prediction held. The compiler picked the existing four-argument method for
both consumer call sites because the new overload has a different arity.

**Is an additive change always safe in Java?** One case where adding something
to an API still breaks a caller, if you can name one.

No. Two cases: (1) adding an abstract method to an interface breaks every
outside class that implements it, because it no longer compiles (here that is
safe only because the consumer uses `InMemoryBookingService` and never
implements `BookingApi`); (2) adding an overload with the same arity as an
existing one can make a call ambiguous, e.g. a new
`createBooking(String, long, long, Object)` would make the consumer's literal
`createBooking(roomId, start, end, null)` fail with "reference is ambiguous".

---

## Milestone 2: The request object

### Prediction (write this before you run the build)

**Will the untouched consumer still compile and pass?** Yes or no, and if no,
which module goes red and whether at compile time or test time.

**Prediction: No.** `consumer` goes red at compile time, before any of its
tests run. Removing `createBooking(String, long, long, String)` in favor of
`createBooking(BookingRequest)` means the old signature no longer exists, so
javac reports a "no suitable method found" / "cannot find symbol" error.

**Where.** Name the call sites you expect to be affected, if any.

Both `createBooking` calls in `consumer/.../FrontDesk.java`: `bookWalkIn` and
`joinWaitlist`. The other calls (`listBookings`, `cancelBooking`) are untouched.

**What about the tests in `api/`, after you update them?** And whether their
result is evidence about the consumer.

All 5 `api/` tests should pass once they are rewritten to the new call. That
result is not evidence about the consumer: the api suite never touches the
consumer module, and it was rewritten by me to match my own change. It can only
show the producer agrees with itself. Only the consumer's build can detect that
the contract broke for an outside caller.

### Step 1: after the fold

**What the build printed.** Paste it for each module, including file and
line for anything that failed.

`javac -Xlint:all` output (api sources and the rewritten api tests are updated;
consumer is untouched):

```
api:      compiles, no errors, no warnings
consumer: 2 errors
consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:27: error: method createBooking in interface BookingApi cannot be applied to given types;
        return api.createBooking(roomId, startMinute, endMinute, null);
  required: BookingRequest
  found:    String,long,long,<null>
  reason: actual and formal argument lists differ in length
consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:33: error: method createBooking in interface BookingApi cannot be applied to given types;
        return api.createBooking(roomId, startMinute, endMinute, guestName);
  required: BookingRequest
  found:    String,long,long,<null>
  reason: actual and formal argument lists differ in length
2 errors
```

Maven output for this commit (`mvn -B clean test`): _paste here: expect api
5 tests green, consumer COMPILATION ERROR at FrontDesk.java:[27,..] and [33,..],
0 consumer tests run._

**Which module's tests ran, and which did not.** And what that tells you about
who can detect a contract break.

The api module builds first and its 5 tests (rewritten by me to the new call)
run and pass. The consumer fails in the compile phase, so none of its 7 tests
run at all. The api suite cannot detect the break, since I rewrote it to match
my own change. Only the consumer's build can, which is why its suite in my
build is the gate. A break shows up as a compile error there, not as a failing
assertion.

### Step 2: the deprecation path

**What you added.** The signatures that came back, and what they delegate to.

Both old signatures are back on `BookingApi` as `@Deprecated default` methods:
`createBooking(String, long, long, String)` and
`createBooking(String, long, long, String, String)`. Each builds a
`BookingRequest` with `BookingRequest.of(...)`, `.withWaitlistKey(...)` and
(for the five-argument one) `.withNotes(...)`, then calls
`createBooking(BookingRequest)`. Being default methods, `InMemoryBookingService`
implements only the new method.

**The warnings.** Paste one deprecation warning line from the build log (from
a `mvn -B clean test` run, since a rerun with nothing to compile prints none).

From `javac -Xlint:all` on the consumer (no changes to `consumer/`):

```
consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:27: warning: [deprecation] createBooking(String,long,long,String) in BookingApi has been deprecated
```

Maven output for this commit (`mvn -B clean test`): _paste here: expect both
modules green and the same `[deprecation]` warning from the consumer's compile._

**What the deprecation path resolves.** Who can now build that could not build
during step 1, and who is on which schedule.

The front desk team can build again, with no change on their side, even though
they were completely broken in step 1. The two schedules are now decoupled:
the API team ships the new `createBooking(BookingRequest)` now and new callers
use it immediately, while the front desk team migrates `FrontDesk.java:27` and
`:33` whenever their own schedule allows, before we remove the old methods in
some later version.

**What the warnings accomplish that a README note would not.** Be concrete
about where the warning shows up and who sees it without looking for it.

The warning is printed by the consumer team's own compiler, on their own build,
at the exact lines that need to change (`FrontDesk.java:27` and `:33`), and
their IDE strikes through the old call. They see it without reading anything
we published, and it points at the replacement through the `@deprecated`
javadoc. A README note sits in our repo, which they have no reason to open, and
does not name their call sites or go away when they fix them.

---

## Milestone 3: The misuse critique

Not coded. One misuse, one redesign, one cost. Discuss it with your TA.

### The misuse

**What is easy to get wrong.** One specific thing about the API surface.

The `boolean notifyWaitlist` flag on `cancelBooking(long bookingId, boolean
notifyWaitlist)`. A bare `true` or `false` does not say what it does, and
swapping it still compiles.

**The call site.** File and line in `consumer/`, with the call. Show the
code that a reader cannot understand without opening the javadoc, or that a
caller could get wrong with the compiler still happy.

`consumer/src/main/java/edu/cmu/cs214/frontdesk/FrontDesk.java:48` and `:53`:

```java
return api.cancelBooking(bookingId, true);    // line 48, cancelAndOfferToWaitlist
return api.cancelBooking(bookingId, false);   // line 53, cancelQuietly
```

A reader cannot tell what `true` means without opening the javadoc. Only the
method names around them carry the meaning, and nothing checks that they agree
with the flag.

**What goes wrong when it happens.** Silent bad behavior, wrong data, a crash
somewhere far away?

If someone flips the literal, `cancelQuietly` would start promoting a
waitlisted guest to CONFIRMED on a desk correction, or a real cancellation
would leave the next guest in line waiting forever. Both compile, no test in
the consumer might notice, and the visible effect (a guest told they have a
room, or never told) happens far from the typo.

### The redesign

**The proposal.** Types, enums, factories, or whatever you are proposing. Show
the new signature and the new call site.

Replace the boolean with an enum:

```java
public enum WaitlistAction { PROMOTE_NEXT, LEAVE_QUEUE }

boolean cancelBooking(long bookingId, WaitlistAction onWaitlist);
```

New call sites:

```java
return api.cancelBooking(bookingId, WaitlistAction.PROMOTE_NEXT);  // line 48
return api.cancelBooking(bookingId, WaitlistAction.LEAVE_QUEUE);   // line 53
```

**Why the mistake is now hard or impossible to make.** Point at the mechanism,
such as the compiler, a validating constructor, or an exhaustive switch.

The type system does the enforcing. A bare `true` or `false` no longer
compiles, so the caller must name the behavior at the call site, and the
meaning is readable without the javadoc. The implementation can `switch` over
the enum exhaustively, so adding a third mode later is a compile error
everywhere it is not handled instead of a silent default.

### One tradeoff

**What it costs.** Something real, such as caller ceremony, migration burden
against the deprecation path you just built, or more types for a newcomer to
learn. "No real downside" does not count.

Migration burden. Changing the parameter type is a breaking change to a method
the front desk team calls at two sites, so I would have to keep
`cancelBooking(long, boolean)` as a `@Deprecated` overload that delegates to
the enum version, exactly as in milestone 2, and carry it until they migrate.
It also adds one more type for a newcomer to learn, and every caller has to
import and write the longer call.

**When the price is worth paying.** A condition under which it is.

When the flag changes real-world outcomes that are hard to undo (a guest is
confirmed into a room) and more than two modes are likely, for example a third
"notify but do not promote". A boolean cannot grow, so paying the migration
cost once now is cheaper than paying it again when a second flag shows up.
