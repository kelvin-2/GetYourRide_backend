package com.example1.getyourride.dto.response;

import java.time.LocalTime;

public class ShuttleTimeSlotResponse {

    private Long slotId;
    private String period; // sent as plain string ("Morning"/"Afternoon") - easy for Kotlin to parse
    private LocalTime departs;
    private LocalTime arrives;

    public ShuttleTimeSlotResponse() {
    }

    public ShuttleTimeSlotResponse(Long slotId, String period, LocalTime departs, LocalTime arrives) {
        this.slotId = slotId;
        this.period = period;
        this.departs = departs;
        this.arrives = arrives;
    }

    // ---- getters and setters ----
    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public String getPeriod() {
        return period;
    }

    public void setPeriod(String period) {
        this.period = period;
    }

    public LocalTime getDeparts() {
        return departs;
    }

    public void setDeparts(LocalTime departs) {
        this.departs = departs;
    }

    public LocalTime getArrives() {
        return arrives;
    }

    public void setArrives(LocalTime arrives) {
        this.arrives = arrives;
    }
}