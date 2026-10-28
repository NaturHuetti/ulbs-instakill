package com.instakill.media;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.AlphaComposite;
import java.awt.Color;
import java.awt.Font;
import java.awt.FontMetrics;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;

@Service
public class ImageProcessingService {

    private static final Logger log = LoggerFactory.getLogger(ImageProcessingService.class);
    private static final int MAX_WIDTH = 300;
    private static final String WATERMARK = "Instakill Demo";

    /** Scales to max 300px width and adds a watermark; returns the original bytes if the format cannot be decoded. */
    public byte[] process(byte[] original, String extension) throws IOException {
        BufferedImage source = ImageIO.read(new ByteArrayInputStream(original));
        if (source == null) {
            log.warn("Cannot decode '{}' image, storing unprocessed", extension);
            return original;
        }
        boolean opaque = extension.equals("jpg") || extension.equals("jpeg");
        int width = Math.min(source.getWidth(), MAX_WIDTH);
        int height = Math.max(1, Math.round((float) source.getHeight() * width / source.getWidth()));
        BufferedImage target = new BufferedImage(width, height,
                opaque ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB);

        Graphics2D g = target.createGraphics();
        try {
            g.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
            g.drawImage(source, 0, 0, width, height, null);
            drawWatermark(g, width, height);
        } finally {
            g.dispose();
        }

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        String format = opaque ? "jpg" : extension.equals("gif") ? "gif" : "png";
        if (!ImageIO.write(target, format, out)) {
            log.warn("No writer for '{}', storing unprocessed", format);
            return original;
        }
        return out.toByteArray();
    }

    private static void drawWatermark(Graphics2D g, int width, int height) {
        g.setFont(new Font(Font.SANS_SERIF, Font.BOLD, Math.max(10, width / 20)));
        FontMetrics metrics = g.getFontMetrics();
        int margin = Math.max(4, width / 50);
        int x = width - metrics.stringWidth(WATERMARK) - margin;
        int y = height - margin - metrics.getDescent();
        g.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER, 0.7f));
        g.setColor(Color.BLACK);
        g.drawString(WATERMARK, x + 1, y + 1);
        g.setColor(Color.WHITE);
        g.drawString(WATERMARK, x, y);
    }
}
