package com.muhend.nfcagent.controller;

import com.muhend.nfcagent.dto.MrzRequestDto;
import com.muhend.nfcagent.service.SmartCardService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/nfc")
@RequiredArgsConstructor
public class NfcController {

    private final SmartCardService smartCardService;

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        String debug = "None";
        boolean readerPresent = false;
        try {
            javax.smartcardio.TerminalFactory factory = javax.smartcardio.TerminalFactory.getDefault();
            debug = "Factory: " + factory.getType();
            java.util.List<javax.smartcardio.CardTerminal> terminals = factory.terminals().list();
            debug += " | Terminals: " + terminals.size();
            readerPresent = !terminals.isEmpty();
        } catch (Exception e) {
            debug = "Error: " + e.getMessage();
        }
        
        return ResponseEntity.ok(Map.of(
            "agent_version", "1.0.0",
            "reader_present", readerPresent,
            "status", "OK",
            "debug", debug
        ));
    }

    @PostMapping("/read")
    public ResponseEntity<?> readCard(@RequestBody MrzRequestDto mrzRequest) {
        try {
            if (mrzRequest.getDocumentNumber() == null || mrzRequest.getDateOfBirth() == null || mrzRequest.getDateOfExpiry() == null) {
                return ResponseEntity.badRequest().body(Map.of("error", "MRZ data is incomplete"));
            }
            
            // On lance la lecture NFC via PC/SC
            Map<String, Object> result = smartCardService.readIdentityCard(
                    mrzRequest.getDocumentNumber(),
                    mrzRequest.getDateOfBirth(),
                    mrzRequest.getDateOfExpiry()
            );
            
            return ResponseEntity.ok(result);
        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
}
