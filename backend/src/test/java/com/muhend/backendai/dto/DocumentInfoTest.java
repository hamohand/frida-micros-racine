package com.muhend.backendai.dto;

import com.muhend.backendai.enums.DocumentType;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Le nom d'entité OCR choisi à l'upload est ajouté au nom du sous-dossier ("03_en_en_01_qrcode_01").
 * Depuis le 2026-09-11, l'entité par défaut des extraits de naissance est en_01_qrcode_01 :
 * ses "_" supplémentaires ne doivent ni tronquer le nom ni fausser le type de document.
 */
class DocumentInfoTest {

    @Test
    void entiteQrCode_NomCompletConserve() {
        DocumentInfo info = DocumentInfo.fromFolderName("03_en_en_01_qrcode_01");

        assertEquals(DocumentType.EXTRAIT_NAISSANCE, info.getDocumentType());
        assertEquals("en_01_qrcode_01", info.getEntityName());
    }

    @Test
    void dossierSansEntite_EntiteParDefautQrCode() {
        DocumentInfo info = DocumentInfo.fromFolderName("1_en");

        assertNull(info.getEntityName());
        assertEquals("en_01_qrcode_01", info.getDocumentType().getOcrEntityId());
    }

    @Test
    void ancienneEntite_ToujoursAcceptee() {
        DocumentInfo info = DocumentInfo.fromFolderName("03_en_en_01");

        assertEquals(DocumentType.EXTRAIT_NAISSANCE, info.getDocumentType());
        assertEquals("en_01", info.getEntityName());
    }

    @Test
    void versoCni_NonAffecte() {
        DocumentInfo info = DocumentInfo.fromFolderName("02_cni_cni_01_verso");

        assertEquals(DocumentType.CNI, info.getDocumentType());
        assertEquals("cni_01_verso", info.getEntityName());
    }
}
