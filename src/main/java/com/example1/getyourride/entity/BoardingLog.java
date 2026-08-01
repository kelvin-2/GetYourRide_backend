package com.example1.getyourride.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "boarding_log")
public class BoardingLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    private Long logId;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "boarded_at")
    private LocalDateTime boardedAt;

    @Column(name = "dropped_off_at")
    private LocalDateTime droppedOffAt;

    public BoardingLog() {
    }

    public Long getLogId() {
        return logId;
    }

    public void setLogId(Long logId) {
        this.logId = logId;
    }

    public Booking getBooking() {
        return booking;
    }

    public void setBooking(Booking booking) {
        this.booking = booking;
    }

    public LocalDateTime getBoardedAt() {
        return boardedAt;
    }

    public void setBoardedAt(LocalDateTime boardedAt) {
        this.boardedAt = boardedAt;
    }

    public LocalDateTime getDroppedOffAt() {
        return droppedOffAt;
    }

    public void setDroppedOffAt(LocalDateTime droppedOffAt) {
        this.droppedOffAt = droppedOffAt;
    }
}
