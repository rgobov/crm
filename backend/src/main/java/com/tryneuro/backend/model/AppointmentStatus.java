package com.tryneuro.backend.model;

public enum AppointmentStatus {
    SCHEDULED,   // Запланировано (по умолчанию)
    CONFIRMED,   // Подтверждено клиентом (нажал "ДА")
    NEEDS_CALL,  // Нужно перезвонить (нажал "НЕТ")
    COMPLETED,   // Выполнено
    CANCELLED    // Отменено
}
