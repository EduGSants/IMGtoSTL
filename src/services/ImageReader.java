package services;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.awt.Graphics;

public class ImageReader {
    private static final int LIMIT = 108;
    private static final float LIMITTHCK = 0.4F; // VALOR AINDA NÃO REVISADO

    public BufferedImage Reader(String imgPath) throws IOException {
            // Lê o arquivo e carrega na memória
            return ImageIO.read(new File(imgPath));
    }

    public static pixels[][] generateMatrix(BufferedImage image, float maxThickness) {
        int width = image.getWidth();
        int height = image.getHeight();

        pixels[][] matrix = new pixels[width][height];

        for (int x = 0; x < width; x++) {
            for (int y = 0; y < height; y++) {
                int rgb = image.getRGB(x, y);
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Converter para escala de cinza
                int gray = (int) (0.299 * r + 0.587 * g + 0.114 * b);

                float thickness = (gray / 255.0f) * maxThickness;

                matrix[x][y] = new pixels(gray, thickness);
            }
        }

        return matrix;
    }

    public static class pixels {
        int tons;
        float thickness;

        public pixels(int tons, float thickness) {
            this.tons = tons;
            this.thickness = thickness;
        }
    }
}