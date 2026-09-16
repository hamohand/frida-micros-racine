package com.muhend.nfcagent.service;

import net.sf.scuba.smartcards.CardService;
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

    private TerminalFactory getTerminalFactory() throws Exception {
        return TerminalFactory.getDefault();
    }

    private void fixPcscContext() {
        try {
            Class<?> pcscClass = Class.forName("sun.security.smartcardio.PCSCTerminals");
            java.lang.reflect.Field contextIdField = pcscClass.getDeclaredField("contextId");
            java.lang.reflect.Field unsafeField = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            unsafeField.setAccessible(true);
            sun.misc.Unsafe unsafe = (sun.misc.Unsafe) unsafeField.get(null);
            long fieldOffset = unsafe.staticFieldOffset(contextIdField);
            unsafe.putLong(unsafe.staticFieldBase(contextIdField), fieldOffset, 0L);
            System.out.println("PC/SC Context réparé automatiquement.");
        } catch (Throwable t) {
            System.err.println("Impossible de réparer le PC/SC: " + t.getMessage());
        }
    }

    public boolean isReaderPresent() {
        try {
            List<CardTerminal> terminals = getTerminalFactory().terminals().list();
            return !terminals.isEmpty();
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("SCARD_E_SERVICE_STOPPED")) {
                fixPcscContext();
                try {
                    return !getTerminalFactory().terminals().list().isEmpty();
                } catch(Exception ignored) {}
            }
            return false;
        }
    }

    public Map<String, Object> readIdentityCard(String documentNumber, String dateOfBirth, String dateOfExpiry) throws Exception {
        List<CardTerminal> terminals;
        try {
            terminals = getTerminalFactory().terminals().list();
        } catch (Exception e) {
            if (e.getCause() != null && e.getCause().getMessage().contains("SCARD_E_SERVICE_STOPPED")) {
                fixPcscContext();
                terminals = getTerminalFactory().terminals().list();
            } else {
                throw e;
            }
        }
        
        if (terminals.isEmpty()) {
            throw new Exception("Aucun lecteur de carte à puce détecté par Java.");
        }

        // On prend le premier lecteur disponible
        CardTerminal terminal = terminals.get(0);
        Card card = null;
        
        // Au lieu de isCardPresent (qui est passif), on force la connexion activement
        // pendant 5 secondes. Ça force le lecteur à émettre un champ NFC plus fort.
        long startTime = System.currentTimeMillis();
        while (System.currentTimeMillis() - startTime < 5000) {
            try {
                card = terminal.connect("*");
                break; // Connexion réussie !
            } catch (Exception e) {
                // On attend un peu et on réessaie
                try { Thread.sleep(500); } catch (Exception ignored) {}
            }
        }
        
        if (card == null) {
            throw new Exception("Carte introuvable. Veuillez déplacer la carte sur le lecteur et réessayer.");
        }
        
        // On se déconnecte de la connexion manuelle de test,
        // car TerminalCardService va l'ouvrir lui-même proprement.
        try { card.disconnect(false); } catch(Exception ignored) {}
        
        // On utilise TerminalCardService avec le TERMINAL (et non la carte)
        net.sf.scuba.smartcards.CardService cardService = new net.sf.scuba.smartcards.TerminalCardService(terminal);
        cardService.open();
        
        PassportService passportService = new PassportService(
                cardService,
                PassportService.NORMAL_MAX_TRANCEIVE_LENGTH,
                PassportService.DEFAULT_MAX_BLOCKSIZE,
                true,   // isSFIEnabled (Use Short File Identifiers for faster reads if possible)
                true    // isSMForSelectEnabled (Required for French CNI, SELECT must be wrapped in PACE SM)
        );

        passportService.open();

        try {
            // Clé BAC (Basic Access Control) dérivée de la MRZ
            System.out.println("====== TENTATIVE D'AUTHENTIFICATION ======");
            System.out.println("Doc Number : [" + documentNumber + "] (Longueur: " + documentNumber.length() + ")");
            System.out.println("Date Naissance : [" + dateOfBirth + "]");
            System.out.println("Date Expir. : [" + dateOfExpiry + "]");
            System.out.println("==========================================");

            BACKey bacKey = new BACKey(documentNumber, dateOfBirth, dateOfExpiry);
            
            boolean authenticated = false;
            
            // On essaie d'abord PACE (pour les nouvelles CNI européennes)
            try {
                System.out.println("Sélection du Master File (MF) pour lire EF.CardAccess...");
                passportService.sendSelectMF();
                
                System.out.println("Tentative de lecture de EF.CardAccess pour PACE...");
                net.sf.scuba.smartcards.CardFileInputStream cardAccessFile = passportService.getInputStream(PassportService.EF_CARD_ACCESS);
                org.jmrtd.lds.CardAccessFile caf = new org.jmrtd.lds.CardAccessFile(cardAccessFile);
                
                // Maintenant qu'on a le PACEInfo, on DOIT sélectionner l'Applet eMRTD pour lancer PACE !
                System.out.println("Sélection de l'Applet eMRTD (Sans demander le FCI pour éviter 6982)...");
                net.sf.scuba.smartcards.CommandAPDU selectApplet = new net.sf.scuba.smartcards.CommandAPDU(
                        0x00, 0xA4, 0x04, 0x00, 
                        new byte[]{(byte)0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01}
                );
                cardService.transmit(selectApplet);

                for (org.jmrtd.lds.SecurityInfo si : caf.getSecurityInfos()) {
                    if (si instanceof org.jmrtd.lds.PACEInfo) {
                        org.jmrtd.lds.PACEInfo paceInfo = (org.jmrtd.lds.PACEInfo) si;
                        System.out.println("PACE Supporté ! OID=" + paceInfo.getObjectIdentifier());
                        passportService.doPACE(
                            bacKey, 
                            paceInfo.getObjectIdentifier(), 
                            org.jmrtd.lds.PACEInfo.toParameterSpec(paceInfo.getParameterId()), 
                            paceInfo.getParameterId()
                        );
                        authenticated = true;
                        System.out.println("Authentification PACE réussie avec la MRZ !");
                        break;
                    }
                }
            } catch (Exception e) {
                System.out.println("PACE non disponible ou a échoué, fallback sur BAC: " + e.getMessage());
            }

            // Si PACE n'a pas marché ou n'est pas supporté, on tente BAC classique
            if (!authenticated) {
                try {
                    System.out.println("Tentative d'authentification BAC (Classique)...");
                    net.sf.scuba.smartcards.CommandAPDU selectApplet = new net.sf.scuba.smartcards.CommandAPDU(
                            0x00, 0xA4, 0x04, 0x00, 
                            new byte[]{(byte)0xA0, 0x00, 0x00, 0x02, 0x47, 0x10, 0x01}
                    );
                    cardService.transmit(selectApplet);
                    passportService.doBAC(bacKey);
                    authenticated = true;
                    System.out.println("Authentification BAC réussie !");
                } catch (Exception e) {
                    throw new Exception("L'accès a été refusé par la puce. Vérifiez les champs saisis ! (" + e.getMessage() + ")");
                }
            }

            // Lecture des groupes de données en utilisant les SFI (Short File Identifiers)
            // Cela évite d'envoyer des commandes "SELECT FILE" qui requièrent SM sur la CNI française.
            Map<String, Object> result = new HashMap<>();

            try {
                System.out.println("Lecture de DG1 (SFI_DG1)...");
                net.sf.scuba.smartcards.CardFileInputStream dg1In = passportService.getInputStream(PassportService.EF_DG1, PassportService.SFI_DG1);
                org.jmrtd.lds.icao.DG1File dg1File = new org.jmrtd.lds.icao.DG1File(dg1In);
                org.jmrtd.lds.icao.MRZInfo mrzInfo = dg1File.getMRZInfo();
                result.put("mrz", mrzInfo.toString());
                result.put("firstName", mrzInfo.getSecondaryIdentifier().replace("<", " ").trim());
                result.put("lastName", mrzInfo.getPrimaryIdentifier().replace("<", " ").trim());
                result.put("documentNumber", mrzInfo.getDocumentNumber());
                result.put("dateOfBirth", mrzInfo.getDateOfBirth());
                result.put("dateOfExpiry", mrzInfo.getDateOfExpiry());
                result.put("gender", mrzInfo.getGender().name());
                result.put("nationality", mrzInfo.getNationality());
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
