package models;

public class Triangle {
    public Vector3D v1, v2, v3;
    public Vector3D normal;

    public Triangle(Vector3D v1, Vector3D v2, Vector3D v3) {
        this.v1 = v1;
        this.v2 = v2;
        this.v3 = v3;

        // Calcula a normal automaticamente no momento da criação
        this.normal = calc(v1, v2, v3);
    }

    private Vector3D calc(Vector3D p1, Vector3D p2, Vector3D p3) {
        Vector3D edge1 = p2.sub(p1); // Vetor apontando de p1 para p2
        Vector3D edge2 = p3.sub(p1); // Vetor apontando de p1 para p3
        Vector3D normal = edge1.vectorProduct(edge2);

        return normal.normalizar();
    }
}