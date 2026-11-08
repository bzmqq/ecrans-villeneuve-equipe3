# Les Écrans de Villeneuve — Architecture de la billetterie

Projet final du module **Concevoir une application numérique** (B3).

**Dépôt de l'équipe** : https://github.com/bzmqq/ecrans-villeneuve-equipe3.git

## Équipe 3

| Membre | Étapes codées pendant le TP | Sections du DAT dont il ou elle est référent·e | Partie présentée à l'oral |
|---|---|---|---|
| **Pierre** | Étapes 3, 4, 5 (Services d'orchestration) | Sections 6, 7, 10 (Vue composants, dynamique, tests) | L'architecture hexagonale & le flux « réserver / payer » |
| **Akchayan** | Étapes 1, 2 (Agrégats Seance et Commande) | Sections 4, 8 (Domaine, Modèle, Invariants) | Le domaine, les agrégats & les règles métier B1 à B6 |
| **Maylès** | Étapes 7, 8 (Spring Boot, Partenaire SortiesFlash) | Sections 1, 2, 5, 9, 11 (Cadrage, Grille, ADR, Risques) | Le problème de mars, les ADRs & les hypothèses |

## Organisation du dépôt

```
ecrans-villeneuve/
├── README.md                              ← ce fichier : l'équipe, le lien du dépôt, les jalons
├── code/
│   └── vente/                             ← le cœur de la Vente de places, en Java (TP du J4)
│       ├── pom.xml
│       ├── README.md
│       ├── fournis/                       ← les tests des étapes 7 et 8, à copier
│       └── src/
│           ├── main/java/com/ecrans/vente/    ← domaine, application, adapters, config
│           └── test/java/com/ecrans/vente/    ← un dossier de tests par étape, bonus, scénario
└── docs/
    ├── DAT-Ecrans-de-Villeneuve.docx      ← le Document d'Architecture Technique (11 sections)
    ├── DAT-Ecrans-de-Villeneuve.pdf       ← son export PDF, produit pour le rendu
    └── diagrammes/                        ← par diagramme : source (.drawio, .puml) et export PNG
        ├── 1-contexte.png                 (section 3)
        ├── 2-context-map.png              (section 4)
        ├── 3-composants-vente.png         (section 6)
        ├── 4-sequence-reserver-payer.png  (section 7)
        └── 5-modele-vente.png             (section 8)
```

## Comment lancer le projet

### Exécuter tous les tests (65 tests)
```bash
cd code/vente
mvn test
```

### Lancer l'application Spring Boot
```bash
cd code/vente
mvn spring-boot:run
```
Ou directement en exécutant `com.ecrans.vente.VenteApplication` depuis IntelliJ.

### Swagger UI
Une fois l'application démarrée, l'interface interactive Swagger est disponible sur :
👉 http://localhost:8080/swagger-ui.html

---

## Jalons

- [x] **J4, fin de la phase 1** : glossaire, context map v0 et grille dans le DAT, poussés
- [x] **J4, fin de la phase 2** : tests verts pour les étapes 1 à 5, recherches de l'étape 6 sans résultat
- [x] **J4, fin de la phase 3** : `3-composants-vente` terminé, `4-sequence-reserver-payer` commencé
- [x] **Avant le J5** : étapes 7 et 8 faites (Spring Boot, ventes partenaires ; `ApiVenteTest` et `VentePartenaireApiTest` verts, Swagger répond), séquence terminée, DAT rédigé, composants mis à jour
- [x] **J5, finalisation** : dernières retouches, DAT exporté en PDF dans `docs/`, version taguée `v1.0` et poussée
- [x] **Rendu sur Moodle** avant les passages : le lien de ce dépôt (tag `v1.0` poussé, DAT en PDF dans `docs/`)
