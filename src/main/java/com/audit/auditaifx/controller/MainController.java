package com.audit.auditaifx.controller;

import com.audit.auditaifx.model.RapportAudit;
import com.audit.auditaifx.model.Recommandation;
import com.audit.auditaifx.model.Risque;
import com.audit.auditaifx.model.StatutRapport;
import com.audit.auditaifx.service.AIService;
import com.audit.auditaifx.service.RapportService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.chart.*;
import javafx.scene.layout.*;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import java.util.List;
import java.util.stream.Collectors;
import javafx.geometry.Pos;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.IndexedColors;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.ss.usermodel.HorizontalAlignment;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.File;
import java.io.FileOutputStream;
import com.lowagie.text.*;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import java.awt.Color;

public class MainController {

    // --- Sidebar & Views ---
    @FXML private ScrollPane dashboardView;
    @FXML private VBox allReportsView;
    @FXML private VBox detailView;

    // --- Dashboard Analytics ---
    @FXML private PieChart chartStatut;
    @FXML private BarChart<String, Number> chartPriorite;
    @FXML private VBox vboxRecentReports;

    // --- Rapports Table (Full List Page) ---
    @FXML private TableView<RapportAudit> rapportsTable;
    @FXML private TableColumn<RapportAudit, String> colTitre;
    @FXML private TableColumn<RapportAudit, String> colAuditeur;
    @FXML private TableColumn<RapportAudit, String> colEntite;
    @FXML private TableColumn<RapportAudit, String> colStatut;
    @FXML private TableColumn<RapportAudit, String> colDate;
    @FXML private TableColumn<RapportAudit, Integer> colNbReco;
    @FXML private TableColumn<RapportAudit, Void> colActionsRapport;
    @FXML private TextField txtSearchReports;

    // --- Dashboard Stats ---
    @FXML private Label lblDashTotalRapports;
    @FXML private Label lblDashTotalReco;
    @FXML private Label lblDashTaux;
    @FXML private ProgressIndicator progressIndicator;

    // --- Selected Report Info ---
    @FXML private Label lblSelectedTitre;
    @FXML private Label lblSelectedSub;

    // --- Recommandations ---
    @FXML private TableView<Recommandation> recoTable;
    @FXML private TableColumn<Recommandation, String> colRecoDesc;
    @FXML private TableColumn<Recommandation, String> colRecoPriorite;
    @FXML private TableColumn<Recommandation, Boolean> colRecoResolue;
    @FXML private TableColumn<Recommandation, Void> colActionsReco;

    // --- Labels + Boutons ---
    @FXML private Label lblRecommandations;
    @FXML private Label lblTotalRapports;
    @FXML private Label lblTotalReco;
    @FXML private Button btnAjouterReco;
    @FXML private Button btnAnalyserIA;
    @FXML private Button btnDetecterRisques;

    // --- Services ---
    private RapportService service = new RapportService();
    private AIService aiService = new AIService();
    private RapportAudit rapportSelectionne = null;

    @FXML
    public void initialize() {
        System.out.println("DEBUG Admin: Démarrage de initialize Multi-Page...");
        
        // Config table rapports (La "page" liste)
        colTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colAuditeur.setCellValueFactory(new PropertyValueFactory<>("auditeur"));
        colEntite.setCellValueFactory(new PropertyValueFactory<>("entiteAuditee"));
        colStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
        colDate.setCellValueFactory(new PropertyValueFactory<>("dateCreation"));
        colNbReco.setCellValueFactory(cellData ->
                new javafx.beans.property.SimpleIntegerProperty(
                        cellData.getValue().getRecommandations().size()).asObject());

        // Boutons actions dans la table
        ajouterBoutonsRapportsTable();

        // Écouteur de sélection (Simple sélection, pas de changement de vue)
        rapportsTable.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            rapportSelectionne = newVal;
        });

        // Colonnes recommandations (La "page" détails)
        colRecoDesc.setCellValueFactory(new PropertyValueFactory<>("description"));
        colRecoPriorite.setCellValueFactory(new PropertyValueFactory<>("priorite"));
        colRecoResolue.setCellValueFactory(new PropertyValueFactory<>("resolue"));

        // Boutons actions recommandations
        ajouterBoutonsReco();

        // Recherche dynamique
        txtSearchReports.textProperty().addListener((obs, oldText, newText) -> {
            filtrerRapports(newText);
        });

        // Charger données
        rafraichirListe();
        showDashboard();
    }

    private void filtrerRapports(String query) {
        if (query == null || query.isEmpty()) {
            rapportsTable.setItems(service.getTous());
        } else {
            String lower = query.toLowerCase();
            ObservableList<RapportAudit> filtres = service.getTous().filtered(r -> 
                r.getTitre().toLowerCase().contains(lower) || 
                r.getEntiteAuditee().toLowerCase().contains(lower) ||
                r.getAuditeur().toLowerCase().contains(lower)
            );
            rapportsTable.setItems(filtres);
        }
    }

    private void ajouterBoutonsRapportsTable() {
        colActionsRapport.setCellFactory(param -> new TableCell<>() {
            private final Button btnVoir = new Button("Voir");
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Supp");
            private final HBox container = new HBox(5, btnVoir, btnEdit, btnDel);

            {
                btnVoir.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnEdit.setStyle("-fx-background-color: #f1c40f; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-cursor: hand; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                
                btnVoir.setFocusTraversable(false);
                btnEdit.setFocusTraversable(false);
                btnDel.setFocusTraversable(false);
                
                btnVoir.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    rapportSelectionne = r;
                    showReportDetails(r);
                });
                btnEdit.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    ouvrirFormulaireRapport(r);
                });
                btnDel.setOnAction(e -> {
                    RapportAudit r = getTableView().getItems().get(getIndex());
                    supprimerRapport(r);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : container);
            }
        });
    }

    private void supprimerRapport(RapportAudit rapport) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer le rapport ?");
        confirm.setContentText("« " + rapport.getTitre() + " » sera supprimé définitivement.");
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimer(rapport);
                rafraichirListe();
            }
        });
    }

    @FXML
    public void showAllReports() {
        dashboardView.setVisible(false);
        detailView.setVisible(false);
        allReportsView.setVisible(true);
        rapportsTable.getSelectionModel().clearSelection();
    }

    @FXML
    public void showDashboard() {
        dashboardView.setVisible(true);
        detailView.setVisible(false);
        allReportsView.setVisible(false);
        mettreAJourCompteurs();
        
        ObservableList<RapportAudit> rapports = service.getTous();
        
        // 1. Statistiques Globales
        int totalReco = 0;
        int resolues = 0;
        int statusBrouillon = 0, statusEnCours = 0, statusTermine = 0;
        int pHaute = 0, pMoyenne = 0, pBasse = 0;

        for (RapportAudit r : rapports) {
            totalReco += r.getRecommandations().size();
            for (Recommandation reco : r.getRecommandations()) {
                if (reco.isResolue()) resolues++;
                String p = reco.getPriorite().toLowerCase();
                if (p.contains("haut")) pHaute++;
                else if (p.contains("moy")) pMoyenne++;
                else pBasse++;
            }
            
            switch (r.getStatut()) {
                case BROUILLON -> statusBrouillon++;
                case EN_COURS -> statusEnCours++;
                case FINALISE -> statusTermine++;
            }
        }

        lblDashTotalRapports.setText(String.valueOf(rapports.size()));
        lblDashTotalReco.setText(String.valueOf(totalReco));
        lblDashTaux.setText(totalReco > 0 ? (resolues * 100 / totalReco) + "%" : "0%");

        // 2. PieChart Statut
        chartStatut.getData().clear();
        chartStatut.getData().add(new PieChart.Data("Brouillon", statusBrouillon));
        chartStatut.getData().add(new PieChart.Data("En Cours", statusEnCours));
        chartStatut.getData().add(new PieChart.Data("Finalisé", statusTermine));

        // 3. BarChart Priorité
        chartPriorite.getData().clear();
        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.getData().add(new XYChart.Data<>("Haute", pHaute));
        series.getData().add(new XYChart.Data<>("Moyenne", pMoyenne));
        series.getData().add(new XYChart.Data<>("Basse", pBasse));
        chartPriorite.getData().add(series);

        // 4. Activités Récentes (3 derniers rapports)
        vboxRecentReports.getChildren().clear();
        List<RapportAudit> recent = rapports.stream()
                .sorted((r1, r2) -> r2.getDateCreation().compareTo(r1.getDateCreation()))
                .limit(3)
                .toList();

        for (RapportAudit r : recent) {
            HBox item = new HBox(15);
            item.setAlignment(Pos.CENTER_LEFT);
            item.setStyle("-fx-padding: 10; -fx-background-color: #f9f9f9; -fx-background-radius: 8; -fx-border-color: #eee; -fx-border-radius: 8;");
            
            VBox txt = new VBox(2);
            Label titre = new Label(r.getTitre());
            titre.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
            Label date = new Label("Créé le " + r.getDateCreation() + " | " + r.getStatut());
            date.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d;");
            txt.getChildren().addAll(titre, date);
            
            Region spacer = new Region();
            HBox.setHgrow(spacer, Priority.ALWAYS);
            
            Button btn = new Button("Voir");
            btn.setStyle("-fx-background-color: white; -fx-border-color: #3498db; -fx-text-fill: #3498db; -fx-border-radius: 5;");
            btn.setFocusTraversable(false);
            btn.setOnAction(e -> {
                rapportSelectionne = r;
                showReportDetails(r);
            });
            
            item.getChildren().addAll(txt, spacer, btn);
            vboxRecentReports.getChildren().add(item);
        }
    }

    private void rafraichirListe() {
        rapportsTable.setItems(service.getTous());
        mettreAJourCompteurs();
    }


    private void showReportDetails(RapportAudit rapport) {
        dashboardView.setVisible(false);
        allReportsView.setVisible(false);
        detailView.setVisible(true);
        
        lblSelectedTitre.setText(rapport.getTitre());
        lblSelectedSub.setText(rapport.getEntiteAuditee() + " — Audité par " + rapport.getAuditeur());
        
        recoTable.setItems(FXCollections.observableArrayList(rapport.getRecommandations()));
        
        btnAjouterReco.setDisable(false);
        btnAnalyserIA.setDisable(false);
        btnDetecterRisques.setDisable(false);
    }

    @FXML
    public void modifierRapportSelectionne() {
        if (rapportSelectionne != null) {
            ouvrirFormulaireRapport(rapportSelectionne);
        }
    }

    @FXML
    public void supprimerRapportSelectionne() {
        if (rapportSelectionne != null) {
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Supprimer le rapport ?");
            confirm.setContentText(rapportSelectionne.getTitre());
            confirm.showAndWait().ifPresent(res -> {
                if (res == ButtonType.OK) {
                    service.supprimer(rapportSelectionne);
                    rapportSelectionne = null;
                    rafraichirListe();
                    showAllReports();
                }
            });
        }
    }

    // ─── CRUD Rapports ────────────────────────────────────────

    @FXML
    public void ajouterRapport() {
        ouvrirFormulaireRapport(null);
    }


    private void ouvrirFormulaireRapport(RapportAudit rapport) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/audit/auditaifx/rapport-form-view.fxml"));
            Parent root = loader.load();

            RapportFormController controller = loader.getController();
            controller.setService(service);
            controller.setRapport(rapport);
            controller.setOnSave(() -> {
                rafraichirListe();
                if (rapportSelectionne != null) {
                    showReportDetails(rapportSelectionne);
                } else {
                    showAllReports();
                }
            });

            Stage stage = new Stage();
            stage.setTitle(rapport == null ? "Ajouter un rapport" : "Modifier le rapport");
            stage.setScene(new Scene(root, 500, 450));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            afficherErreur("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    private void voirDetailsRapport(RapportAudit rapport) {
        if (rapport == null) return;

        Alert details = new Alert(Alert.AlertType.INFORMATION);
        details.setTitle("Détails du Rapport");
        details.setHeaderText(rapport.getTitre());
        details.setContentText(
                "🏢 Entité: " + rapport.getEntiteAuditee() + "\n" +
                "👤 Auditeur: " + rapport.getAuditeur() + "\n" +
                "📅 Date: " + rapport.getDateCreation() + "\n" +
                "📊 Statut: " + rapport.getStatut() + "\n\n" +
                "📝 Description:\n" + (rapport.getDescription() != null ? rapport.getDescription() : "Aucune description")
        );
        details.getDialogPane().setPrefWidth(450);
        details.show();
    }

    @FXML
    public void exporterExcel() {
        ObservableList<RapportAudit> rapports = rapportsTable.getItems();
        if (rapports.isEmpty()) {
            afficherErreur("Aucun rapport à exporter.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le rapport Excel");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Excel Files", "*.xlsx"));
        fileChooser.setInitialFileName("Rapports_Audit_Complet.xlsx");
        
        File file = fileChooser.showSaveDialog(dashboardView.getScene().getWindow());
        if (file == null) return;

        try (Workbook workbook = new XSSFWorkbook();
             FileOutputStream fileOut = new FileOutputStream(file)) {
            
            Sheet sheet = workbook.createSheet("Rapports");

            // Style d'en-tête
            CellStyle headerStyle = workbook.createCellStyle();
            org.apache.poi.ss.usermodel.Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.INDIGO.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // En-têtes
            String[] headers = {"Titre", "Auditeur", "Entité", "Statut", "Date Création", "Nb Reco"};
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < headers.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(headers[i]);
                cell.setCellStyle(headerStyle);
            }

            // Données
            int rowNum = 1;
            for (RapportAudit r : rapports) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(r.getTitre());
                row.createCell(1).setCellValue(r.getAuditeur());
                row.createCell(2).setCellValue(r.getEntiteAuditee());
                row.createCell(3).setCellValue(r.getStatut().toString());
                row.createCell(4).setCellValue(r.getDateCreation().toString());
                row.createCell(5).setCellValue(r.getRecommandations().size());
            }

            // Auto-resize colonnes
            for (int i = 0; i < headers.length; i++) {
                sheet.autoSizeColumn(i);
            }

            workbook.write(fileOut);
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export Réussi");
            alert.setHeaderText(null);
            alert.setContentText("Le fichier Excel complet a été généré avec succès !");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de l'export Excel : " + e.getMessage());
        }
    }

    @FXML
    public void exporterPDF() {
        if (rapportSelectionne == null) {
            afficherErreur("Veuillez sélectionner un rapport dans le tableau pour l'exporter en PDF.");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter le Rapport en PDF");
        fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));
        String safeTitre = rapportSelectionne.getTitre().replaceAll("[^a-zA-Z0-9]", "_");
        fileChooser.setInitialFileName("Rapport_Audit_" + safeTitre + ".pdf");
        
        File file = fileChooser.showSaveDialog(dashboardView.getScene().getWindow());
        if (file == null) return;

        try {
            com.lowagie.text.Document document = new com.lowagie.text.Document(com.lowagie.text.PageSize.A4);
            PdfWriter.getInstance(document, new FileOutputStream(file));
            document.open();

            // Polices
            com.lowagie.text.Font titleFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 18, com.lowagie.text.Font.BOLD, Color.DARK_GRAY);
            com.lowagie.text.Font headerFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 12, com.lowagie.text.Font.BOLD, Color.WHITE);
            com.lowagie.text.Font normalFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA, 11);
            com.lowagie.text.Font boldFont = com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 11);

            // Titre
            Paragraph pTitle = new Paragraph("RAPPORT D'AUDIT", titleFont);
            pTitle.setAlignment(Element.ALIGN_CENTER);
            document.add(pTitle);
            document.add(new Paragraph(" ")); // Espace

            // Infos du rapport
            document.add(new Paragraph("Titre: " + rapportSelectionne.getTitre(), boldFont));
            document.add(new Paragraph("Auditeur: " + rapportSelectionne.getAuditeur(), normalFont));
            document.add(new Paragraph("Entité: " + rapportSelectionne.getEntiteAuditee(), normalFont));
            document.add(new Paragraph("Statut: " + rapportSelectionne.getStatut(), normalFont));
            document.add(new Paragraph("Date: " + rapportSelectionne.getDateCreation(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("Description:", boldFont));
            document.add(new Paragraph(rapportSelectionne.getDescription(), normalFont));
            document.add(new Paragraph(" "));
            document.add(new Paragraph("────────────────────────────────────────────────────────────────────────"));
            document.add(new Paragraph(" "));

            // Recommandations
            document.add(new Paragraph("RECOMMANDATIONS", com.lowagie.text.FontFactory.getFont(com.lowagie.text.FontFactory.HELVETICA_BOLD, 14)));
            document.add(new Paragraph(" "));

            PdfPTable table = new PdfPTable(3);
            table.setWidthPercentage(100);
            table.setSpacingBefore(10f);
            table.setSpacingAfter(10f);

            // En-têtes de table
            String[] headers = {"Description", "Priorité", "Résolue"};
            for (String h : headers) {
                com.lowagie.text.pdf.PdfPCell cell = new com.lowagie.text.pdf.PdfPCell(new Phrase(h, headerFont));
                cell.setBackgroundColor(Color.GRAY);
                cell.setPadding(5);
                table.addCell(cell);
            }

            // Données de table
            for (Recommandation reco : rapportSelectionne.getRecommandations()) {
                table.addCell(new Phrase(reco.getDescription(), normalFont));
                table.addCell(new Phrase(reco.getPriorite(), normalFont));
                table.addCell(new Phrase(reco.isResolue() ? "OUI" : "NON", normalFont));
            }

            document.add(table);
            document.close();

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Export PDF Réussi");
            alert.setHeaderText(null);
            alert.setContentText("Le rapport PDF a été généré avec succès !");
            alert.showAndWait();

        } catch (Exception e) {
            e.printStackTrace();
            afficherErreur("Erreur lors de l'export PDF : " + e.getMessage());
        }
    }

    // ─── CRUD Recommandations ─────────────────────────────────

    @FXML
    public void ajouterRecommandation() {
        if (rapportSelectionne != null)
            ouvrirFormulaireReco(null);
    }

    private void modifierRecommandation(Recommandation reco) {
        ouvrirFormulaireReco(reco);
    }

    private void supprimerRecommandation(Recommandation reco) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Supprimer la recommandation ?");
        confirm.setContentText(reco.getDescription());
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                service.supprimerRecommandation(reco.getId());
                rapportSelectionne.getRecommandations().remove(reco);
                recoTable.setItems(FXCollections.observableArrayList(
                        rapportSelectionne.getRecommandations()));
                rafraichirListe();
                mettreAJourCompteurs();
            }
        });
    }

    private void ouvrirFormulaireReco(Recommandation reco) {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/com/audit/auditaifx/reco-form-view.fxml"));
            Parent root = loader.load();

            RecoFormController controller = loader.getController();
            controller.setService(service);
            controller.setRapport(rapportSelectionne);
            controller.setRecommandation(reco);
            controller.setOnSave(() -> {
                rapportSelectionne.setRecommandations(service.getRecommandations(rapportSelectionne.getId()));
                recoTable.setItems(FXCollections.observableArrayList(
                        rapportSelectionne.getRecommandations()));
                rafraichirListe();
                mettreAJourCompteurs();
            });

            Stage stage = new Stage();
            stage.setTitle(reco == null ? "Ajouter une recommandation" : "Modifier");
            stage.setScene(new Scene(root, 450, 320));
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.showAndWait();

        } catch (Exception e) {
            afficherErreur("Erreur ouverture formulaire : " + e.getMessage());
        }
    }

    @FXML
    public void genererRecoIA() {
        if (rapportSelectionne == null) return;

        // Désactiver le bouton pendant le chargement
        btnAnalyserIA.setDisable(true);
        btnAjouterReco.setDisable(true);

        // Créer une tâche de fond pour l'analyse IA
        javafx.concurrent.Task<Recommandation> task = new javafx.concurrent.Task<>() {
            @Override
            protected Recommandation call() throws Exception {
                // Simulation d'un temps de réflexion IA (1.5 seconde)
                Thread.sleep(1500);
                return aiService.genererRecommandation(rapportSelectionne);
            }
        };

        // Succès de la tâche
        task.setOnSucceeded(e -> {
            Recommandation nouvelleReco = task.getValue();
            if (nouvelleReco != null) {
                service.ajouterRecommandation(rapportSelectionne.getId(), nouvelleReco);
                rapportSelectionne.setRecommandations(service.getRecommandations(rapportSelectionne.getId()));

                // Mise à jour UI
                recoTable.setItems(FXCollections.observableArrayList(rapportSelectionne.getRecommandations()));
                rafraichirListe();
                showReportDetails(rapportSelectionne);

                Alert info = new Alert(Alert.AlertType.INFORMATION);
                info.setTitle("IA AuditAI");
                info.setHeaderText("Recommandation Générée");
                info.setContentText("L'IA a terminé l'analyse. Une nouvelle recommandation a été ajoutée.");
                info.show();
            }
            btnAnalyserIA.setDisable(false);
            btnAjouterReco.setDisable(false);
        });

        // Erreur pendant la tâche
        task.setOnFailed(ev -> {
            btnAnalyserIA.setDisable(false);
            btnAjouterReco.setDisable(false);
            afficherErreur("Erreur lors de l'analyse IA : " + task.getException().getMessage());
        });

        // Lancer la tâche
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
    @FXML
    public void genererRisquesIA() {
        if (rapportSelectionne == null) return;

        btnDetecterRisques.setDisable(true);

        javafx.concurrent.Task<Risque> task = new javafx.concurrent.Task<>() {
            @Override
            protected Risque call() throws Exception {
                Thread.sleep(1500);
                return aiService.genererRisque(rapportSelectionne);
            }
        };

        task.setOnSucceeded(e -> {
            Risque r = task.getValue();
            if (r != null) {
                // Sauvegarder en base de données pour que ce soit visible dans le rapport
                service.ajouterRisque(rapportSelectionne.getId(), r);
                rapportSelectionne.getRisques().add(r);

                Alert alert = new Alert(Alert.AlertType.WARNING);
                alert.setTitle("Analyse de Risques IA");
                alert.setHeaderText("Risque détecté : " + r.getNiveau());
                alert.setContentText("Description : " + r.getDescription() + "\n\nImpact : " + r.getImpact());
                alert.getDialogPane().setMinWidth(400);
                alert.show();
            }
            btnDetecterRisques.setDisable(false);
        });

        new Thread(task).start();
    }



    // ─── Boutons dans les tableaux ────────────────────────────


    private void ajouterBoutonsReco() {
        colActionsReco.setCellFactory(col -> new TableCell<>() {
            private final Button btnToggle = new Button();
            private final Button btnEdit = new Button("Edit");
            private final Button btnDel = new Button("Supp");
            private final javafx.scene.layout.HBox box =
                    new javafx.scene.layout.HBox(5, btnToggle, btnEdit, btnDel);

            {
                btnEdit.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                btnDel.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                
                btnToggle.setFocusTraversable(false);
                btnEdit.setFocusTraversable(false);
                btnDel.setFocusTraversable(false);
                
                btnToggle.setOnAction(e -> {
                    Recommandation reco = getTableView().getItems().get(getIndex());
                    boolean oldStatus = reco.isResolue();
                    reco.setResolue(!oldStatus);
                    System.out.println("DEBUG Admin: Toggle reco " + reco.getId() + " from " + oldStatus + " to " + reco.isResolue());
                    
                    service.modifierRecommandation(reco);
                    
                    // Refresh data
                    if (rapportSelectionne != null) {
                        rapportSelectionne.setRecommandations(service.getRecommandations(rapportSelectionne.getId()));
                        recoTable.setItems(FXCollections.observableArrayList(rapportSelectionne.getRecommandations()));
                    }
                    
                    getTableView().refresh();
                    mettreAJourCompteurs();
                });

                btnEdit.setOnAction(e -> modifierRecommandation(getTableView().getItems().get(getIndex())));
                btnDel.setOnAction(e -> supprimerRecommandation(getTableView().getItems().get(getIndex())));
                box.setAlignment(Pos.CENTER);
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Recommandation reco = getTableView().getItems().get(getIndex());
                    if (reco.isResolue()) {
                        btnToggle.setText("Ouvrir");
                        btnToggle.setStyle("-fx-background-color: #bdc3c7; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px;");
                    } else {
                        btnToggle.setText("Résoudre");
                        btnToggle.setStyle("-fx-background-color: #2ecc71; -fx-text-fill: white; -fx-background-radius: 4; -fx-font-size: 11px; -fx-font-weight: bold;");
                    }
                    setGraphic(box);
                }
            }
        });
    }

    // ─── Utilitaires ──────────────────────────────────────────

    private void mettreAJourCompteurs() {
        lblTotalRapports.setText(service.getTous().size() + " rapport(s)");
        int nbReco = rapportSelectionne != null ?
                rapportSelectionne.getRecommandations().size() : 0;
        lblTotalReco.setText(nbReco + " recommandation(s)");
    }

    private void afficherErreur(String msg) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setContentText(msg);
        alert.show();
    }

    @FXML
    public void switchClient() {
        try {
            var resource = getClass().getResource("/com/audit/auditaifx/client-view.fxml");
            if (resource == null) {
                afficherErreur("Fichier client-view.fxml introuvable dans les ressources.");
                return;
            }
            FXMLLoader loader = new FXMLLoader(resource);
            Parent root = loader.load();
            Stage stage = (Stage) dashboardView.getScene().getWindow();
            stage.getScene().setRoot(root);
        } catch (Exception e) {
            e.printStackTrace(); // Print to console for debugging
            afficherErreur("Impossible de basculer vers la vue client : " + e.getMessage());
        }
    }
}