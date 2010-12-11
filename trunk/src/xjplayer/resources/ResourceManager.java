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

package xjplayer.resources;

import java.io.*;
import java.util.*;
import java.util.logging.*;
import java.awt.*;

import javax.sound.sampled.*;

/**
 * Resource manager that manages images, sounds and
 * internationalization messages.
 *
 * This class used the singleton pattern.
 *
 * @author Sebastien Vincent
 */
public class ResourceManager
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(ResourceManager.class.getName());

    /**
     * Locale for internationalization.
     */
    private final Locale mLocale = Locale.getDefault();

    /**
     * Single instance of ResourceManager.
     */
    private static ResourceManager mInstance = null;

    /**
     * Image resources bundle.
     */
    private ResourceBundle mBundleImg = null;

    /**
     * Sound resources bundle.
     */
    private ResourceBundle mBundleSound = null;

    /**
     * Internationalization resources bundle.
     */
    private ResourceBundle mBundleI18n = null;

    /**
     * Constructor.
     */
    private ResourceManager()
    {
        mBundleImg = ResourceBundle.getBundle("resources.images.images");
        mBundleSound = ResourceBundle.getBundle("resources.sounds.sounds");

        /* load corresponding language file, fall back to
         * default language (english) otherwise
         */
        try
        {
            mBundleI18n = ResourceBundle.getBundle("resources.i18n." + mLocale.getLanguage(), mLocale);
        }
        catch(Exception e)
        {
            mBundleI18n = ResourceBundle.getBundle("resources.i18n.default", mLocale);
        }
    }

    /**
     * Get the single instance of a ResourceManager.
     * @return ResourceManager instance
     */
    public static ResourceManager getInstance()
    {
        if(mInstance == null)
        {
            mInstance = new ResourceManager();
        }
        return mInstance;
    }

    /**
     * Load image.
     * @param name name of image (in images.properties)
     * @return image or null if not found or error
     */
    public Image loadImage(String name)
    {
        Image img = null;

        try
        {
            String imgPath = mBundleImg.getString(name);
            img = Toolkit.getDefaultToolkit().getImage(ResourceManager.class.getResource(imgPath));
        }
        catch(Exception e)
        {
            img = null;
        }

        return img;
    }

    /**
     * Load and play a sound.
     * @param name name of sound (in sounds.properties)
     */
    public void playSound(String name)
    {
        String snd = null;
        AudioInputStream is = null;
        File file = null;
        Clip audioClip = null;

        try
        {
            mBundleSound.getString(name);

            if(snd == null)
            {
                logger.warning("No sound found!");
                return;
            }

            audioClip = AudioSystem.getClip();

            /* open sound file and play it */
            file = new File(snd);
            is = AudioSystem.getAudioInputStream(file);
            audioClip.open(is);
            audioClip.loop(0);
            audioClip.flush();

            snd = null;
            is = null;
            file = null;
            audioClip = null;
        }
        catch(Exception e)
        {
            logger.warning("Cannot play sound: " + e);
        }
    }

    /**
     * Get internationalized string.
     *
     * If the resource string is not found, it throws an exception
     * that exit the application.
     * @param name name of the string to internationalize (in
     * <language>.properties)
     * @return internationalized string
     */
    public String getString(String name)
    {
        String str = null;

        try
        {
            str = mBundleI18n.getString(name);
        }
        catch(Exception e)
        {
            /* case when i18n resource is not available in all languages, so
             * fall back to english. If this resource is also not found, it
             * exits.
             */
            str = ResourceBundle.getBundle("resources.i18n.default",
                    mLocale).getString(name);
        }

        return str;
    }
}

