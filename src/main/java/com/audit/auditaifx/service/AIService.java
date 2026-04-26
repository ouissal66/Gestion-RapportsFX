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
        if (rapport == null)
            return null;

        String description = rapport.getDescription() != null ? rapport.getDescription().toLowerCase() : "";
        String titre = rapport.getTitre() != null ? rapport.getTitre().toLowerCase() : "";

        String recoDesc;
        String priorite = "Moyenne";

        // Logique "IA" simple basée sur des mots-clés
        if (description.contains("sécurité") || titre.contains("sécurité") || description.contains("security")) {
            recoDesc = "Renforcer le contrôle d'accès et implémenter une authentification multifacteur (MFA) sur les systèmes critiques.";
            priorite = "Haute";
        } else if (description.contains("données") || description.contains("data")
                || description.contains("stockage")) {
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
        if (rapport == null)
            return null;

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

    /**
     * Analyse tous les rapports et retourne celui à traiter en priorité.
     * Score basé sur : recommandations non résolues (x3 si Haute, x2 si Moyenne),
     * nombre de risques (x4), et statut (EN_COURS bonus).
     */
    public PrioriteResult calculeRapportPrioritaire(List<com.audit.auditaifx.model.RapportAudit> rapports) {
        if (rapports == null || rapports.isEmpty()) return null;

        com.audit.auditaifx.model.RapportAudit best = null;
        int bestScore = -1;
        String bestRaison = "";

        for (com.audit.auditaifx.model.RapportAudit r : rapports) {
            int score = 0;
            int nbHaute = 0, nbMoyenne = 0, nbNonResolues = 0;

            for (com.audit.auditaifx.model.Recommandation reco : r.getRecommandations()) {
                if (!reco.isResolue()) {
                    nbNonResolues++;
                    String p = reco.getPriorite() != null ? reco.getPriorite().toLowerCase() : "";
                    if (p.contains("haut")) { score += 3; nbHaute++; }
                    else if (p.contains("moy")) { score += 2; nbMoyenne++; }
                    else score += 1;
                }
            }

            // Risques comptent beaucoup
            int nbRisques = r.getRisques() != null ? r.getRisques().size() : 0;
            score += nbRisques * 4;

            // Bonus statut EN_COURS (déjà en cours = urgent)
            if (r.getStatut() == com.audit.auditaifx.model.StatutRapport.EN_COURS) score += 5;
            // Brouillon = encore plus urgent car jamais commencé
            if (r.getStatut() == com.audit.auditaifx.model.StatutRapport.BROUILLON) score += 3;

            if (score > bestScore) {
                bestScore = score;
                best = r;
                // Construire l'explication
                List<String> raisons = new java.util.ArrayList<>();
                if (nbHaute > 0) raisons.add(nbHaute + " reco(s) haute priorité non résolue(s)");
                if (nbMoyenne > 0) raisons.add(nbMoyenne + " reco(s) priorité moyenne en attente");
                if (nbRisques > 0) raisons.add(nbRisques + " risque(s) détecté(s)");
                if (r.getStatut() == com.audit.auditaifx.model.StatutRapport.EN_COURS) raisons.add("statut EN COURS");
                if (r.getStatut() == com.audit.auditaifx.model.StatutRapport.BROUILLON) raisons.add("encore en BROUILLON");
                bestRaison = raisons.isEmpty() ? "Rapport nécessitant une attention." : String.join(" • ", raisons);
            }
        }

        return best != null ? new PrioriteResult(best, bestScore, bestRaison) : null;
    }

    /** Résultat de l'analyse de priorité IA */
    public static class PrioriteResult {
        public final com.audit.auditaifx.model.RapportAudit rapport;
        public final int score;
        public final String raison;

        public PrioriteResult(com.audit.auditaifx.model.RapportAudit rapport, int score, String raison) {
            this.rapport = rapport;
            this.score = score;
            this.raison = raison;
        }
    }
}