package com.rideshare.dto;

import jakarta.validation.constraints.NotNull;

public class AvailabilityRequest {

    @NotNull
    private Boolean available;

    public Boolean getAvailable() {
        return available;
    }

    public void setAvailable(Boolean available) {
        this.available = available;
    }
}
