package com.ecrans.vente.etape8;

import com.ecrans.vente.adapter.in.sortiesflash.AclSortiesFlash;
import com.ecrans.vente.adapter.in.sortiesflash.CommandeSortiesFlash;
import com.ecrans.vente.adapter.in.sortiesflash.CorrespondanceSeances;
import com.ecrans.vente.adapter.out.evenement.PublicateurEnMemoire;
import com.ecrans.vente.adapter.out.memoire.CommandesEnMemoire;
import com.ecrans.vente.adapter.out.memoire.SeancesEnMemoire;
import com.ecrans.vente.adapter.out.tarification.GrilleTarifaireFixe;
import com.ecrans.vente.application.exception.SeanceInconnue;
import com.ecrans.vente.application.port.in.VentePartenaire;
import com.ecrans.vente.application.service.EnregistrerVentePartenaireService;
import com.ecrans.vente.domaine.commande.Categorie;
import com.ecrans.vente.domaine.commande.Commande;
import com.ecrans.vente.domaine.commande.StatutCommande;
import com.ecrans.vente.domaine.commun.CommandeId;
import com.ecrans.vente.domaine.commun.Montant;
import com.ecrans.vente.domaine.commun.PlaceId;
import com.ecrans.vente.domaine.evenement.VentePartenaireEnregistree;
import com.ecrans.vente.domaine.seance.PlacesIndisponibles;
import com.ecrans.vente.domaine.seance.Seance;
import com.ecrans.vente.domaine.seance.StatutPlace;
import com.ecrans.vente.support.HorlogeReglable;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.ecrans.vente.support.Donnees.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SortiesFlashTest {

    private SeancesEnMemoire seances;
    private CommandesEnMemoire commandes;
    private PublicateurEnMemoire publicateur;
    private HorlogeReglable horloge;
    private CorrespondanceSeances correspondances;
    private AclSortiesFlash acl;
    private EnregistrerVentePartenaireService service;

    @BeforeEach
    void preparer() {
        seances = new SeancesEnMemoire();
        commandes = new CommandesEnMemoire();
        publicateur = new PublicateurEnMemoire();
        horloge = HorlogeReglable.a("20:00");
        seances.enregistrer(seanceS42());

        correspondances = new CorrespondanceSeances();
        correspondances.associer("SF-SHOW-42", S42);

        acl = new AclSortiesFlash(correspondances);
        service = new EnregistrerVentePartenaireService(seances, commandes, new GrilleTarifaireFixe(), publicateur, horloge);
    }

    @Test
    void traduit_une_commande_valide_vers_le_langage_du_domaine() {
        CommandeSortiesFlash externe = new CommandeSortiesFlash(
                "SF-SHOW-42",
                List.of("G7", "G8"),
                List.of("ADULT", "STUDENT"),
                "SF-999",
                1840
        );

        VentePartenaire vente = acl.traduire(externe);

        assertThat(vente.partenaire()).isEqualTo("SortiesFlash");
        assertThat(vente.referencePartenaire()).isEqualTo("SF-999");
        assertThat(vente.seance()).isEqualTo(S42);
        assertThat(vente.montantPaye()).isEqualTo(Montant.centimes(1840));
        assertThat(vente.billets()).hasSize(2);
        assertThat(vente.billets().get(0).place()).isEqualTo(PlaceId.de("G7"));
        assertThat(vente.billets().get(0).categorie()).isEqualTo(Categorie.PLEIN);
        assertThat(vente.billets().get(1).place()).isEqualTo(PlaceId.de("G8"));
        assertThat(vente.billets().get(1).categorie()).isEqualTo(Categorie.REDUIT);
    }

    @Test
    void signale_une_seance_partenaire_inconnue() {
        CommandeSortiesFlash externe = new CommandeSortiesFlash(
                "INCONNUE",
                List.of("G7"),
                List.of("ADULT"),
                "SF-999",
                1000
        );

        assertThatThrownBy(() -> acl.traduire(externe))
                .isInstanceOf(SeanceInconnue.class);
    }

    @Test
    void signale_un_type_de_billet_inconnu() {
        CommandeSortiesFlash externe = new CommandeSortiesFlash(
                "SF-SHOW-42",
                List.of("G7"),
                List.of("VIP"),
                "SF-999",
                1000
        );

        assertThatThrownBy(() -> acl.traduire(externe))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void enregistre_une_vente_partenaire_vendue_et_payee_immediatement() {
        CommandeSortiesFlash externe = new CommandeSortiesFlash(
                "SF-SHOW-42",
                List.of("G7"),
                List.of("ADULT"),
                "SF-999",
                1000
        );
        VentePartenaire vente = acl.traduire(externe);

        CommandeId commandeId = service.executer(vente);

        Seance seance = seances.parId(S42).orElseThrow();
        assertThat(seance.etat(PlaceId.de("G7")).statut()).isEqualTo(StatutPlace.VENDUE);
        assertThat(seance.etat(PlaceId.de("G7")).commande()).isEqualTo(commandeId);

        Commande commande = commandes.parId(commandeId).orElseThrow();
        assertThat(commande.statut()).isEqualTo(StatutCommande.PAYEE);

        assertThat(publicateur.publies()).anyMatch(e -> e instanceof VentePartenaireEnregistree);
    }

    @Test
    void refuse_la_vente_si_une_place_est_deja_prise() {
        Seance seance = seances.parId(S42).orElseThrow();
        seance.bloquer(places("G7"), C1, le12Novembre("20:00"));
        seances.enregistrer(seance);

        CommandeSortiesFlash externe = new CommandeSortiesFlash(
                "SF-SHOW-42",
                List.of("G7", "G8"),
                List.of("ADULT", "ADULT"),
                "SF-999",
                2000
        );
        VentePartenaire vente = acl.traduire(externe);

        assertThatThrownBy(() -> service.executer(vente))
                .isInstanceOf(PlacesIndisponibles.class);
    }
}
