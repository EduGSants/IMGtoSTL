package services;

import models.*;

public class MeshGenerator {
    private final float baseZ = 0.0f;

    public Mesh createSolid(ImageReader.pixels[][] heightMap) {
        Mesh mesh = new Mesh();
        int width = heightMap.length;
        int height = heightMap[0].length;
        buildTopSurface(mesh, heightMap, width, height);
        buildBottomSurface(mesh, width, height);
        buildWalls(mesh, heightMap, width, height);
        return mesh;
    }

    private void buildTopSurface(Mesh mesh, ImageReader.pixels[][] heightMap, int width, int height) {
        for (int x = 0; x < width - 1; x++) {
            for (int y = 0; y < height - 1; y++) {
                // Índices invertidos na imagem
                int imgY0 = height - 1 - y;
                int imgY1 = height - 2 - y;

                // P1: Superior Esquerdo (x, y)
                // P2: Superior Direito (x+1, y)
                // P3: Inferior Esquerdo (x, y+1)
                // P4: Inferior Direito (x+1, y+1)
                Vector3D p1 = new Vector3D(x, y, heightMap[x][imgY0].thickness);
                Vector3D p2 = new Vector3D(x + 1, y, heightMap[x+1][imgY0].thickness);
                Vector3D p3 = new Vector3D(x, y + 1, heightMap[x][imgY1].thickness);
                Vector3D p4 = new Vector3D(x + 1, y + 1, heightMap[x+1][imgY1].thickness);
                mesh.addTriangle(new Triangle(p1, p3, p2));
                mesh.addTriangle(new Triangle(p3, p4, p2));
            }
        }
    }

    private void buildBottomSurface(Mesh mesh, int width, int height) {
        Vector3D p1 = new Vector3D(0, 0, baseZ);
        Vector3D p2 = new Vector3D(width - 1, 0, baseZ);
        Vector3D p3 = new Vector3D(0, height - 1, baseZ);
        Vector3D p4 = new Vector3D(width - 1, height - 1, baseZ);
        mesh.addTriangle(new Triangle(p1, p2, p3));
        mesh.addTriangle(new Triangle(p2, p4, p3));
    }

    private void buildWalls(Mesh mesh, ImageReader.pixels[][] heightMap, int width, int height) {
        int topY = height - 1;
        for (int x = 0; x < width - 1; x++) {
            Vector3D topLeft = new Vector3D(x, 0, heightMap[x][topY].thickness);
            Vector3D topRight = new Vector3D(x + 1, 0, heightMap[x+1][topY].thickness);
            Vector3D botLeft = new Vector3D(x, 0, baseZ);
            Vector3D botRight = new Vector3D(x + 1, 0, baseZ);
            // Anti-horário visto de fora (frente)
            mesh.addTriangle(new Triangle(topLeft, topRight, botLeft));
            mesh.addTriangle(new Triangle(topRight, botRight, botLeft));
        }

        int bottomY = 0;
        for (int x = 0; x < width - 1; x++) {
            Vector3D topLeft = new Vector3D(x, height - 1, heightMap[x][bottomY].thickness);
            Vector3D topRight = new Vector3D(x + 1, height - 1, heightMap[x+1][bottomY].thickness);
            Vector3D botLeft = new Vector3D(x, height - 1, baseZ);
            Vector3D botRight = new Vector3D(x + 1, height - 1, baseZ);
            // Anti-horário visto de fora (trás)
            mesh.addTriangle(new Triangle(topLeft, botLeft, topRight));
            mesh.addTriangle(new Triangle(topRight, botLeft, botRight));
        }

        for (int y = 0; y < height - 1; y++) {
            int imgY0 = height - 1 - y;
            int imgY1 = height - 2 - y;
            Vector3D topLeft = new Vector3D(0, y, heightMap[0][imgY0].thickness);
            Vector3D topRight = new Vector3D(0, y + 1, heightMap[0][imgY1].thickness);
            Vector3D botLeft = new Vector3D(0, y, baseZ);
            Vector3D botRight = new Vector3D(0, y + 1, baseZ);
            // Anti-horário visto de fora (esquerda)
            mesh.addTriangle(new Triangle(topLeft, botLeft, topRight));
            mesh.addTriangle(new Triangle(topRight, botLeft, botRight));
        }

        int rightX = width - 1;
        for (int y = 0; y < height - 1; y++) {
            int imgY0 = height - 1 - y;
            int imgY1 = height - 2 - y;
            Vector3D topLeft = new Vector3D(rightX, y, heightMap[rightX][imgY0].thickness);
            Vector3D topRight = new Vector3D(rightX, y + 1, heightMap[rightX][imgY1].thickness);
            Vector3D botLeft = new Vector3D(rightX, y, baseZ);
            Vector3D botRight = new Vector3D(rightX, y + 1, baseZ);
            // Anti-horário visto de fora (direita)
            mesh.addTriangle(new Triangle(topLeft, topRight, botLeft));
            mesh.addTriangle(new Triangle(topRight, botRight, botLeft));
        }
    }
}