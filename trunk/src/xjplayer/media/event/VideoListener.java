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

/**
 * Video listener that delivers VideoEvent.
 *
 * @author Sebastien Vincent
 */
public interface VideoListener
{
    /**
     * Indicates that a new image is available.
     * @param event VideoEvent that contains amongs the other the new image
     */
    public void newImage(NewImageEvent event);

    /**
     * Indicates the end of the video.
     * @param event VideoEvent
     */
    public void endOfVideo(VideoEvent event);
}
