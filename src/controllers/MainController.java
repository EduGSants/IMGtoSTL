package controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.image.*;
import javafx.scene.image.Image;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import javafx.concurrent.Task;

import models.*;
import services.*;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.nio.file.*;

public class MainController {

    @FXML private Button btnSelecionarArquivo;
    @FXML private Label lblNomeArquivo;
    @FXML private ImageView imgPreview;
    @FXML private TextField txtLargura;
    @FXML private TextField txtAltura;
    @FXML private ComboBox<String> cbFormato;
    @FXML private Button btnGerar;
    @FXML private Button btnLimpar;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private CheckBox chkinvert;

    private File imagemSelecionada;
    private Mesh meshGerado;

    @FXML
    public void initialize() {
        chkinvert.setSelected(false);
        txtLargura.setText("100.0");
        txtAltura.setText("5.0");
        cbFormato.setValue("Binário (.stl)");
        progressBar.setProgress(0);

        txtLargura.textProperty().addListener((obs, old, newVal) -> validarEntrada(txtLargura));
        txtAltura.textProperty().addListener((obs, old, newVal) -> validarEntrada(txtAltura));
    }

    private void validarEntrada(TextField field) {
        String text = field.getText();
        if (!text.matches("\\d*\\.?\\d*")) {
            field.setText(text.replaceAll("[^\\d.]", ""));
        }
    }

    @FXML
    private void selecionarArquivo() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selecionar Imagem");
        fileChooser.getExtensionFilters().addAll(
                new FileChooser.ExtensionFilter("Imagens", "*.png", "*.jpg", "*.jpeg", "*.bmp")
        );

        Stage stage = (Stage) btnSelecionarArquivo.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);

        if (file != null) {
            imagemSelecionada = file;
            lblNomeArquivo.setText(file.getName());
            lblNomeArquivo.setStyle("-fx-text-fill: #27ae60;");

            // Carregar pré-visualização
            try {
                Image image = new Image(file.toURI().toString());
                imgPreview.setImage(image);
                lblStatus.setText("Imagem carregada: " + file.getName());
            } catch (Exception e) {
                lblStatus.setText("Erro ao carregar imagem!");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void gerarSTL() {
        boolean invert = chkinvert.isSelected();
        if (imagemSelecionada == null) {
            mostrarAlerta("Erro", "Selecione uma imagem primeiro!");
            return;
        }

        try {
            float largura = Float.parseFloat(txtLargura.getText());
            float alturaMax = Float.parseFloat(txtAltura.getText());

            if (largura <= 0 || alturaMax <= 0) {
                mostrarAlerta("Erro", "Largura e altura devem ser maiores que zero!");
                return;
            }

            btnGerar.setDisable(true);
            btnSelecionarArquivo.setDisable(true);

            final Mesh[] meshHolder = new Mesh[1];

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    updateProgress(0, 100);
                    updateMessage("Lendo imagem...");

                    BufferedImage bufferedImage = ImageIO.read(imagemSelecionada);
                    if (bufferedImage == null) {
                        throw new Exception("Erro ao ler a imagem.");
                    }

                    ImageReader.pixels[][] heightMap = ImageReader.generateMatrix(bufferedImage, alturaMax, invert);

                    updateProgress(0.3, 100);
                    updateMessage("Gerando malha 3D...");

                    MeshGenerator generator = new MeshGenerator();
                    Mesh mesh = generator.createSolid(heightMap);

                    // Armazenar o mesh no array
                    meshHolder[0] = mesh;

                    updateProgress(1.0, 100);
                    updateMessage("Pronto para salvar!");

                    return null;
                }
            };

            progressBar.progressProperty().bind(task.progressProperty());
            lblStatus.textProperty().bind(task.messageProperty());

            task.setOnSucceeded(e -> {
                Platform.runLater(() -> {
                    btnGerar.setDisable(false);
                    btnSelecionarArquivo.setDisable(false);
                    lblStatus.textProperty().unbind();
                    progressBar.progressProperty().unbind();

                    if (meshHolder[0] != null) {
                        salvarSTL(meshHolder[0]);
                    } else {
                        lblStatus.setText("❌ Erro: mesh não gerado.");
                    }
                });
            });

            task.setOnFailed(e -> {
                Platform.runLater(() -> {
                    btnGerar.setDisable(false);
                    btnSelecionarArquivo.setDisable(false);
                    lblStatus.textProperty().unbind();
                    progressBar.progressProperty().unbind();
                    lblStatus.setText("❌ Erro: " + task.getException().getMessage());
                    progressBar.setProgress(0);
                });
            });

            new Thread(task).start();

        } catch (NumberFormatException ex) {
            mostrarAlerta("Erro", "Valores numéricos inválidos!");
        }
    }

    private void salvarSTL(Mesh mesh) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar STL");
        fileChooser.setInitialFileName("*.stl");

        String formato = cbFormato.getValue();
        if (formato.contains("Binário")) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL Binário", "*.stl")
            );
        } else {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL ASCII", "*.stl")
            );
        }

        Stage stage = (Stage) btnGerar.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String filename = file.getAbsolutePath();
                if (!filename.endsWith(".stl")) {
                    filename += ".stl";
                }

                if (formato.contains("Binário")) {
                    mesh.saveAsSTL_Binary(filename);
                } else {
                    String nome = file.getName().replace(".stl", "");
                    mesh.saveAsSTL_ASCII(filename, nome);
                }

                lblStatus.setText("✅ STL salvo em: " + file.getPath());
            } catch (Exception ex) {
                lblStatus.setText("❌ Erro ao salvar STL: " + ex.getMessage());
                ex.printStackTrace();
            }
        } else {
            lblStatus.setText("Salvamento cancelado.");
        }
    }

    private void salvarSTL(Mesh mesh, float largura, float alturaMax) throws Exception {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar STL");
        fileChooser.setInitialFileName("modelo_3d.stl");

        // Adicionar extensão baseada no formato
        String formato = cbFormato.getValue();
        if (formato.contains("Binário")) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL Binário", "*.stl")
            );
        } else {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL ASCII", "*.stl")
            );
        }

        Stage stage = (Stage) btnGerar.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            String filename = file.getAbsolutePath();
            if (!filename.endsWith(".stl")) {
                filename += ".stl";
            }

            // Escalar o mesh para a largura desejada
            if (formato.contains("Binário")) {
                mesh.saveAsSTL_Binary(filename);
            } else {
                String nome = file.getName().replace(".stl", "");
                mesh.saveAsSTL_ASCII(filename, nome);
            }

            Desktop.getDesktop().open(file.getParentFile());
        }
    }

    @FXML
    private void limpar() {
        imagemSelecionada = null;
        meshGerado = null;
        lblNomeArquivo.setText("Nenhum arquivo selecionado");
        lblNomeArquivo.setStyle("-fx-text-fill: #7f8c8d;");
        imgPreview.setImage(null);
        lblStatus.setText("Aguardando...");
        progressBar.setProgress(0);
        txtLargura.setText("100.0");
        txtAltura.setText("5.0");
        cbFormato.setValue("Binário (.stl)");
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}