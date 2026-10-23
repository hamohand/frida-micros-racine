package com.muhend.backendai.service;

import com.muhend.backendai.entities.HeritierEntity;
import com.muhend.backendai.repository.HeritierRepo;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;

import java.util.List;

@Profile("!calc-only")
@Service
@AllArgsConstructor
public class HeritierService {
    @Autowired
    private final HeritierRepo heritierRepo;

    @Autowired
    private final com.muhend.backendai.repository.IdentitesRepo identitesRepo;

    public List<HeritierEntity> listeHeritiers(String numFrida) {
        List<HeritierEntity> list = heritierRepo.listeHeritiers(numFrida);
        if (list != null && identitesRepo != null) {
            for (HeritierEntity h : list) {
                com.muhend.backendai.entities.IdentitesEntity idH = h.getIdentite();
                if (idH != null && (idH.getLieuNaissance() == null || idH.getLieuNaissance().isBlank())) {
                    String recovered = resolveLieuNaissance(idH);
                    if (!recovered.isEmpty()) {
                        idH.setLieuNaissance(recovered);
                        try {
                            identitesRepo.save(idH);
                        } catch (Exception ignored) {}
                    }
                }
            }
        }
        return list;
    }

    private String resolveLieuNaissance(com.muhend.backendai.entities.IdentitesEntity idH) {
        try {
            if (idH.getNin() != null && !idH.getNin().isBlank()) {
                for (com.muhend.backendai.entities.IdentitesEntity cand : identitesRepo.findByNin(idH.getNin().trim())) {
                    if (cand.getLieuNaissance() != null && !cand.getLieuNaissance().isBlank()) {
                        return cand.getLieuNaissance().trim();
                    }
                }
            }
            if (idH.getNom() != null && !idH.getNom().isBlank() && idH.getPrenom() != null && !idH.getPrenom().isBlank()) {
                for (com.muhend.backendai.entities.IdentitesEntity cand : identitesRepo.findByNomAndPrenom(idH.getNom().trim(), idH.getPrenom().trim())) {
                    if (cand.getLieuNaissance() != null && !cand.getLieuNaissance().isBlank()) {
                        return cand.getLieuNaissance().trim();
                    }
                }
            }
        } catch (Exception ignored) {}
        return "";
    }
}
