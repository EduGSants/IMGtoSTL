package services;

import models.*;

public class MeshGenerator {
    private static final float EPSILON = 0.001f;
    private final float baseZ = 0.0f;

    public Mesh createSolid(ImageReader.pixels[][] heightMap) {
        Mesh mesh = new Mesh();
        int width = heightMap.length;
        int height = heightMap[0].length;

        buildTopSurface(mesh, heightMap, width, height);
        buildBottomSurface(mesh, heightMap, width, height);
        buildWalls(mesh, heightMap, width, height);

        return mesh;
    }

    // ============================================
    // Helpers
    // ============================================
    private boolean isSolid(ImageReader.pixels p) {
        return p.thickness > EPSILON;
    }

    /** Retorna o pixel correspondente em (x, y) no espaço do modelo (com Y invertido). */
    private ImageReader.pixels pixelAt(ImageReader.pixels[][] hm, int x, int y, int h) {
        return hm[x][h - 1 - y];
    }

    /** Um quad é ativo se os 4 cantos são sólidos. */
    private boolean quadActive(ImageReader.pixels[][] hm, int x, int y, int w, int h) {
        if (x < 0 || y < 0 || x >= w - 1 || y >= h - 1) return false;
        return isSolid(hm[x][h - 1 - y])
                && isSolid(hm[x + 1][h - 1 - y])
                && isSolid(hm[x][h - 2 - y])
                && isSolid(hm[x + 1][h - 2 - y]);
    }

    // ============================================
    // ETAPA A: SUPERFÍCIE SUPERIOR
    // ============================================
    private void buildTopSurface(Mesh mesh, ImageReader.pixels[][] hm, int w, int h) {
        for (int x = 0; x < w - 1; x++) {
            for (int y = 0; y < h - 1; y++) {
                if (!quadActive(hm, x, y, w, h)) continue;

                ImageReader.pixels pTL = pixelAt(hm, x,     y,     h);
                ImageReader.pixels pTR = pixelAt(hm, x + 1, y,     h);
                ImageReader.pixels pBL = pixelAt(hm, x,     y + 1, h);
                ImageReader.pixels pBR = pixelAt(hm, x + 1, y + 1, h);

                Vector3D vTL = new Vector3D(x,     y,     pTL.thickness);
                Vector3D vTR = new Vector3D(x + 1, y,     pTR.thickness);
                Vector3D vBL = new Vector3D(x,     y + 1, pBL.thickness);
                Vector3D vBR = new Vector3D(x + 1, y + 1, pBR.thickness);

                mesh.addTriangle(new Triangle(vTL, vBL, vTR));
                mesh.addTriangle(new Triangle(vBL, vBR, vTR));
            }
        }
    }

    // ============================================
    // ETAPA B: SUPERFÍCIE INFERIOR
    // ============================================
    private void buildBottomSurface(Mesh mesh, ImageReader.pixels[][] hm, int w, int h) {
        for (int x = 0; x < w - 1; x++) {
            for (int y = 0; y < h - 1; y++) {
                if (!quadActive(hm, x, y, w, h)) continue;

                Vector3D vTL = new Vector3D(x,     y,     baseZ);
                Vector3D vTR = new Vector3D(x + 1, y,     baseZ);
                Vector3D vBL = new Vector3D(x,     y + 1, baseZ);
                Vector3D vBR = new Vector3D(x + 1, y + 1, baseZ);

                // Winding invertida (visto de baixo)
                mesh.addTriangle(new Triangle(vTL, vTR, vBL));
                mesh.addTriangle(new Triangle(vTR, vBR, vBL));
            }
        }
    }

    // ============================================
    // ETAPA C: PAREDES (perímetro + fronteiras internas)
    // ============================================
    private void buildWalls(Mesh mesh, ImageReader.pixels[][] hm, int w, int h) {
        for (int x = 0; x < w - 1; x++) {
            for (int y = 0; y < h - 1; y++) {
                if (!quadActive(hm, x, y, w, h)) continue;

                ImageReader.pixels pTL = pixelAt(hm, x,     y,     h);
                ImageReader.pixels pTR = pixelAt(hm, x + 1, y,     h);
                ImageReader.pixels pBL = pixelAt(hm, x,     y + 1, h);
                ImageReader.pixels pBR = pixelAt(hm, x + 1, y + 1, h);

                // --- Aresta SUPERIOR (vizinho em y-1) ---
                if (!quadActive(hm, x, y - 1, w, h)) {
                    Vector3D topL = new Vector3D(x,     y, pTL.thickness);
                    Vector3D topR = new Vector3D(x + 1, y, pTR.thickness);
                    Vector3D botL = new Vector3D(x,     y, baseZ);
                    Vector3D botR = new Vector3D(x + 1, y, baseZ);
                    mesh.addTriangle(new Triangle(topL, topR, botL));
                    mesh.addTriangle(new Triangle(topR, botR, botL));
                }

                // --- Aresta INFERIOR (vizinho em y+1) ---
                if (!quadActive(hm, x, y + 1, w, h)) {
                    Vector3D topL = new Vector3D(x,     y + 1, pBL.thickness);
                    Vector3D topR = new Vector3D(x + 1, y + 1, pBR.thickness);
                    Vector3D botL = new Vector3D(x,     y + 1, baseZ);
                    Vector3D botR = new Vector3D(x + 1, y + 1, baseZ);
                    mesh.addTriangle(new Triangle(topL, botL, topR));
                    mesh.addTriangle(new Triangle(topR, botL, botR));
                }

                // --- Aresta ESQUERDA (vizinho em x-1) ---
                if (!quadActive(hm, x - 1, y, w, h)) {
                    Vector3D topL = new Vector3D(x, y,     pTL.thickness);
                    Vector3D topR = new Vector3D(x, y + 1, pBL.thickness);
                    Vector3D botL = new Vector3D(x, y,     baseZ);
                    Vector3D botR = new Vector3D(x, y + 1, baseZ);
                    mesh.addTriangle(new Triangle(topL, botL, topR));
                    mesh.addTriangle(new Triangle(topR, botL, botR));
                }

                // --- Aresta DIREITA (vizinho em x+1) ---
                if (!quadActive(hm, x + 1, y, w, h)) {
                    Vector3D topL = new Vector3D(x + 1, y,     pTR.thickness);
                    Vector3D topR = new Vector3D(x + 1, y + 1, pBR.thickness);
                    Vector3D botL = new Vector3D(x + 1, y,     baseZ);
                    Vector3D botR = new Vector3D(x + 1, y + 1, baseZ);
                    mesh.addTriangle(new Triangle(topL, topR, botL));
                    mesh.addTriangle(new Triangle(topR, botR, botL));
                }
            }
        }
    }
}