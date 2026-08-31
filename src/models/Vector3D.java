package models;

public class Vector3D {
    public float x, y, z;

    public Vector3D(float x, float y, float z) {
        this.x = x;
        this.y = y;
        this.z = z;
    }

    // Subtração de vetores
    public Vector3D sub(Vector3D outro) {
        return new Vector3D(this.x - outro.x, this.y - outro.y, this.z - outro.z);
    }

    public Vector3D vectorProduct(Vector3D outro) {
        return new Vector3D(
                (this.y * outro.z) - (this.z * outro.y),
                (this.z * outro.x) - (this.x * outro.z),
                (this.x * outro.y) - (this.y * outro.x)
        );
    }

    public Vector3D normalizar() {
        float length = (float) Math.sqrt((x * x) + (y * y) + (z * z));
        if (length == 0) return new Vector3D(0, 0, 0); // Evita divisão por zero
        return new Vector3D(x / length, y / length, z / length);
    }
}