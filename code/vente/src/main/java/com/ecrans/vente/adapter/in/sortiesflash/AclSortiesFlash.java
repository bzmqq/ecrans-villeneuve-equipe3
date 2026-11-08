package com.ecrans.vente.adapter.in.sortiesflash;

import com.ecrans.vente.application.exception.SeanceInconnue;
import com.ecrans.vente.application.port.in.DemandeBillet;
import com.ecrans.vente.application.port.in.VentePartenaire;
import com.ecrans.vente.domaine.commande.Categorie;
import com.ecrans.vente.domaine.commun.Montant;
import com.ecrans.vente.domaine.commun.PlaceId;
import com.ecrans.vente.domaine.commun.SeanceId;

import java.util.ArrayList;
import java.util.List;

public class AclSortiesFlash {

    private final CorrespondanceSeances correspondances;

    public AclSortiesFlash(CorrespondanceSeances correspondances) {
        this.correspondances = correspondances;
    }

    public VentePartenaire traduire(CommandeSortiesFlash commande) {
        SeanceId seanceId = correspondances.trouverSeanceId(commande.show_ref())
                .orElseThrow(() -> new SeanceInconnue(new SeanceId(commande.show_ref())));

        if (commande.seats() == null || commande.ticket_types() == null
                || commande.seats().size() != commande.ticket_types().size()) {
            throw new IllegalArgumentException("Nombre de places et de types incompatible");
        }

        List<DemandeBillet> billets = new ArrayList<>();
        for (int i = 0; i < commande.seats().size(); i++) {
            String seat = commande.seats().get(i);
            String type = commande.ticket_types().get(i);
            Categorie categorie = switch (type) {
                case "ADULT" -> Categorie.PLEIN;
                case "STUDENT" -> Categorie.REDUIT;
                case "CHILD" -> Categorie.ENFANT;
                default -> throw new IllegalArgumentException("Type de billet inconnu: " + type);
            };
            billets.add(new DemandeBillet(PlaceId.de(seat), categorie));
        }

        return new VentePartenaire(
                "SortiesFlash",
                commande.partner_order_id(),
                seanceId,
                billets,
                Montant.centimes(commande.paid_amount_cents())
        );
    }
}
