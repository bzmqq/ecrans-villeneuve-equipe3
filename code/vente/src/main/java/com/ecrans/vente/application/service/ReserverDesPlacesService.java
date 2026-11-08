package com.ecrans.vente.application.service;

import com.ecrans.vente.application.exception.SeanceInconnue;
import com.ecrans.vente.application.port.in.DemandeBillet;
import com.ecrans.vente.application.port.in.DemandeReservation;
import com.ecrans.vente.application.port.in.Reservation;
import com.ecrans.vente.application.port.in.ReserverDesPlaces;
import com.ecrans.vente.application.port.out.Commandes;
import com.ecrans.vente.application.port.out.ConflitDeVersion;
import com.ecrans.vente.application.port.out.GrilleTarifaire;
import com.ecrans.vente.application.port.out.Paiements;
import com.ecrans.vente.application.port.out.Seances;
import com.ecrans.vente.domaine.commande.Billet;
import com.ecrans.vente.domaine.commande.Commande;
import com.ecrans.vente.domaine.commande.Tarif;
import com.ecrans.vente.domaine.commun.CommandeId;
import com.ecrans.vente.domaine.commun.Montant;
import com.ecrans.vente.domaine.commun.PlaceId;
import com.ecrans.vente.domaine.seance.Seance;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Orchestration seulement : charger, faire décider le domaine, enregistrer, puis appeler le PSP.
 *
 * Choix d'agrégats (ADR-0002, option B) : Seance et Commande sont écrits dans la même transaction.
 * L'appel au PSP est fait APRÈS : on ne garde pas une transaction ouverte pendant un appel réseau.
 */
public class ReserverDesPlacesService implements ReserverDesPlaces {

    /** Bonus : nombre d'essais quand une autre vente a modifié la séance en même temps. */
    static final int ESSAIS_MAX = 3;

    private final Seances seances;
    private final Commandes commandes;
    private final GrilleTarifaire grille;
    private final Paiements paiements;
    private final Clock horloge;

    public ReserverDesPlacesService(Seances seances, Commandes commandes, GrilleTarifaire grille,
                                    Paiements paiements, Clock horloge) {
        this.seances = seances;
        this.commandes = commandes;
        this.grille = grille;
        this.paiements = paiements;
        this.horloge = horloge;
    }

    @Override
    public Reservation executer(DemandeReservation demande) {
        LocalDateTime maintenant = LocalDateTime.now(horloge);
        CommandeId id = CommandeId.nouvelle();
        List<Billet> billets = new ArrayList<>();
        Set<PlaceId> demandees = new LinkedHashSet<>();
        for (DemandeBillet b : demande.billets()) {
            Montant prix = grille.prix(demande.seance(), b.categorie());
            billets.add(new Billet(b.place(), new Tarif(b.categorie(), prix)));
            demandees.add(b.place());
        }
        Seance seance = seances.parId(demande.seance())
                .orElseThrow(() -> new SeanceInconnue(demande.seance()));
        LocalDateTime expireA = seance.bloquer(demandees, id, maintenant);
        Commande commande = Commande.creer(id, demande.seance(), billets, expireA);
        seances.enregistrer(seance);
        commandes.enregistrer(commande);
        String urlPaiement = paiements.demanderPaiement(id, commande.total());
        return new Reservation(id, commande.total(), urlPaiement, expireA);
    }
}
