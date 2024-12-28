package ru.mtuci.pshandakov.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import ru.mtuci.pshandakov.model.Ticket;

import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.util.Base64;

public final class SignatureUtil {

    private SignatureUtil() {

    }


    public static String makeSignature(Ticket ticket) throws NoSuchAlgorithmException, JsonProcessingException, InvalidKeyException, SignatureException {
        ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
        String ticketRes = objectMapper.writeValueAsString(ticket);

        KeyPairGenerator keyPairGenerator = KeyPairGenerator.getInstance("RSA");
        keyPairGenerator.initialize(2048);
        KeyPair keyPair = keyPairGenerator.generateKeyPair();
        PrivateKey privateKey = keyPair.getPrivate();

        Signature signature = Signature.getInstance("SHA256withRSA");
        signature.initSign(privateKey);
        signature.update(ticketRes.getBytes());

        return Base64.getEncoder().encodeToString(signature.sign());
    }
}
