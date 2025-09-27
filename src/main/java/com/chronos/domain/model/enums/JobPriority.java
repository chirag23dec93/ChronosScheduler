package com.chronos.domain.model.enums;

public enum JobPriority {
    LOW,
    MEDIUM,
    HIGH;

    public short getValue() {
        return (short) ordinal();
    }

    public static JobPriority fromValue(short value) {
        for (JobPriority priority : values()) {
            if (priority.ordinal() == value) {
                return priority;
            }
        }
        throw new IllegalArgumentException("Invalid priority value: " + value);
    }
}
