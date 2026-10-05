package com.example.neoncache.entity;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "request_log")
public class RequestLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "request_value", nullable = false, length = 50)
    private String requestValue;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    public RequestLog() {
    }

    public RequestLog(String requestValue, LocalDateTime createdAt) {
        this.requestValue = requestValue;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRequestValue() {
        return requestValue;
    }

    public void setRequestValue(String requestValue) {
        this.requestValue = requestValue;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
