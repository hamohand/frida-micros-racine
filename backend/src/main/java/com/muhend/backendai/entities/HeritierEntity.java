package com.muhend.backendai.entities;

import jakarta.persistence.*;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties({ "hibernateLazyInitializer", "handler" })

@Entity
@Getter
@Setter
@Table(name = "heritier")
public class HeritierEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id", nullable = false)
    private Long id;

    @Size(max = 255)
    @Column(name = "numFrida")
    private String numFrida = "";

    @Size(max = 255)
    @Column(name = "numParente")
    private String numParente = "";

    @Size(max = 255)
    @Column(name = "adresse")
    private String adresse = "";

    @Size(max = 255)
    @Column(name = "profession")
    private String profession = "";

    @Column(name = "coefPart")
    private Float coefPart;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumn(name = "identite_id")
    private IdentitesEntity identite;

    // La relation unidirectionnelle avec FridaEntity est gérée côté FridaEntity
    // via @OneToMany + @JoinColumn(name = "frida_id").

    @Transient
    public String getCategorieArabe() {
        if (numParente == null) return "";
        String s = identite != null && identite.getSexe() != null ? identite.getSexe() : "";
        boolean estHomme = "M".equalsIgnoreCase(s) || "MASCULIN".equalsIgnoreCase(s) || "ذكر".equals(s);
        return switch (numParente) {
            case "02" -> estHomme ? "زوج" : "زوجة";
            case "03" -> estHomme ? "إبن" : "بنت";
            case "04" -> estHomme ? "أب" : "أم";
            case "05" -> estHomme ? "أخ" : "أخت";
            case "06" -> estHomme ? "عم" : "عمة";
            case "07" -> estHomme ? "إبن عم" : "إبنة عم";
            case "08" -> "جد";
            case "09" -> "إبن إبن";
            case "10" -> "بنت إبن";
            case "11" -> "جدة";
            default -> "";
        };
    }

    @Transient
    public String getPartFormattee() {
        if (coefPart == null) return "";
        return String.valueOf(coefPart);
    }
}