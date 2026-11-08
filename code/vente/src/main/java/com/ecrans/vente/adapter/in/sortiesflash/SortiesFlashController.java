package com.ecrans.vente.adapter.in.sortiesflash;

import com.ecrans.vente.application.port.in.EnregistrerVentePartenaire;
import com.ecrans.vente.application.port.in.VentePartenaire;
import com.ecrans.vente.domaine.commun.CommandeId;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class SortiesFlashController {

    private final AclSortiesFlash acl;
    private final EnregistrerVentePartenaire enregistrerVentePartenaire;

    public SortiesFlashController(AclSortiesFlash acl, EnregistrerVentePartenaire enregistrerVentePartenaire) {
        this.acl = acl;
        this.enregistrerVentePartenaire = enregistrerVentePartenaire;
    }

    @PostMapping("/partner/v2/orders")
    @ResponseStatus(HttpStatus.CREATED)
    public ReponseSortiesFlash commander(@RequestBody CommandeSortiesFlash commande) {
        VentePartenaire vente = acl.traduire(commande);
        CommandeId commandeId = enregistrerVentePartenaire.executer(vente);
        return new ReponseSortiesFlash(commande.partner_order_id(), commandeId.valeur(), "CONFIRMED");
    }

    public record ReponseSortiesFlash(String partner_order_id, String cinema_order_id, String status) {
    }
}
