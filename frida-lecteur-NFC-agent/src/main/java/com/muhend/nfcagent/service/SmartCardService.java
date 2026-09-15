package com.muhend.nfcagent.service;

import net.sf.scuba.smartcards.CardEvent;
import net.sf.scuba.smartcards.CardService;
import net.sf.scuba.smartcards.TerminalFactoryUtil;
import org.jmrtd.BACKey;
import org.jmrtd.PassportService;
import org.jmrtd.lds.CardAccessFile;
import org.jmrtd.lds.PACEInfo;
import org.jmrtd.lds.icao.DG1File;
import org.jmrtd.lds.icao.DG2File;
import org.jmrtd.lds.icao.DG11File;
import org.jmrtd.lds.icao.MRZInfo;
import org.springframework.stereotype.Service;

import javax.smartcardio.Card;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.TerminalFactory;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class SmartCardService {

    public boolean isReaderPresent() {
        try {
            TerminalFactory factory = TerminalFactory.getDefault();
            List<CardTerminal> terminals = factory.terminals().list();
            return !terminals.isEmpty();
        } catch (Exception e) {
            return false;
        }
    }

    public Map<String, Object> readIdentityCard(String documentNumber, String dateOfBirth, String dateOfExpiry) throws Exception {
        TerminalFactory factory = TerminalFactory.getDefault();
        List<CardTerminal> terminals = factory.terminals().list();
        if (terminals.isEmpty()) {
            throw new Exception("Aucun lecteur de carte à puce détecté.");
        }

        // On prend le premier lecteur disponible
        CardTerminal terminal = terminals.get(0);
        if (!terminal.isCardPresent()) {
            throw new Exception("Veuillez poser la pièce d'identité sur le lecteur.");
        }

        Card card = terminal.connect("*");
        CardService cardService = CardService.getInstance(card);
        
        PassportService passportService = new PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                true,
                false
        );

        passportService.open();

        try {
            // Clé BAC (Basic Access Control) dérivée de la MRZ
            BACKey bacKey = new BACKey(documentNumber, dateOfBirth, dateOfExpiry);
            
            // On déverrouille la puce
            passportService.sendMutualAuth(bacKey);

            // Lecture des groupes de données
            Map<String, Object> result = new HashMap<>();
            
            // DG1 : MRZ
            try {
                DG1File dg1 = new DG1File(passportService.getInputStream(PassportService.EF_DG1));
                MRZInfo mrzInfo = dg1.getMRZInfo();
                result.put("primaryIdentifier", mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                result.put("secondaryIdentifier", mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                result.put("documentNumber", mrzInfo.getDocumentNumber());
                result.put("dateOfBirth", mrzInfo.getDateOfBirth());
                result.put("dateOfExpiry", mrzInfo.getDateOfExpiry());
                result.put("gender", mrzInfo.getGender().name());
                result.put("nationality", mrzInfo.getNationality());
                result.put("issuingState", mrzInfo.getIssuingState());
                result.put("personalNumber", mrzInfo.getPersonalNumber()); // NIN souvent stocké ici
            } catch (Exception e) {
                System.err.println("Impossible de lire DG1 : " + e.getMessage());
            }
            
            // DG11 : Détails supplémentaires (Adresse, Nom complet natif / arabe)
            try {
                DG11File dg11 = new DG11File(passportService.getInputStream(PassportService.EF_DG11));
                result.put("nin_dg11", dg11.getPersonalNumber()); // NIN prioritaire
                // On pourrait extraire l'adresse, le lieu de naissance, etc. si nécessaire
            } catch (Exception e) {
                // Le DG11 n'est pas toujours présent
            }

            // Note: DG2 (Photo) peut être lue ici (JPEG2000), mais on omet l'image pour l'instant 
            // pour reproduire exactement la charge utile JSON de l'application mobile.
            // Le formattage correspond au parseur de DossierProcessingService (parseNfcJson).

            return result;
        } finally {
            try {
                passportService.close();
            } catch (Exception ignored) {}
        }
    }
}
