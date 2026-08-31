package models;

import java.io.*;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.ArrayList;
import java.util.List;

public class Mesh {
    private final List<Triangle> faces;

    public Mesh() {
        this.faces = new ArrayList<>();
    }

    public void addTriangle(Triangle triangle) {
        this.faces.add(triangle);
    }

    public List<Triangle> getTriangles() {
        return this.faces;
    }

    public int getTriangleCount() {
        return this.faces.size();
    }

    public String toSTL_ASCII(String solidName) {
        StringBuilder sb = new StringBuilder();

        sb.append("solid ").append(solidName).append("\n");

        for (Triangle t : faces) {
            sb.append("  facet normal ")
                    .append(t.normal.x).append(" ")
                    .append(t.normal.y).append(" ")
                    .append(t.normal.z).append("\n");

            sb.append("    outer loop\n");

            sb.append("      vertex ")
                    .append(t.v1.x).append(" ")
                    .append(t.v1.y).append(" ")
                    .append(t.v1.z).append("\n");

            sb.append("      vertex ")
                    .append(t.v2.x).append(" ")
                    .append(t.v2.y).append(" ")
                    .append(t.v2.z).append("\n");

            sb.append("      vertex ")
                    .append(t.v3.x).append(" ")
                    .append(t.v3.y).append(" ")
                    .append(t.v3.z).append("\n");

            sb.append("    endloop\n");
            sb.append("  endfacet\n");
        }

        sb.append("endsolid ").append(solidName).append("\n");

        return sb.toString();
    }

    public byte[] toSTL_Binary() {
        int numTriangles = faces.size();

        int totalSize = 84 + (numTriangles * 50);

        ByteBuffer buffer = ByteBuffer.allocate(totalSize);
        buffer.order(ByteOrder.LITTLE_ENDIAN);

        byte[] header = new byte[80];
        String headerText = "STL Gerado por MeshGenerator";
        byte[] headerBytes = headerText.getBytes();
        System.arraycopy(headerBytes, 0, header, 0, Math.min(headerBytes.length, 80));
        buffer.put(header);

        buffer.putInt(numTriangles);

        for (Triangle t : faces) {
            // Normal
            buffer.putFloat(t.normal.x);
            buffer.putFloat(t.normal.y);
            buffer.putFloat(t.normal.z);

            // Vértice 1
            buffer.putFloat(t.v1.x);
            buffer.putFloat(t.v1.y);
            buffer.putFloat(t.v1.z);

            // Vértice 2
            buffer.putFloat(t.v2.x);
            buffer.putFloat(t.v2.y);
            buffer.putFloat(t.v2.z);

            // Vértice 3
            buffer.putFloat(t.v3.x);
            buffer.putFloat(t.v3.y);
            buffer.putFloat(t.v3.z);

            // Atributo
            buffer.putShort((short) 0);
        }

        return buffer.array();
    }

    public void saveAsSTL_ASCII(String filename, String solidName) throws IOException {
        String stlContent = toSTL_ASCII(solidName);

        try (FileWriter writer = new FileWriter(filename)) {
            writer.write(stlContent);
        }
    }

    public void saveAsSTL_Binary(String filename) throws IOException {
        byte[] stlData = toSTL_Binary();

        try (FileOutputStream fos = new FileOutputStream(filename)) {
            fos.write(stlData);
        }
    }
}