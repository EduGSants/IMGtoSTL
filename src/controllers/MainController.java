package controllers;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.SubScene;
import javafx.scene.control.*;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import models.Mesh;
import services.ImageReader;
import services.MeshGenerator;
import views.Mesh3DViewer;
import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class MainController {

    @FXML private Button btnSelecionarArquivo;
    @FXML private Label lblNomeArquivo;
    @FXML private TextField txtLargura;
    @FXML private TextField txtAlturaBranco;
    @FXML private TextField txtAlturaPreto;
    @FXML private TextField txtAlturaVermelho;
    @FXML private ComboBox<String> cbFormato;
    @FXML private Button btnGerar;
    @FXML private Button btnLimpar;
    @FXML private Button btnPreview;
    @FXML private ProgressBar progressBar;
    @FXML private Label lblStatus;
    @FXML private StackPane previewPane;
    private Mesh3DViewer viewer;
    private File imagemSelecionada;
    private Mesh meshGerado;


    @FXML
    public void initialize() {
        final int VIEW_W = 500;
        final int VIEW_H = 400;
        previewPane.setMinSize(VIEW_W, VIEW_H);
        previewPane.setPrefSize(VIEW_W, VIEW_H);
        previewPane.setMaxSize(VIEW_W, VIEW_H);
        viewer = new Mesh3DViewer(VIEW_W, VIEW_H);
        SubScene sub = viewer.getSubScene();
        sub.setWidth(VIEW_W);
        sub.setHeight(VIEW_H);
        previewPane.setClip(new Rectangle(VIEW_W, VIEW_H));
        previewPane.getChildren().add(sub);

        txtLargura.setText("100.0");
        txtAlturaBranco.setText("5.0");
        txtAlturaPreto.setText("0.0");
        txtAlturaVermelho.setText("2.5");
        cbFormato.setValue("Binário (.stl)");
        progressBar.setProgress(0);

        txtLargura.textProperty().addListener((o, a, b) -> validarEntrada(txtLargura));
        txtAlturaBranco.textProperty().addListener((o, a, b) -> validarEntrada(txtAlturaBranco));
        txtAlturaPreto.textProperty().addListener((o, a, b) -> validarEntrada(txtAlturaPreto));
        txtAlturaVermelho.textProperty().addListener((o, a, b) -> validarEntrada(txtAlturaVermelho));
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

            try {
                if (file != null) {
                    imagemSelecionada = file;
                    lblNomeArquivo.setText(file.getName());
                    lblNomeArquivo.setStyle("-fx-text-fill: #27ae60;");
                    lblStatus.setText("Imagem carregada: " + file.getName());
                    atualizarPreview();
                }
            } catch (Exception e) {
                lblStatus.setText("Erro ao carregar imagem!");
                e.printStackTrace();
            }
        }
    }

    @FXML
    private void gerarSTL() {
        if (imagemSelecionada == null) {
            mostrarAlerta("Erro", "Selecione uma imagem primeiro!");
            return;
        }

        try {
            float largura        = Float.parseFloat(txtLargura.getText());
            float alturaBranco   = Float.parseFloat(txtAlturaBranco.getText());
            float alturaPreto    = Float.parseFloat(txtAlturaPreto.getText());
            float alturaVermelho = Float.parseFloat(txtAlturaVermelho.getText());

            if (largura <= 0) {
                mostrarAlerta("Erro", "A largura deve ser maior que zero!");
                return;
            }
            if (alturaBranco < 0 || alturaPreto < 0 || alturaVermelho < 0) {
                mostrarAlerta("Erro", "As alturas não podem ser negativas!");
                return;
            }
            if (alturaBranco == 0 && alturaPreto == 0 && alturaVermelho == 0) {
                mostrarAlerta("Erro", "Pelo menos uma altura deve ser maior que zero!");
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

                    ImageReader.pixels[][] heightMap = ImageReader.generateMatrix(
                            bufferedImage, alturaBranco, alturaPreto, alturaVermelho);

                    updateProgress(0.5, 100);
                    updateMessage("Gerando malha 3D...");

                    MeshGenerator generator = new MeshGenerator();
                    meshHolder[0] = generator.createSolid(heightMap);

                    updateProgress(1.0, 100);
                    updateMessage("Pronto!");
                    return null;
                }
            };

            progressBar.progressProperty().bind(task.progressProperty());
            lblStatus.textProperty().bind(task.messageProperty());

            task.setOnSucceeded(e -> Platform.runLater(() -> {
                System.out.println(">>> setOnSucceeded executou. meshHolder[0] = " + meshHolder[0]);

                btnGerar.setDisable(false);
                btnSelecionarArquivo.setDisable(false);
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();
                progressBar.setProgress(1.0);

                if (meshHolder[0] != null) {
                    meshGerado = meshHolder[0];
                    System.out.println(">>> meshGerado atribuído. Triangles = " + meshGerado.getTriangles().size());

                    viewer.setMesh(meshGerado, Color.web("#4a90d9"));
                    salvarSTL(meshGerado);
                } else {
                    lblStatus.setText("❌ Erro: mesh não gerado.");
                }
            }));

            task.setOnFailed(e -> Platform.runLater(() -> {
                btnGerar.setDisable(false);
                btnSelecionarArquivo.setDisable(false);
                lblStatus.textProperty().unbind();
                progressBar.progressProperty().unbind();
                lblStatus.setText("❌ Erro: " + task.getException().getMessage());
                progressBar.setProgress(0);
                task.getException().printStackTrace();
            }));

            new Thread(task).start();

        } catch (NumberFormatException ex) {
            mostrarAlerta("Erro", "Valores numéricos inválidos!");
        }
    }

    private void salvarSTL(Mesh mesh) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Salvar STL");
        fileChooser.setInitialFileName("modelo_3d.stl");

        String formato = cbFormato.getValue();
        if (formato.contains("Binário")) {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL Binário", "*.stl"));
        } else {
            fileChooser.getExtensionFilters().add(
                    new FileChooser.ExtensionFilter("STL ASCII", "*.stl"));
        }

        Stage stage = (Stage) btnGerar.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                String filename = file.getAbsolutePath();
                if (!filename.endsWith(".stl")) filename += ".stl";

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

    private BufferedImage reduzirImagem(
            BufferedImage original,
            int larguraMaxima
    ) {
        int largura = original.getWidth();
        int altura = original.getHeight();

        if (largura <= larguraMaxima) {
            return original;
        }

        double escala = (double) larguraMaxima / largura;

        int novaLargura = larguraMaxima;
        int novaAltura = Math.max(
                1,
                (int) (altura * escala)
        );

        BufferedImage reduzida = new BufferedImage(
                novaLargura,
                novaAltura,
                BufferedImage.TYPE_INT_RGB
        );

        Graphics2D g = reduzida.createGraphics();

        g.setRenderingHint(
                RenderingHints.KEY_INTERPOLATION,
                RenderingHints.VALUE_INTERPOLATION_BILINEAR
        );

        g.drawImage(
                original,
                0,
                0,
                novaLargura,
                novaAltura,
                null
        );

        g.dispose();

        return reduzida;
    }

    @FXML
    private void atualizarPreview() {

        System.out.println(">>> atualizarPreview chamado.");

        if (imagemSelecionada == null) {
            mostrarAlerta("Erro", "Selecione uma imagem primeiro!");
            return;
        }

        try {
            float largura = Float.parseFloat(txtLargura.getText());
            float alturaBranco = Float.parseFloat(txtAlturaBranco.getText());
            float alturaPreto = Float.parseFloat(txtAlturaPreto.getText());
            float alturaVermelho = Float.parseFloat(txtAlturaVermelho.getText());

            if (largura <= 0) {
                mostrarAlerta("Erro", "A largura deve ser maior que zero!");
                return;
            }

            if (alturaBranco < 0 || alturaPreto < 0 || alturaVermelho < 0) {
                mostrarAlerta("Erro", "As alturas não podem ser negativas!");
                return;
            }

            if (alturaBranco == 0 && alturaPreto == 0 && alturaVermelho == 0) {
                mostrarAlerta("Erro", "Pelo menos uma altura deve ser maior que zero!");
                return;
            }

            final File arquivo = imagemSelecionada;

            final float hBranco = alturaBranco;
            final float hPreto = alturaPreto;
            final float hVermelho = alturaVermelho;

            btnPreview.setDisable(true);

            Task<Mesh> task = new Task<>() {

                @Override
                protected Mesh call() throws Exception {

                    updateMessage("Lendo imagem...");

                    BufferedImage bufferedImage = ImageIO.read(arquivo);

                    if (bufferedImage == null) {
                        throw new Exception("Não foi possível ler a imagem.");
                    }

                    BufferedImage imagemPreview = reduzirImagem(
                            bufferedImage,
                            256
                    );

                    updateMessage("Gerando matriz de alturas...");

                    ImageReader.pixels[][] heightMap =
                            ImageReader.generateMatrix(
                                    imagemPreview,
                                    hBranco,
                                    hPreto,
                                    hVermelho
                            );

                    updateMessage("Gerando malha 3D...");

                    MeshGenerator generator = new MeshGenerator();

                    Mesh mesh = generator.createSolid(heightMap);

                    if (mesh == null || mesh.getTriangles().isEmpty()) {
                        throw new Exception("A malha gerada está vazia.");
                    }

                    return mesh;
                }
            };

            lblStatus.textProperty().bind(task.messageProperty());

            task.setOnSucceeded(event -> {

                lblStatus.textProperty().unbind();
                btnPreview.setDisable(false);

                meshGerado = task.getValue();

                System.out.println(
                        ">>> Preview gerado. Triangles = "
                                + meshGerado.getTriangles().size()
                );

                viewer.setMesh(meshGerado, Color.web("#4a90d9"));

                lblStatus.setText("Preview atualizado com sucesso!");
            });
            task.setOnFailed(event -> {

                lblStatus.textProperty().unbind();
                btnPreview.setDisable(false);

                Throwable erro = task.getException();

                lblStatus.setText("Erro ao gerar preview: " + erro.getMessage());

                erro.printStackTrace();
            });
            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();

        } catch (NumberFormatException ex) {

            mostrarAlerta("Erro", "Valores numéricos inválidos!");
        }
    }

    @FXML
    private void limpar() {
        imagemSelecionada = null;
        meshGerado = null;
        lblNomeArquivo.setText("Nenhum arquivo selecionado");
        lblNomeArquivo.setStyle("-fx-text-fill: #7f8c8d;");
        lblStatus.setText("Aguardando...");
        progressBar.setProgress(0);
        txtLargura.setText("100.0");
        txtAlturaBranco.setText("5.0");
        txtAlturaPreto.setText("0.0");
        txtAlturaVermelho.setText("2.5");
        cbFormato.setValue("Binário (.stl)");
        if (viewer != null) viewer.limpar();
    }

    private void mostrarAlerta(String titulo, String mensagem) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(titulo);
        alert.setHeaderText(null);
        alert.setContentText(mensagem);
        alert.showAndWait();
    }
}