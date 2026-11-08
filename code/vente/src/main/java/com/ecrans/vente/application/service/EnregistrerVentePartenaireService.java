package com.ecrans.vente.application.service;

import com.ecrans.vente.application.exception.SeanceInconnue;
import com.ecrans.vente.application.port.in.DemandeBillet;
import com.ecrans.vente.application.port.in.EnregistrerVentePartenaire;
import com.ecrans.vente.application.port.in.VentePartenaire;
import com.ecrans.vente.application.port.out.Commandes;
import com.ecrans.vente.application.port.out.GrilleTarifaire;
import com.ecrans.vente.application.port.out.PublicateurEvenements;
import com.ecrans.vente.application.port.out.Seances;
import com.ecrans.vente.domaine.commande.Billet;
import com.ecrans.vente.domaine.commande.Commande;
import com.ecrans.vente.domaine.commande.Tarif;
import com.ecrans.vente.domaine.commun.CommandeId;
import com.ecrans.vente.domaine.commun.Montant;
import com.ecrans.vente.domaine.commun.PlaceId;
import com.ecrans.vente.domaine.evenement.VentePartenaireEnregistree;
import com.ecrans.vente.domaine.seance.Seance;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

public class EnregistrerVentePartenaireService implements EnregistrerVentePartenaire {

    private final Seances seances;
    private final Commandes commandes;
    private final GrilleTarifaire grille;
    private final PublicateurEvenements publicateur;
    private final Clock horloge;

    public EnregistrerVentePartenaireService(Seances seances, Commandes commandes, GrilleTarifaire grille,
                                             PublicateurEvenements publicateur, Clock horloge) {
        this.seances = seances;
        this.commandes = commandes;
        this.grille = grille;
        this.publicateur = publicateur;
        this.horloge = horloge;
    }

    @Override
    public CommandeId executer(VentePartenaire vente) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        CommandeId id = CommandeId.nouvelle();
        List<Billet> billets = new ArrayList<>();
        Set<PlaceId> demandees = new LinkedHashSet<>();
        for (DemandeBillet b : vente.billets()) {
            Montant prix = grille.prix(vente.seance(), b.categorie());
            billets.add(new Billet(b.place(), new Tarif(b.categorie(), prix)));
            demandees.add(b.place());
        }

        Commande commande = Commande.creer(id, vente.seance(), billets, maintenant);
        Seance seance = seances.parId(vente.seance())
                .orElseThrow(() -> new SeanceInconnue(vente.seance()));
        seance.bloquer(demandees, id, maintenant);
        seance.vendre(id, demandees, maintenant);
        commande.payer(maintenant);

        seances.enregistrer(seance);
        commandes.enregistrer(commande);

        commande.retirerEvenements().forEach(publicateur::publier);
        publicateur.publier(new VentePartenaireEnregistree(
                id,
                vente.partenaire(),
                vente.referencePartenaire(),
                vente.montantPaye(),
                maintenant
        ));

        return id;
    }
}
