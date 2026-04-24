-- SQL Script to populate AuditAI Database with 10 reports and recommendations
-- Use this in phpMyAdmin if the application cannot initialize the database.

CREATE DATABASE IF NOT EXISTS auditai_db;
USE auditai_db;

-- Drop tables if they are corrupted "in engine"
DROP TABLE IF EXISTS recommandation;
DROP TABLE IF EXISTS rapport_audit;

-- Create tables
CREATE TABLE rapport_audit (
    id VARCHAR(36) PRIMARY KEY,
    titre VARCHAR(200) NOT NULL,
    auditeur VARCHAR(100) NOT NULL,
    entite_auditee VARCHAR(100) NOT NULL,
    statut VARCHAR(50) NOT NULL,
    description TEXT,
    date_creation DATE NOT NULL,
    date_mise_a_jour DATE NOT NULL
) ENGINE=InnoDB;

CREATE TABLE recommandation (
    id VARCHAR(36) PRIMARY KEY,
    rapport_id VARCHAR(36) NOT NULL,
    description TEXT NOT NULL,
    priorite VARCHAR(20) NOT NULL,
    resolue BOOLEAN DEFAULT FALSE,
    FOREIGN KEY (rapport_id) REFERENCES rapport_audit(id) ON DELETE CASCADE
) ENGINE=InnoDB;

-- Populate Data
INSERT INTO rapport_audit (id, titre, auditeur, entite_auditee, statut, description, date_creation, date_mise_a_jour) VALUES
('R1', 'Audit Sécurité SI', 'Ali Ben Salem', 'DSI', 'EN_COURS', 'Analyse de la sécurité périmétrique et des accès VPN.', CURDATE(), CURDATE()),
('R2', 'Audit Financier Q1', 'Sara Mansour', 'Finance', 'FINALISE', 'Vérification des processus de facturation manuels.', CURDATE(), CURDATE()),
('R3', 'Audit RH 2024', 'Mohamed Triki', 'Ressources Humaines', 'BROUILLON', 'Audit des dossiers du personnel et stockage physique.', CURDATE(), CURDATE()),
('R4', 'Audit Qualité ISO 9001', 'Fatma Riahi', 'Production', 'EN_COURS', 'Évaluation de la traçabilité des pièces sur la chaîne.', CURDATE(), CURDATE()),
('R5', 'Audit Performance IT', 'Jean Dupont', 'IT', 'FINALISE', 'Optimisation des temps de réponse des serveurs critiques.', CURDATE(), CURDATE()),
('R6', 'Audit Conformité RGPD', 'Sophie Laurent', 'Juridique', 'EN_COURS', 'Vérification de la gestion des données personnelles.', CURDATE(), CURDATE()),
('R7', 'Audit Logistique', 'Marc Durand', 'Entrepôt', 'BROUILLON', 'Analyse de la gestion des stocks et des flux.', CURDATE(), CURDATE()),
('R8', 'Audit Marketing Digital', 'Kevin Martin', 'Marketing', 'FINALISE', 'Évaluation du ROI des campagnes publicitaires.', CURDATE(), CURDATE()),
('R9', 'Audit Maintenance', 'Paul Lefebvre', 'Technique', 'EN_COURS', 'Vérification du plan de maintenance préventive.', CURDATE(), CURDATE()),
('R10', 'Audit Sécurité Physique', 'Eric Petit', 'Sécurité', 'FINALISE', 'Contrôle des accès aux bâtiments et vidéosurveillance.', CURDATE(), CURDATE());

INSERT INTO recommandation (id, rapport_id, description, priorite, resolue) VALUES
(UUID(), 'R1', 'Mettre à jour les pare-feux', 'Haute', 0),
(UUID(), 'R1', 'Activer l\'authentification 2FA', 'Haute', 0),
(UUID(), 'R2', 'Réviser la politique des dépenses', 'Moyenne', 0),
(UUID(), 'R2', 'Automatiser les rapports comptables', 'Faible', 0),
(UUID(), 'R3', 'Mettre à jour les contrats', 'Haute', 0),
(UUID(), 'R3', 'Digitaliser les dossiers', 'Faible', 0),
(UUID(), 'R3', 'Former au RGPD', 'Moyenne', 0),
(UUID(), 'R4', 'Système de traçabilité', 'Haute', 0),
(UUID(), 'R4', 'Revoir contrôle qualité', 'Moyenne', 0),
(UUID(), 'R5', 'Migrer vers le Cloud', 'Moyenne', 0),
(UUID(), 'R5', 'Optimiser les requêtes SQL', 'Haute', 0),
(UUID(), 'R6', 'Nommer un DPO', 'Haute', 0),
(UUID(), 'R6', 'Mettre à jour les mentions légales', 'Faible', 0),
(UUID(), 'R7', 'Installer un WMS', 'Haute', 0),
(UUID(), 'R7', 'Réorganiser le zonage', 'Moyenne', 0),
(UUID(), 'R8', 'Changer d\'agence média', 'Faible', 0),
(UUID(), 'R8', 'Améliorer le tracking', 'Moyenne', 0),
(UUID(), 'R9', 'Remplacer les machines obsolètes', 'Haute', 0),
(UUID(), 'R9', 'Planifier les arrêts techniques', 'Moyenne', 0),
(UUID(), 'R10', 'Ajouter des caméras', 'Moyenne', 0),
(UUID(), 'R10', 'Badgeage biométrique', 'Haute', 0),
(UUID(), 'R10', 'Remplacer les serrures', 'Moyenne', 0);
