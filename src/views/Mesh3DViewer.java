package views;

import javafx.geometry.Bounds;
import javafx.scene.*;
import javafx.scene.layout.StackPane;
import javafx.scene.paint.Color;
import javafx.scene.paint.PhongMaterial;
import javafx.scene.shape.CullFace;
import javafx.scene.shape.MeshView;
import javafx.scene.shape.Rectangle;
import javafx.scene.shape.TriangleMesh;
import javafx.scene.transform.Rotate;
import models.Mesh;
import models.Triangle;
import models.Vector3D;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class Mesh3DViewer {

    private final Group worldGroup = new Group();       // rotação do usuário
    private final Group orientationGroup = new Group(); // eixo Z para cima
    private final Group contentGroup = new Group();     // mesh + centralização
    private final StackPane container;
    private final SubScene subScene;
    private final PerspectiveCamera camera = new PerspectiveCamera(true);

    private double anchorX, anchorY;
    private double rotateY = -30, rotateX = 20;
    private final Rotate rotateYTransform = new Rotate(rotateY, Rotate.Y_AXIS);
    private final Rotate rotateXTransform = new Rotate(rotateX, Rotate.X_AXIS);

    public Mesh3DViewer(double width, double height) {
        // --- Câmera ---
        camera.setNearClip(0.1);
        camera.setFarClip(100000);
        camera.setFieldOfView(35);
        camera.setTranslateZ(-500);

        // --- Luzes ---
        AmbientLight ambient = new AmbientLight(Color.color(0.55, 0.55, 0.55));
        PointLight keyLight = new PointLight(Color.WHITE);
        keyLight.setTranslateX(-300);
        keyLight.setTranslateY(-400);
        keyLight.setTranslateZ(-400);
        PointLight fillLight = new PointLight(Color.color(0.4, 0.4, 0.5));
        fillLight.setTranslateX(400);
        fillLight.setTranslateY(200);
        fillLight.setTranslateZ(-200);

        // --- Hierarquia: worldGroup -> orientationGroup -> contentGroup ---
        // orientationGroup gira 90° em X para que o eixo Z (altura) aponte para cima no JavaFX
        orientationGroup.getTransforms().add(new Rotate(90, Rotate.X_AXIS));
        orientationGroup.getChildren().add(contentGroup);
        worldGroup.getTransforms().addAll(rotateYTransform, rotateXTransform);
        worldGroup.getChildren().addAll(ambient, keyLight, fillLight, orientationGroup);

        subScene = new SubScene(worldGroup, width, height, true, SceneAntialiasing.BALANCED);
        subScene.setCamera(camera);
        subScene.setFill(Color.rgb(255, 255, 255));

        // Container com tamanho fixo
        container = new StackPane(subScene);
        container.setMinSize(width, height);
        container.setPrefSize(width, height);
        container.setMaxSize(width, height);
        container.setClip(new Rectangle(width, height));

        setupControls();
    }

    public SubScene getSubScene() {
        return subScene;
    }

    public StackPane getContainer() {
        return container;
    }

    // ============================================
    // Controles de mouse (arrastar = rotacionar, scroll = zoom)
    // ============================================
    private void setupControls() {
        subScene.setOnMousePressed(e -> {
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
        });

        subScene.setOnMouseDragged(e -> {
            double dx = e.getSceneX() - anchorX;
            double dy = e.getSceneY() - anchorY;
            rotateY += dx * 0.4;
            rotateX -= dy * 0.4;
            rotateYTransform.setAngle(rotateY);
            rotateXTransform.setAngle(rotateX);
            anchorX = e.getSceneX();
            anchorY = e.getSceneY();
        });

        subScene.setOnScroll(e -> {
            double zoom = e.getDeltaY();
            double z = camera.getTranslateZ() - zoom * 2;
            camera.setTranslateZ(Math.max(-5000, Math.min(-50, z)));
        });
    }

    // ============================================
    // Define o mesh a ser exibido
    // ============================================
    public void setMesh(Mesh mesh, Color cor) {
        System.out.println(">>> setMesh chamado. mesh = " + mesh);
        if (mesh == null) return;
        System.out.println(">>> Triangles recebidos: " + mesh.getTriangles().size());
        contentGroup.getChildren().clear();
        if (mesh == null || mesh.getTriangles().isEmpty()) return;

        TriangleMesh fxMesh = convertToFxMesh(mesh);
        MeshView meshView = new MeshView(fxMesh);

        PhongMaterial material = new PhongMaterial(cor);
        material.setSpecularColor(Color.rgb(220, 220, 220));
        material.setSpecularPower(32);
        meshView.setMaterial(material);
        meshView.setCullFace(CullFace.BACK);

        // Centraliza e escala o modelo
        Bounds b = meshView.getBoundsInLocal();

        double maxDim = Math.max(
                b.getWidth(),
                Math.max(b.getHeight(), b.getDepth())
        );

        if (maxDim <= 0) {
            return;
        }

        // Ocupa 65% da dimensão de referência
        double margem = 0.65;
        double escala = (300.0 * margem) / maxDim;

        contentGroup.setScaleX(escala);
        contentGroup.setScaleY(escala);
        contentGroup.setScaleZ(escala);

        contentGroup.setTranslateX(-b.getCenterX() * escala);
        contentGroup.setTranslateY(-b.getCenterY() * escala);
        contentGroup.setTranslateZ(-b.getCenterZ() * escala);

        contentGroup.getChildren().add(meshView);
    }

    // ============================================
    // Conversão models.Mesh -> javafx.scene.shape.TriangleMesh
    // ============================================
    private TriangleMesh convertToFxMesh(Mesh mesh) {
        // Reutiliza vértices idênticos (economiza memória em malhas grandes)
        Map<String, Integer> indexMap = new HashMap<>();
        List<Float> points = new ArrayList<>();
        List<Integer> faces = new ArrayList<>();

        for (Triangle t : mesh.getTriangles()) {
            int i1 = getOrAddVertex(t.v1, points, indexMap);
            int i2 = getOrAddVertex(t.v2, points, indexMap);
            int i3 = getOrAddVertex(t.v3, points, indexMap);

            // Formato POINT_TEXCOORD: p0, t0, p1, t1, p2, t2
            // Usamos 0 como índice de textura (não há textura por enquanto)
            faces.add(i1); faces.add(0);
            faces.add(i2); faces.add(0);
            faces.add(i3); faces.add(0);
        }

        float[] pts = new float[points.size()];
        for (int i = 0; i < pts.length; i++) pts[i] = points.get(i);

        int[] fcs = new int[faces.size()];
        for (int i = 0; i < fcs.length; i++) fcs[i] = faces.get(i);

        TriangleMesh fxMesh = new TriangleMesh();
        fxMesh.getPoints().setAll(pts);
        fxMesh.getTexCoords().addAll(0, 0); // uma única texCoord
        fxMesh.getFaces().setAll(fcs);
        return fxMesh;
    }

    private int getOrAddVertex(Vector3D v, List<Float> points, Map<String, Integer> indexMap) {
        String key = v.x + "|" + v.y + "|" + v.z;
        Integer idx = indexMap.get(key);
        if (idx != null) return idx;
        int newIdx = points.size() / 3;
        points.add(v.x);
        points.add(v.y);
        points.add(v.z);
        indexMap.put(key, newIdx);
        return newIdx;
    }

    public void limpar() {
        contentGroup.getChildren().clear();
    }
}