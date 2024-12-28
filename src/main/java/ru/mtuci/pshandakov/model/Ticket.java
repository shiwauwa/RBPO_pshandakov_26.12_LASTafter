package ru.mtuci.pshandakov.model;

import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import ru.mtuci.pshandakov.utils.SignatureUtil;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.UUID;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
@Entity
@Builder
public class Ticket {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private LocalDateTime serverDate;

    private int ticketLifetime;

    private Date activationDate;

    private Date expirationDate;

    private Long userId;

    private Long deviceId;

    private boolean isBlocked;

    private String detail;

    private String digitalSignature;

    public static Ticket createTicket(Long userId, boolean isBlocked, Date expirationDate, String detail)
            throws NoSuchAlgorithmException, SignatureException, InvalidKeyException, JsonProcessingException {
        var ticket = Ticket.builder()
                .serverDate(LocalDateTime.now())
                .ticketLifetime(2)
                .activationDate(new Date())
                .expirationDate(expirationDate)
                .userId(userId)
                .isBlocked(isBlocked)
                .detail(detail)
                .build();

        ticket.setDigitalSignature(SignatureUtil.makeSignature(ticket));
        return ticket;
    }

}
