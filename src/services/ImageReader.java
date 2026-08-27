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

    protected pixels[][] generateMatrix (BufferedImage img, float thickness){
        int height = img.getHeight();
        int width = img.getWidth();
        pixels[][] matrix = new pixels[height][width];
        BufferedImage bg = new BufferedImage(width, height, BufferedImage.TYPE_BYTE_GRAY);
        Graphics gp = bg.getGraphics();
        gp.drawImage(img, 0,0,null);
        gp.dispose();
        for (int y = 0; y < height; y++) {
            for (int x = 0; x < width; x++) {
                int rgb = img.getRGB(x, y);

                // Extrai os canais RGB
                int r = (rgb >> 16) & 0xFF;
                int g = (rgb >> 8) & 0xFF;
                int b = rgb & 0xFF;

                // Fórmula de luminância
                int cinza = (int) (0.299 * r + 0.587 * g + 0.114 * b);
                matrix[x][y].tons = cinza;
                if(cinza>LIMIT) {
                    matrix[x][y].thickness = thickness;
                } else {
                    matrix[x][y].thickness = (thickness + LIMITTHCK)/2;
                }
            }
        }
        return matrix;
    }

    protected class pixels {
        int tons;
        float thickness;

        public pixels(int tons, float thickness) {
            this.tons = tons;
            this.thickness = thickness;
        }
    }
}