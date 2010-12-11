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

package xjplayer.media;

import java.nio.*;

import com.xuggle.mediatool.*;
import com.xuggle.mediatool.event.*;
import com.xuggle.xuggler.*;

import xjplayer.media.event.*;

/**
 * Class that will control MediaCore object. It means to start, stop,
 * seek, ... the media.
 *
 * @author Sebastien Vincent
 */
public class MediaControl
{
    /**
     * Object that will decode media.
     */
    private final MediaCore mCore = new MediaCore();

    /**
     * State of the media.
     */
    private MediaState mState = MediaState.STOPPED;

    /**
     * Media location.
     */
    private String mMedia = null;

    /**
     * Xuggler media reader.
     */
    private IMediaReader mReader = null;

    /**
     * Volume adjustor.
     */
    private VolumeAdjustor mVolumeAdjustor = null;

    /**
     * Thread that will read and decode media.
     */
    private MediaThread mThread = null;

    /**
     * Synchronization object.
     */
    private final Object mSyncThread = new Object();

    /**
     * Constructor.
     */
    public MediaControl()
    {
    }

    /**
     * Get media core.
     * @return MediaCore object
     */
    public MediaCore getMediaCore()
    {
        return mCore;
    }

    /**
     * Get current media state.
     * @return media state
     */
    public MediaState getState()
    {
        return mState;
    }

    /**
     * Load a media (file, URL, ...).
     * @param media media file/URL/...
     */
    public void loadMedia(String media)
    {
        unloadMedia();
        mMedia = media;

        mVolumeAdjustor = new VolumeAdjustor(0.1);
        mReader = ToolFactory.makeReader(mMedia);
        mReader.setAddDynamicStreams(true);
        mReader.addListener(mVolumeAdjustor);
        mVolumeAdjustor.addListener(mCore);
    }

    /**
     * Unload media and release resource.
     */
    public void unloadMedia()
    {
        stop();
        mMedia = null;
        mReader = null;
        mThread = null;
        mVolumeAdjustor = null;
    }

    /**
     * Add a video listener that will be notified when
     * a new image is available.
     * @param listener video listener to add
     */
    public void addVideoListener(VideoListener listener)
    {
        mCore.addVideoListener(listener);
    }

    /**
     * Remove a video listener.
     * @param listener video listener to remove
     */
    public void removeVideoListener(VideoListener listener)
    {
        mCore.removeVideoListener(listener);
    }

    /**
     * Start media.
     */
    public void start()
    {
        synchronized(this)
        {
            if(mState == MediaState.STOPPED)
            {
                mReader.open();
            }

            mState = MediaState.STARTED;
        }

        mCore.resetTimeStamp();

        if(mThread == null)
        {
            mThread = new MediaThread(mReader);
            mThread.start();
        }
        else
        {
            synchronized(mSyncThread)
            {
               mSyncThread.notifyAll();
            }
        }
    }

    /**
     * Stop media.
     */
    public void stop()
    {
        synchronized(this)
        {
            if(mState == MediaState.STOPPED)
            {
                return;
            }

            mState = MediaState.STOPPED;
        }

        mThread = null;
        mReader.close();

        synchronized(mSyncThread)
        {
            mSyncThread.notifyAll();
        }
    }

    /**
     * Pause media.
     */
    public void pause()
    {
        synchronized(this)
        {
            /* pause a stopped stream has no sense */
            if(mState == MediaState.STARTED)
            {
                mState = MediaState.PAUSED;
            }
        }
    }

    /**
     * Seek media to position (percent).
     * @param percent position in stream to seek to.
     */
    public synchronized void seek(int percent)
    {
        if(mReader == null)
        {
            return;
        }

        IContainer container = mReader.getContainer();
        long position = (container.getFileSize() * percent) / 100;

        /* seek for all streams in the media */
        for(int i = 0 ; i < container.getNumStreams() ; i++)
        {
            container.seekKeyFrame(i, position, position, position,
                    IContainer.SEEK_FLAG_BYTE);
        }
    }

    /**
     * Set volume of the media (if sound is present).
     * @param percent percent of the sound (0 to 100).
     */
    public synchronized void setVolume(int percent)
    {
        double value = percent;

        if(value < 0)
        {
            value = 0;
        }

        if(value > 100)
        {
            value = 100;
        }

        mVolumeAdjustor.setVolume(value / 100);
    }

    /**
     * Get duration of the media.
     * @return media duration
     */
    public long getDuration()
    {
        IContainer container = null;

        if(mReader == null)
        {
            return 0;
        }

        container = mReader.getContainer();
        if(container.isOpened())
        {
            return container.getDuration();
        }
        else
        {
            return 0;
        }
    }

    /**
     * Thread that will read and decode media stream.
     *
     * @author Sebastien Vincent
     */
    private class MediaThread extends Thread
    {
        /**
         * Media reader.
         */
        private IMediaReader mReader = null;

        /**
         * Constructor.
         * @param reader media reader
         */
        public MediaThread(IMediaReader reader)
        {
            setName("MediaThread");
            mReader = reader;
        }

        /**
         * Entry point of the thread.
         */
        public void run()
        {
            while(mState != MediaState.STOPPED && mReader.readPacket() == null)
            {
                if(mState == MediaState.PAUSED)
                {
                    /* wait until stop or play */
                    synchronized(mSyncThread)
                    {
                        try
                        {
                            mSyncThread.wait();
                        }
                        catch(Exception e)
                        {
                        }
                    }
                }
            }
        }
    }

    /**
     * Create a tool which adjusts the volume of audio by some constant factor.
     *
     * @author Xuggle
     */
    private class VolumeAdjustor extends MediaToolAdapter
    {
        /**
         * The amount to adjust the volume by.
         */
        private double mVolume;

        /**
         * Construct a volume adjustor.
         *
         * @param volume the volume muliplier, values between 0 and 1 are
         *        recommended.
         */
        public VolumeAdjustor(double volume)
        {
            mVolume = volume;
        }

        /**
         * Set volume.
         * @param volume the volume muliplier, values between 0 and 1 are
         *        recommended.
         */
        public void setVolume(double volume)
        {
            mVolume = volume;
        }

        /**
         * {@inheritDoc}
         */
        @Override
        public void onAudioSamples(IAudioSamplesEvent event)
        {
            // get the raw audio byes and adjust it's value

            ShortBuffer buffer = event.getAudioSamples().getByteBuffer().asShortBuffer();
            for (int i = 0; i < buffer.limit(); ++i)
                buffer.put(i, (short)(buffer.get(i) * mVolume));

            // call parent which will pass the audio onto next tool in chain

            super.onAudioSamples(event);
        }
    }
}

