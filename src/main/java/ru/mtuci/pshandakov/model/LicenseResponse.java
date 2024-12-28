package ru.mtuci.pshandakov.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;

@Data
@AllArgsConstructor
@Builder
public class LicenseResponse {

    private String message;

    private Ticket ticket;

}
