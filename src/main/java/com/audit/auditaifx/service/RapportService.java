package com.audit.auditaifx.service;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.StatutRapport;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import com.audit.auditaifx.model.Risque;
import java.sql.*;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class RapportService {

    private Connection conn;

    public RapportService() {
        this.conn = DatabaseConnection.getConnection();
        if (this.conn != null) {
            creerTablesInitiales();
        } else {
            System.err.println("❌ Impossible d'initialiser le service : Connexion BD nulle.");
        }
    }

    // ─── Créer tables si elles n'existent pas ────────────────

    private void creerTablesInitiales() {
        if (conn == null) return;
        try {
            Statement st = conn.createStatement();
            
            System.out.println("🔍 Vérification des tables...");
            boolean needRepair = false;
            try {
                st.executeQuery("SELECT 1 FROM rapport_audit LIMIT 1").close();
            } catch (SQLException e) {
                String msg = e.getMessage().toLowerCase();
                if (msg.contains("doesn't exist in engine") || msg.contains("table") && msg.contains("doesn't exist")) {
                    needRepair = true;
                }
            }

            if (needRepair) {
                System.out.println("⚠️ Réparation de la base de données en cours...");
                st.execute("DROP TABLE IF EXISTS recommandation");
                st.execute("DROP TABLE IF EXISTS rapport_audit");
                System.out.println("✅ Tables supprimées pour réinitialisation.");
            }

            st.execute("""
                CREATE TABLE IF NOT EXISTS audit_rapport (
                    id VARCHAR(36) PRIMARY KEY,
                    titre VARCHAR(200) NOT NULL,
                    auditeur VARCHAR(100) NOT NULL,
                    entite_auditee VARCHAR(100) NOT NULL,
                    statut VARCHAR(50) NOT NULL,
                    description TEXT,
                    date_creation DATE NOT NULL,
                    date_mise_a_jour DATE NOT NULL
                ) ENGINE=InnoDB;
            """);
            
            st.execute("""
                CREATE TABLE IF NOT EXISTS audit_reco (
                    id VARCHAR(36) PRIMARY KEY,
                    rapport_id VARCHAR(36) NOT NULL,
                    description TEXT NOT NULL,
                    priorite VARCHAR(20) NOT NULL,
                    resolue BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (rapport_id)
                        REFERENCES audit_rapport(id)
                        ON DELETE CASCADE
                ) ENGINE=InnoDB;
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS audit_risque (
                    id VARCHAR(50) PRIMARY KEY,
                    rapport_id VARCHAR(50),
                    description TEXT,
                    niveau VARCHAR(20),
                    impact TEXT,
                    FOREIGN KEY (rapport_id) REFERENCES audit_rapport(id) ON DELETE CASCADE
                )
            """);

            System.out.println("✅ Structure de la base de données opérationnelle.");
        } catch (SQLException e) {
            System.err.println("❌ Erreur critique lors de l'initialisation DB : " + e.getMessage());
            e.printStackTrace();
        }
    }

    // ─── CRUD Rapport ─────────────────────────────────────────

    public void ajouter(RapportAudit r) {
        if (conn == null) return;
        String sql = """
            INSERT INTO audit_rapport
            (id, titre, auditeur, entite_auditee, statut, description,
             date_creation, date_mise_a_jour)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.setString(2, r.getTitre());
            ps.setString(3, r.getAuditeur());
            ps.setString(4, r.getEntiteAuditee());
            ps.setString(5, r.getStatut().name());
            ps.setString(6, r.getDescription());
            ps.setDate(7, Date.valueOf(r.getDateCreation()));
            ps.setDate(8, Date.valueOf(r.getDateMiseAJour()));
            ps.executeUpdate();

            // Sauvegarder les recommandations
            for (Recommandation reco : r.getRecommandations()) {
                ajouterRecommandation(r.getId(), reco);
            }
        } catch (SQLException e) {
            System.err.println("Erreur ajout rapport : " + e.getMessage());
        }
    }

    public void modifier(RapportAudit r) {
        if (conn == null) return;
        String sql = """
            UPDATE audit_rapport SET
                titre=?, auditeur=?, entite_auditee=?,
                statut=?, description=?, date_mise_a_jour=?
            WHERE id=?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getTitre());
            ps.setString(2, r.getAuditeur());
            ps.setString(3, r.getEntiteAuditee());
            ps.setString(4, r.getStatut().name());
            ps.setString(5, r.getDescription());
            ps.setDate(6, Date.valueOf(LocalDate.now()));
            ps.setString(7, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur modification rapport : " + e.getMessage());
        }
    }

    public void supprimer(RapportAudit r) {
        if (conn == null) return;
        String sql = "DELETE FROM audit_rapport WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression rapport : " + e.getMessage());
        }
    }

    public ObservableList<RapportAudit> getTous() {
        ObservableList<RapportAudit> liste = FXCollections.observableArrayList();
        if (conn == null) return liste;
        String sql = "SELECT * FROM audit_rapport";
        try (Statement st = conn.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                RapportAudit r = new RapportAudit();
                r.setId(rs.getString("id"));
                r.setTitre(rs.getString("titre"));
                r.setAuditeur(rs.getString("auditeur"));
                r.setEntiteAuditee(rs.getString("entite_auditee"));
                r.setStatut(StatutRapport.valueOf(rs.getString("statut")));
                r.setDescription(rs.getString("description"));
                r.setDateMiseAJour(rs.getDate("date_mise_a_jour").toLocalDate());
                r.setRecommandations(getRecommandations(r.getId()));
                r.setRisques(getRisques(r.getId()));
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture rapports : " + e.getMessage());
        }
        return liste;
    }

    // ─── CRUD Risque ──────────────────────────────────────────

    public void ajouterRisque(String rapportId, Risque risque) {
        if (conn == null) return;
        String sql = "INSERT INTO audit_risque (id, rapport_id, description, niveau, impact) VALUES (?, ?, ?, ?, ?)";
        String newId = UUID.randomUUID().toString();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, rapportId);
            ps.setString(3, risque.getDescription());
            ps.setString(4, risque.getNiveau());
            ps.setString(5, risque.getImpact());
            ps.executeUpdate();
            risque.setId(newId);
        } catch (SQLException e) {
            System.err.println("Erreur ajout risque : " + e.getMessage());
        }
    }

    public List<Risque> getRisques(String rapportId) {
        List<Risque> liste = new ArrayList<>();
        if (conn == null) return liste;
        String sql = "SELECT * FROM audit_risque WHERE rapport_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rapportId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Risque r = new Risque(
                        rs.getString("description"),
                        rs.getString("niveau"),
                        rs.getString("impact")
                );
                r.setId(rs.getString("id"));
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture risques : " + e.getMessage());
        }
        return liste;
    }

    public void supprimerRisque(String risqueId) {
        String sql = "DELETE FROM audit_risque WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, risqueId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression risque : " + e.getMessage());
        }
    }

    // ─── CRUD Recommandation ──────────────────────────────────

    public void ajouterRecommandation(String rapportId, Recommandation reco) {
        if (conn == null) return;
        String sql = "INSERT INTO audit_reco (id, rapport_id, description, priorite, resolue) VALUES (?, ?, ?, ?, ?)";
        String newId = UUID.randomUUID().toString();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, newId);
            ps.setString(2, rapportId);
            ps.setString(3, reco.getDescription());
            ps.setString(4, reco.getPriorite());
            ps.setBoolean(5, reco.isResolue());
            ps.executeUpdate();
            reco.setId(newId);
        } catch (SQLException e) {
            System.err.println("Erreur ajout recommandation : " + e.getMessage());
        }
    }

    public void modifierRecommandation(Recommandation reco) {
        String sql = """
            UPDATE audit_reco
            SET description=?, priorite=?, resolue=?
            WHERE id=?
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, reco.getDescription());
            ps.setString(2, reco.getPriorite());
            ps.setBoolean(3, reco.isResolue());
            ps.setString(4, reco.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur modification recommandation : " + e.getMessage());
        }
    }

    public void supprimerRecommandation(String recoId) {
        String sql = "DELETE FROM audit_reco WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression recommandation : " + e.getMessage());
        }
    }

    public java.util.List<Recommandation> getRecommandations(String rapportId) {
        java.util.List<Recommandation> liste = new java.util.ArrayList<>();
        if (conn == null) return liste;
        String sql = "SELECT * FROM audit_reco WHERE rapport_id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, rapportId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Recommandation reco = new Recommandation(
                        rs.getString("description"),
                        rs.getString("priorite")
                );
                reco.setId(rs.getString("id"));
                reco.setResolue(rs.getBoolean("resolue"));
                liste.add(reco);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture recommandations : " + e.getMessage());
        }
        return liste;
    }

    // ─── Données de test ──────────────────────────────────────

    public void chargerDonneesTest() {
        if (conn == null) return;
        // On ne charge que si la table est vide
        if (!getTous().isEmpty()) return;

        // 1. Audit Sécurité SI
        RapportAudit r1 = new RapportAudit("Audit Sécurité SI", "Ali Ben Salem", "DSI");
        r1.setStatut(StatutRapport.EN_COURS);
        r1.setDescription("Analyse de la sécurité périmétrique et des accès VPN.");
        r1.ajouterRecommandation(new Recommandation("Mettre à jour les pare-feux", "Haute"));
        r1.ajouterRecommandation(new Recommandation("Activer l'authentification 2FA", "Haute"));
        ajouter(r1);

        // 2. Audit Financier Q1
        RapportAudit r2 = new RapportAudit("Audit Financier Q1", "Sara Mansour", "Finance");
        r2.setStatut(StatutRapport.FINALISE);
        r2.setDescription("Vérification des processus de facturation manuels.");
        r2.ajouterRecommandation(new Recommandation("Réviser la politique des dépenses", "Moyenne"));
        r2.ajouterRecommandation(new Recommandation("Automatiser les rapports comptables", "Faible"));
        ajouter(r2);

        // 3. Audit RH 2024
        RapportAudit r3 = new RapportAudit("Audit RH 2024", "Mohamed Triki", "Ressources Humaines");
        r3.setStatut(StatutRapport.BROUILLON);
        r3.setDescription("Audit des dossiers du personnel et stockage physique.");
        r3.ajouterRecommandation(new Recommandation("Mettre à jour les contrats", "Haute"));
        r3.ajouterRecommandation(new Recommandation("Digitaliser les dossiers", "Faible"));
        r3.ajouterRecommandation(new Recommandation("Former au RGPD", "Moyenne"));
        ajouter(r3);

        // 4. Audit Qualité ISO 9001
        RapportAudit r4 = new RapportAudit("Audit Qualité ISO 9001", "Fatma Riahi", "Production");
        r4.setStatut(StatutRapport.EN_COURS);
        r4.setDescription("Évaluation de la traçabilité des pièces sur la chaîne.");
        r4.ajouterRecommandation(new Recommandation("Système de traçabilité", "Haute"));
        r4.ajouterRecommandation(new Recommandation("Revoir contrôle qualité", "Moyenne"));
        ajouter(r4);

        // 5. Audit Performance IT
        RapportAudit r5 = new RapportAudit("Audit Performance IT", "Jean Dupont", "IT");
        r5.setStatut(StatutRapport.FINALISE);
        r5.setDescription("Optimisation des temps de réponse des serveurs critiques.");
        r5.ajouterRecommandation(new Recommandation("Migrer vers le Cloud", "Moyenne"));
        r5.ajouterRecommandation(new Recommandation("Optimiser les requêtes SQL", "Haute"));
        ajouter(r5);

        // 6. Audit Conformité RGPD
        RapportAudit r6 = new RapportAudit("Audit Conformité RGPD", "Sophie Laurent", "Juridique");
        r6.setStatut(StatutRapport.EN_COURS);
        r6.setDescription("Vérification de la gestion des données personnelles.");
        r6.ajouterRecommandation(new Recommandation("Nommer un DPO", "Haute"));
        r6.ajouterRecommandation(new Recommandation("Mettre à jour les mentions légales", "Faible"));
        ajouter(r6);

        // 7. Audit Logistique
        RapportAudit r7 = new RapportAudit("Audit Logistique", "Marc Durand", "Entrepôt");
        r7.setStatut(StatutRapport.BROUILLON);
        r7.setDescription("Analyse de la gestion des stocks et des flux.");
        r7.ajouterRecommandation(new Recommandation("Installer un WMS", "Haute"));
        r7.ajouterRecommandation(new Recommandation("Réorganiser le zonage", "Moyenne"));
        ajouter(r7);

        // 8. Audit Marketing Digital
        RapportAudit r8 = new RapportAudit("Audit Marketing Digital", "Kevin Martin", "Marketing");
        r8.setStatut(StatutRapport.FINALISE);
        r8.setDescription("Évaluation du ROI des campagnes publicitaires.");
        r8.ajouterRecommandation(new Recommandation("Changer d'agence média", "Faible"));
        r8.ajouterRecommandation(new Recommandation("Améliorer le tracking", "Moyenne"));
        ajouter(r8);

        // 9. Audit Maintenance
        RapportAudit r9 = new RapportAudit("Audit Maintenance", "Paul Lefebvre", "Technique");
        r9.setStatut(StatutRapport.EN_COURS);
        r9.setDescription("Vérification du plan de maintenance préventive.");
        r9.ajouterRecommandation(new Recommandation("Remplacer les machines obsolètes", "Haute"));
        r9.ajouterRecommandation(new Recommandation("Planifier les arrêts techniques", "Moyenne"));
        ajouter(r9);

        // 10. Audit Sécurité Physique
        RapportAudit r10 = new RapportAudit("Audit Sécurité Physique", "Eric Petit", "Sécurité");
        r10.setStatut(StatutRapport.FINALISE);
        r10.setDescription("Contrôle des accès aux bâtiments et vidéosurveillance.");
        r10.ajouterRecommandation(new Recommandation("Ajouter des caméras", "Moyenne"));
        r10.ajouterRecommandation(new Recommandation("Badgeage biométrique", "Haute"));
        r10.ajouterRecommandation(new Recommandation("Remplacer les serrures", "Moyenne"));
        ajouter(r10);
    }
}