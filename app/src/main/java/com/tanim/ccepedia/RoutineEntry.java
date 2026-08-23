package com.tanim.ccepedia;

/**
 * One class in a student's personal routine: a day, a start and end time, and a course.
 *
 * <p>Persisted as a map inside the {@code routine} array field on {@code users/{uid}}, so it needs
 * a public no-arg constructor and getters/setters for Firestore (de)serialization. {@code day} is
 * an IIUC week index — 0=Sat, 1=Sun, 2=Mon, 3=Tue, 4=Wed (Thu/Fri are off) — and {@code start} /
 * {@code end} are 24-hour {@code "HH:mm"} strings so they sort and parse without locale surprises.
 * {@code end} is {@code ""} for classes saved before end times existed; callers treat that as "no
 * explicit end" and fall back to an assumed duration.
 */
public class RoutineEntry {

    private int day;
    private String start;
    private String end;
    private String course;

    public RoutineEntry() {
        // Required by Firestore.
    }

    public RoutineEntry(int day, String start, String course) {
        this(day, start, "", course);
    }

    public RoutineEntry(int day, String start, String end, String course) {
        this.day = day;
        this.start = start;
        this.end = end;
        this.course = course;
    }

    public int getDay() {
        return day;
    }

    public void setDay(int day) {
        this.day = day;
    }

    public String getStart() {
        return start;
    }

    public void setStart(String start) {
        this.start = start;
    }

    public String getEnd() {
        return end;
    }

    public void setEnd(String end) {
        this.end = end;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }
}
