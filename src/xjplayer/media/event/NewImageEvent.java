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

package xjplayer.media.event;

import java.awt.image.*;

/**
 * Event that is notified when a new image is available.
 *
 * @author Sebastien Vincent
 */
public class NewImageEvent extends VideoEvent
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 0L;

    /**
     * New available image.
     */
    private final BufferedImage mImage;

    /**
     * Presentation timestamp.
     */
    private final long mPts;

    /**
     * Constructor.
     * @param source object source
     * @param image new image that will be passed to listener
     * @param pts presentation timestamp
     */
    public NewImageEvent(Object source, BufferedImage image, long pts)
    {
        super(source);
        mImage = image;
        mPts = pts;
    }

    /**
     * Get image.
     * @return image
     */
    public BufferedImage getImage()
    {
        return mImage;
    }

    /**
     * Get presentation timestamp.
     * @return presentation timestamp
     */
    public long getPts()
    {
        return mPts;
    }
}
