package com.audit.auditaifx.service;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.Risque;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class AIService {

    private final Random random = new Random();

    /**
     * Analyse les données d'un rapport et génère une recommandation pertinente.
     */
    public Recommandation genererRecommandation(RapportAudit rapport) {
        if (rapport == null) return null;

        String description = rapport.getDescription() != null ? rapport.getDescription().toLowerCase() : "";
        String titre = rapport.getTitre() != null ? rapport.getTitre().toLowerCase() : "";

        String recoDesc;
        String priorite = "Moyenne";

        // Logique "IA" simple basée sur des mots-clés
        if (description.contains("sécurité") || titre.contains("sécurité") || description.contains("security")) {
            recoDesc = "Renforcer le contrôle d'accès et implémenter une authentification multifacteur (MFA) sur les systèmes critiques.";
            priorite = "Haute";
        } else if (description.contains("données") || description.contains("data") || description.contains("stockage")) {
            recoDesc = "Mettre en place un plan de sauvegarde automatisé et chiffrer les données au repos.";
            priorite = "Haute";
        } else if (description.contains("réseau") || description.contains("network")) {
            recoDesc = "Isoler les environnements de test de la production via une segmentation réseau stricte.";
            priorite = "Moyenne";
        } else if (description.contains("processus") || description.contains("manuel")) {
            recoDesc = "Automatiser les workflows d'approbation pour réduire les risques d'erreurs humaines.";
            priorite = "Basse";
        } else {
            // Recommandations génériques si aucun mot-clé n'est trouvé
            String[] generiques = {
                "Réaliser une revue trimestrielle des habilitations utilisateurs.",
                "Mettre à jour la documentation des procédures opérationnelles standard.",
                "Former le personnel aux bonnes pratiques d'hygiène informatique.",
                "Optimiser le suivi des correctifs de vulnérabilité logicielle."
            };
            recoDesc = generiques[random.nextInt(generiques.length)];
        }

        Recommandation reco = new Recommandation();
        reco.setDescription("[IA] " + recoDesc);
        reco.setPriorite(priorite);
        reco.setResolue(false);

        return reco;
    }

    public Risque genererRisque(RapportAudit rapport) {
        if (rapport == null) return null;

        String description = rapport.getDescription() != null ? rapport.getDescription().toLowerCase() : "";
        
        String risqueDesc;
        String niveau = "Moyen";
        String impact = "Impact modéré sur les opérations.";

        if (description.contains("sécurité") || description.contains("security")) {
            risqueDesc = "Risque d'accès non autorisé ou de fuite de données.";
            niveau = "Critique";
            impact = "Perte de confidentialité et non-conformité RGPD.";
        } else if (description.contains("réseau") || description.contains("network")) {
            risqueDesc = "Vulnérabilité aux attaques par déni de service (DDoS).";
            niveau = "Élevé";
            impact = "Indisponibilité des services critiques.";
        } else if (description.contains("données") || description.contains("data")) {
            risqueDesc = "Absence de redondance pour les bases de données critiques.";
            niveau = "Élevé";
            impact = "Perte irréversible de données en cas de panne matérielle.";
        } else {
            risqueDesc = "Manque de documentation à jour sur les procédures d'urgence.";
            niveau = "Faible";
            impact = "Ralentissement de la reprise d'activité après sinistre.";
        }

        return new Risque("[IA] " + risqueDesc, niveau, impact);
    }
}
