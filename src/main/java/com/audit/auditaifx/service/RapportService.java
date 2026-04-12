package com.audit.auditaifx.service;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.StatutRapport;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;

import java.sql.*;
import java.time.LocalDate;
import java.util.UUID;

public class RapportService {

    private Connection conn;

    public RapportService() {
        this.conn = DatabaseConnection.getConnection();
        creerTablesInitiales();
    }

    // ─── Créer tables si elles n'existent pas ────────────────

    private void creerTablesInitiales() {
        try {
            Statement st = conn.createStatement();
            st.execute("""
                CREATE TABLE IF NOT EXISTS rapport_audit (
                    id VARCHAR(36) PRIMARY KEY,
                    titre VARCHAR(200) NOT NULL,
                    auditeur VARCHAR(100) NOT NULL,
                    entite_auditee VARCHAR(100) NOT NULL,
                    statut VARCHAR(50) NOT NULL,
                    description TEXT,
                    date_creation DATE NOT NULL,
                    date_mise_a_jour DATE NOT NULL
                )
            """);
            st.execute("""
                CREATE TABLE IF NOT EXISTS recommandation (
                    id VARCHAR(36) PRIMARY KEY,
                    rapport_id VARCHAR(36) NOT NULL,
                    description TEXT NOT NULL,
                    priorite VARCHAR(20) NOT NULL,
                    resolue BOOLEAN DEFAULT FALSE,
                    FOREIGN KEY (rapport_id)
                        REFERENCES rapport_audit(id)
                        ON DELETE CASCADE
                )
            """);
        } catch (SQLException e) {
            System.err.println("Erreur création tables : " + e.getMessage());
        }
    }

    // ─── CRUD Rapport ─────────────────────────────────────────

    public void ajouter(RapportAudit r) {
        String sql = """
            INSERT INTO rapport_audit
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
        String sql = """
            UPDATE rapport_audit SET
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
        String sql = "DELETE FROM rapport_audit WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, r.getId());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression rapport : " + e.getMessage());
        }
    }

    public ObservableList<RapportAudit> getTous() {
        ObservableList<RapportAudit> liste = FXCollections.observableArrayList();
        String sql = "SELECT * FROM rapport_audit";
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
                r.setDateCreation(rs.getDate("date_creation").toLocalDate());
                r.setDateMiseAJour(rs.getDate("date_mise_a_jour").toLocalDate());
                r.setRecommandations(getRecommandations(r.getId()));
                liste.add(r);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lecture rapports : " + e.getMessage());
        }
        return liste;
    }

    // ─── CRUD Recommandation ──────────────────────────────────

    public void ajouterRecommandation(String rapportId, Recommandation reco) {
        String sql = """
            INSERT INTO recommandation (id, rapport_id, description, priorite, resolue)
            VALUES (?, ?, ?, ?, ?)
        """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, UUID.randomUUID().toString());
            ps.setString(2, rapportId);
            ps.setString(3, reco.getDescription());
            ps.setString(4, reco.getPriorite());
            ps.setBoolean(5, reco.isResolue());
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur ajout recommandation : " + e.getMessage());
        }
    }

    public void modifierRecommandation(Recommandation reco) {
        String sql = """
            UPDATE recommandation
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
        String sql = "DELETE FROM recommandation WHERE id=?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, recoId);
            ps.executeUpdate();
        } catch (SQLException e) {
            System.err.println("Erreur suppression recommandation : " + e.getMessage());
        }
    }

    public java.util.List<Recommandation> getRecommandations(String rapportId) {
        java.util.List<Recommandation> liste = new java.util.ArrayList<>();
        String sql = "SELECT * FROM recommandation WHERE rapport_id=?";
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
        // Ne charge que si la table est vide
        if (!getTous().isEmpty()) return;

        RapportAudit r1 = new RapportAudit("Audit Sécurité SI", "Ali Ben Salem", "DSI");
        r1.setStatut(StatutRapport.EN_COURS);
        r1.ajouterRecommandation(new Recommandation("Mettre à jour les pare-feux", "Haute"));
        r1.ajouterRecommandation(new Recommandation("Activer l'authentification 2FA", "Haute"));
        r1.ajouterRecommandation(new Recommandation("Former les employés cybersécurité", "Moyenne"));
        ajouter(r1);

        RapportAudit r2 = new RapportAudit("Audit Financier Q1", "Sara Mansour", "Finance");
        r2.setStatut(StatutRapport.FINALISE);
        r2.ajouterRecommandation(new Recommandation("Réviser la politique des dépenses", "Moyenne"));
        r2.ajouterRecommandation(new Recommandation("Automatiser les rapports comptables", "Faible"));
        ajouter(r2);

        RapportAudit r3 = new RapportAudit("Audit RH 2024", "Mohamed Triki", "Ressources Humaines");
        r3.setStatut(StatutRapport.BROUILLON);
        r3.ajouterRecommandation(new Recommandation("Mettre à jour les contrats", "Haute"));
        r3.ajouterRecommandation(new Recommandation("Digitaliser les dossiers", "Faible"));
        ajouter(r3);

        RapportAudit r4 = new RapportAudit("Audit Qualité ISO 9001", "Fatma Riahi", "Production");
        r4.setStatut(StatutRapport.EN_COURS);
        r4.ajouterRecommandation(new Recommandation("Système de traçabilité", "Haute"));
        r4.ajouterRecommandation(new Recommandation("Revoir contrôle qualité", "Moyenne"));
        ajouter(r4);
    }
}