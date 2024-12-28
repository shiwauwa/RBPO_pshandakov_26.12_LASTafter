package ru.mtuci.pshandakov.controller;

import jakarta.annotation.security.RolesAllowed;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;

import lombok.extern.slf4j.Slf4j;


import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import ru.mtuci.pshandakov.configuration.JwtTokenProvider;
import ru.mtuci.pshandakov.exception.LicenseException;
import ru.mtuci.pshandakov.model.ApplicationUser;
import ru.mtuci.pshandakov.model.LicenseActivationRequest;
import ru.mtuci.pshandakov.model.LicenseCheckRequest;
import ru.mtuci.pshandakov.model.LicenseCreateRequest;
import ru.mtuci.pshandakov.model.LicenseCreateResponse;
import ru.mtuci.pshandakov.model.LicenseResponse;
import ru.mtuci.pshandakov.model.LicenseUpdateRequest;
import ru.mtuci.pshandakov.model.Ticket;
import ru.mtuci.pshandakov.service.impl.ApplicationUserService;
import ru.mtuci.pshandakov.service.impl.LicensingService;

import java.text.ParseException;
import java.util.Optional;

@RestController
@RequestMapping("/licensing")
@RequiredArgsConstructor
@Slf4j
public class LicensingController {

    private final JwtTokenProvider jwtTokenProvider;
    private final ApplicationUserService applicationUserService;
    private final LicensingService licensingService;


    private Optional<ApplicationUser> getUserFromRequest(HttpServletRequest request) {
        String email = jwtTokenProvider.getEmailFromRequest(request);
        return applicationUserService.getUserByEmail(email);
    }

    private LicenseResponse licenseResponseWithErrorTicket(HttpServletRequest request, String message) {
        var user = getUserFromRequest(request).orElse(null);
        Ticket ticket = null;
        try {
            ticket = Ticket.createTicket(user != null ? user.getId() : null, true, null, message);
            log.info("Тикет с ошибкой: {}", ticket);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
        }

        return LicenseResponse.builder()
                .message(message)
                .ticket(ticket)
                .build();
    }


    @PostMapping("/create")
    @RolesAllowed("ROLE_ADMIN")
    public ResponseEntity<LicenseCreateResponse> createLicense(HttpServletRequest request, @RequestBody LicenseCreateRequest requestData) {
        try {
            var result = licensingService.createLicense(getUserFromRequest(request).orElse(null), requestData);
            return ResponseEntity.status(HttpStatus.CREATED).body(result);
        } catch (LicenseException e) {
            return ResponseEntity.status(e.getHttpStatus()).body(LicenseCreateResponse.builder()
                    .message(e.getMessage())
                    .build());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(
                    LicenseCreateResponse.builder()
                            .message("Произошла ошибка при создании лицензии")
                            .build());
        }
    }

    @PostMapping("/update")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<?> updateLicense(HttpServletRequest request, @RequestBody LicenseUpdateRequest requestData) {
        try {
            var result = licensingService.updateLicense(getUserFromRequest(request).orElse(null), requestData);
            return ResponseEntity.ok().body(result);
        } catch (ParseException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Неверный формат даты.");
        } catch (LicenseException e) {
            if (e.isCreateTicket()) {
                return ResponseEntity.status(e.getHttpStatus()).body(licenseResponseWithErrorTicket(request, e.getMessage()));
            }

            return ResponseEntity.status(e.getHttpStatus()).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Ошибка при продлении лицензии.");
        }
    }

    @PostMapping("/check")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<?> checkLicense(HttpServletRequest request, @RequestBody LicenseCheckRequest requestData) {
        try {
            var result = licensingService.checkLicense(getUserFromRequest(request).orElse(null), requestData);
            return ResponseEntity.ok().body(result);
        } catch (LicenseException e) {
            if (e.isCreateTicket()) {
                return ResponseEntity.status(e.getHttpStatus()).body(licenseResponseWithErrorTicket(request, e.getMessage()));
            }

            return ResponseEntity.status(e.getHttpStatus()).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Произошла ошибка при проверке лицензии.");
        }
    }


    @PostMapping("/activation")
    @RolesAllowed({"ROLE_USER", "ROLE_ADMIN"})
    public ResponseEntity<?> activateLicense(HttpServletRequest request, @RequestBody LicenseActivationRequest activationRequest) {
        try {
            var result = licensingService.activateLicense(getUserFromRequest(request).orElse(null), activationRequest);
            return ResponseEntity.ok().body(result);
        } catch (LicenseException e) {
            if (e.isCreateTicket()) {
                return ResponseEntity.status(e.getHttpStatus()).body(licenseResponseWithErrorTicket(request, e.getMessage()));
            }

            return ResponseEntity.status(e.getHttpStatus()).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(e.getMessage());
        }
    }
}
