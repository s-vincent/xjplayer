/*
 *  XJPlayer - Mediaplayer in Java based on Xuggler.
 *  Copyright (C) 2010 Sebastien Vincent <sebastien.vincent@cppextrem.com>
 *
 *  This program is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU Lesser General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  This program is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU Lesser General Public License for more details.
 *
 *  You should have received a copy of the GNU Lesser General Public License
 *  along with this program.  If not, see <http://www.gnu.org/licenses/
 */

package gui.media;

import java.awt.*;
import java.awt.image.*;
import java.awt.geom.*;

import javax.swing.*;

import media.event.*;

/**
 * Simple video panel that can display static image, video  or some equalizer
 * stuff.
 *
 * @author Sebastien Vincent
 */
public class VideoPanel extends JPanel implements VideoListener
{
    /**
     * Serial version UID.
     */
    public static final long serialVersionUID = 0;

    /**
     * Current image of the VideoPanel.
     */
    private BufferedImage mImage = null;

    /**
     * Size of panel.
     */
    private Dimension mSize = null;

    /**
     * If the image is resized prior to be displayed.
     */
    private boolean resized = false;

    /**
     * Constructor.
     */
    public VideoPanel()
    {
        this.setBackground(Color.BLACK);
        this.setPreferredSize(new Dimension(400, 300));
    }

    /**
     * Paint this panel.
     * @param g graphics object that is able to paint
     */
    protected void paintComponent(Graphics g)
    {
        super.paintComponent((Graphics2D)g);

        if(mImage != null && !resized && (mSize == null || 
                    mSize.width < mImage.getWidth() || 
                    mSize.height < mImage.getHeight()))
        {
            mSize = new Dimension(mImage.getWidth(), mImage.getHeight());
            setPreferredSize(mSize);

            SwingUtilities.getWindowAncestor(this).setMinimumSize(mSize);
        }

        if(mImage != null)
        {
            int width = getWidth();
            int height = getHeight();

            int x = (width - mImage.getWidth()) / 2;
            int y = (height - mImage.getHeight()) / 2;

            g.drawImage(mImage, x, y, null);
        }
        else
        {
            /* TODO draw logo */
        }
    }

    /**
     * {@inheritDoc}
     */
    public void newImage(NewImageEvent event)
    {
        BufferedImage image = event.getImage();
        Dimension panelSize = getSize();

        if(image != null && (image.getWidth() < panelSize.width &&
                    image.getHeight() < panelSize.height))
        {
            mImage = getScaledImage(image, getSize().width, getSize().height, 
                    BufferedImage.TYPE_INT_ARGB);
            resized = true;
        }
        else
        {
            mImage = image;
            resized = false;
        }

        repaint();
    }

    /**
     * {@inheritDoc}
     */
    public void endOfVideo(VideoEvent event)
    {
        mImage = null;
        mSize = null;
        repaint();
    }

    /**
     * Get a scaled <tt>BufferedImage</tt>.
     *
     * Mainly inspired by:
     * http://java.developpez.com/faq/gui/?page=graphique_general_images
     * #GRAPHIQUE_IMAGE_redimensionner
     *
     * @param src source image
     * @param width width of scaled image
     * @param height height of scaled image
     * @param type BufferedImage type
     * @return scaled <tt>BufferedImage</tt>
     */
    private static BufferedImage getScaledImage(BufferedImage src, int width,
            int height, int type)
    {
        double scaleWidth = width / ((double)src.getWidth());
        double scaleHeight = height / ((double)src.getHeight());
        AffineTransform tx = new AffineTransform();

        // Skip rescaling if input and output size are the same.
        if ((Double.compare(scaleWidth, 1) != 0)
                || (Double.compare(scaleHeight, 1) != 0))
            tx.scale(scaleWidth, scaleHeight);

        AffineTransformOp op
            = new AffineTransformOp(tx, AffineTransformOp.TYPE_BILINEAR);
        BufferedImage dst = new BufferedImage(width, height, type);

        return op.filter(src, dst);
    }
}

