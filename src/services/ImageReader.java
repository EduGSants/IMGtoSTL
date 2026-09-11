package services;

import java.awt.image.BufferedImage;

public class ImageReader {

    public enum TipoCor { PRETO, BRANCO, VERMELHO }

    public static class pixels {
        public int tons;            // valor RGB original (debug)
        public float thickness;     // altura aplicada
        public TipoCor tipo;        // classificação

        public pixels(int tons, float thickness, TipoCor tipo) {
            this.tons = tons;
            this.thickness = thickness;
            this.tipo = tipo;
        }
    }

    public static pixels[][] generateMatrix(BufferedImage image,
                                            float alturaBranco,
                                            float alturaPreto,
                                            float alturaVermelho) {
        int width = image.getWidth();
        int height = image.getHeight();
        pixels[][] matrix = new pixels[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                TipoCor tipo;
                float thickness;

                if (ehVermelho(r, g, b)) {
                    tipo = TipoCor.VERMELHO;
                    thickness = alturaVermelho;
                } else {
                    int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                    if (gray < 128) {
                        tipo = TipoCor.PRETO;
                        thickness = alturaPreto;
                    } else {
                        tipo = TipoCor.BRANCO;
                        thickness = alturaBranco;
                    }
                }

                matrix[x][y] = new pixels(rgb, thickness, tipo);
            }
        }
        return matrix;
    }

    private static boolean ehVermelho(int r, int g, int b) {
        return r > 120 && (r - g) > 60 && (r - b) > 60;
    }
}