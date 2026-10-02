package edu.cmu.cs214.booking;

/**
 * Everything needed to ask for a booking. Immutable.
 *
 * <p>Build one with {@link #of(String, long, long)} and add the optional parts
 * with {@link #withWaitlistKey(String)} and {@link #withNotes(String)}. The
 * rules for each field are those documented on
 * {@link BookingApi#createBooking(BookingRequest)}.
 */
public final class BookingRequest {

    private final String roomId;
    private final long startMinute;
    private final long endMinute;
    private final String waitlistKey;
    private final String notes;

    private BookingRequest(String roomId, long startMinute, long endMinute,
                           String waitlistKey, String notes) {
        this.roomId = roomId;
        this.startMinute = startMinute;
        this.endMinute = endMinute;
        this.waitlistKey = waitlistKey;
        this.notes = notes;
    }

    /** A request for {@code [startMinute, endMinute)} with no waitlist key and no notes. */
    public static BookingRequest of(String roomId, long startMinute, long endMinute) {
        return new BookingRequest(roomId, startMinute, endMinute, null, null);
    }

    /** A copy of this request that waitlists on conflict under the given key. */
    public BookingRequest withWaitlistKey(String waitlistKey) {
        return new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes);
    }

    /** A copy of this request carrying the given notes. */
    public BookingRequest withNotes(String notes) {
        return new BookingRequest(roomId, startMinute, endMinute, waitlistKey, notes);
    }

    public String getRoomId() {
        return roomId;
    }

    public long getStartMinute() {
        return startMinute;
    }

    public long getEndMinute() {
        return endMinute;
    }

    /** The waitlist key, or null to decline waitlisting. */
    public String getWaitlistKey() {
        return waitlistKey;
    }

    /** The notes, or null for none. */
    public String getNotes() {
        return notes;
    }
}
