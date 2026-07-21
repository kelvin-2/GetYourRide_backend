package com.example1.getyourride.entity;

import jakarta.persistence.*;
import java.time.LocalTime;

@Entity
@Table(name = "shuttle_time_slot")
public class ShuttleTimeSlot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "slot_id")
    private Long slotId;

    @Enumerated(EnumType.STRING)
    @Column(name = "period", nullable = false)
    private Period period; // MORNING or AFTERNOON

    @Column(name = "departs", nullable = false)
    private LocalTime departs;

    @Column(name = "arrives", nullable = false)
    private LocalTime arrives;

    // matches the SQL ENUM('Morning','Afternoon') values exactly
    public enum Period {
        Morning, Afternoon
    }

    public ShuttleTimeSlot() {
    }

    // ---- getters and setters ----
    public Long getSlotId() {
        return slotId;
    }

    public void setSlotId(Long slotId) {
        this.slotId = slotId;
    }

    public Period getPeriod() {
        return period;
    }

    public void setPeriod(Period period) {
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