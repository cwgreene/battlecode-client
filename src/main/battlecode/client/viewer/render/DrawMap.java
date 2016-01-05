package battlecode.client.viewer.render;

import battlecode.client.util.ImageFile;

import java.awt.*;
import java.awt.geom.AffineTransform;
import java.awt.geom.Line2D;
import java.awt.image.BufferedImage;

public class DrawMap {

    private int mapWidth;
    private int mapHeight;
    private Stroke gridStroke;
    private int locPixelWidth = 32;

    // prerendered images
    private BufferedImage prerender;

    private BufferedImage mapBG;

    private final int subtileHeight = 4; // 4 x 4
    private final int roadTileCount = 3; // empty, full, rounded

    public DrawMap(battlecode.world.GameMap map) {
        mapWidth = map.getWidth();
        mapHeight = map.getHeight();

        loadMapArt();

        //FIXME: commented out for now
//    if (!RenderConfiguration.getInstance().isTournamentMode()) {
        prerenderMap(map);
//    }
        gridStroke = new BasicStroke(0.3f / RenderConfiguration.getInstance()
                .getSpriteSize());

    }

    public void prerenderMap(battlecode.world.GameMap m) {
        Graphics2D g2 = prerender.createGraphics();
        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        for (int x = 0; x < mapWidth; x += mapBG.getWidth() / locPixelWidth) {
            for (int y = 0; y < mapHeight; y += mapBG.getHeight() /
                    locPixelWidth) {
                g2.drawImage(mapBG, null, x * locPixelWidth, y * locPixelWidth);
            }
        }

        g2.dispose();
    }

    public int getMapWidth() {
        return mapWidth;
    }

    public int getMapHeight() {
        return mapHeight;
    }

    public void draw(Graphics2D g2, DrawState ds) {
        AffineTransform pushed = g2.getTransform();

        g2.setComposite(AlphaComposite.getInstance(AlphaComposite.SRC_OVER));

        g2.scale(1.0 / locPixelWidth, 1.0 / locPixelWidth);

        g2.drawImage(prerender, null, null);

        g2.setTransform(pushed);
        if (RenderConfiguration.showGridlines()) {
            g2.setColor(new Color(0.4f, 0.4f, 0.4f, 1.0f));
            g2.setStroke(gridStroke);
            Line2D.Float gridline = new Line2D.Float(0, 0, 0, mapHeight);
            for (int i = 1; i < mapWidth; i += 1) {
                gridline.x1 = gridline.x2 = i;
                g2.draw(gridline);
            }
            gridline.x1 = 0;
            gridline.x2 = mapWidth;
            for (int i = 1; i < mapHeight; i += 1) {
                gridline.y1 = gridline.y2 = i;
                g2.draw(gridline);
            }
        }
    }


    private int mapIndex(int x, int y, int sx, int sy) {
        return (x * subtileHeight + sx) * mapHeight * subtileHeight
                + (y * subtileHeight + sy);
    }

    public void loadMapArt() {
        mapBG = (new ImageFile("art/map_bg.png")).image;

        prerender = GraphicsEnvironment.getLocalGraphicsEnvironment()
                .getDefaultScreenDevice()
                .getDefaultConfiguration()
                .createCompatibleImage(locPixelWidth * mapWidth,
                        locPixelWidth * mapHeight,
                        Transparency.TRANSLUCENT);
    }
}
