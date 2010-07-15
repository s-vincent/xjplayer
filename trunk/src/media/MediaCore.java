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

package media;

import java.util.*;
import java.util.logging.*;
import java.util.concurrent.*;
import java.util.concurrent.locks.*;
import java.util.concurrent.atomic.*;

import java.awt.image.*;

import javax.sound.sampled.*;

import com.xuggle.xuggler.*;
import com.xuggle.xuggler.video.*;
import com.xuggle.mediatool.*;
import com.xuggle.mediatool.event.*;

import static java.util.concurrent.TimeUnit.NANOSECONDS;
import static java.util.concurrent.TimeUnit.MILLISECONDS;
import static java.util.concurrent.TimeUnit.MICROSECONDS;

import media.event.*;

/**
 * Multimedia core class that is able to decode and play audio
 * and video.
 *
 * Parts of the code are taken from Xuggler's MediaViewer.
 *
 * @author Sebastien Vincent
 * @author Xuggle
 */
public class MediaCore extends MediaListenerAdapter
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(MediaCore.class.getName());

    /**
     * The capacity (in time) of media buffers.
     */
    private long mVideoQueueCapacity = TIME_UNIT.convert(1000, MILLISECONDS);

    /**
     * The capacity (in time) of media buffers.
     */
    private long mAudioQueueCapacity = TIME_UNIT.convert(1000, MILLISECONDS);

    /**
     * The standard time unit used in the media viewer.
     */
    private static final TimeUnit TIME_UNIT = MICROSECONDS;

    /**
     * Default video early time window, before which video is delayed.
     */
    private static final long DEFAULT_VIDEO_EARLY_WINDOW =
        TIME_UNIT.convert(50, MILLISECONDS);

    /**
     * Default video late time window, after which video is dropped.
     */
    private static final long DEFAULT_VIDEO_LATE_WINDOW =
        TIME_UNIT.convert(50, MILLISECONDS);

    /**
     * Default audio early time window, before which audio is delayed.
     */
    private static final long DEFAULT_AUDIO_EARLY_WINDOW =
        TIME_UNIT.convert(Long.MAX_VALUE, MILLISECONDS);

    /**
     * Default audio late time window, after which audio is dropped.
     */
    private static final long DEFAULT_AUDIO_LATE_WINDOW =
        TIME_UNIT.convert(Long.MAX_VALUE, MILLISECONDS);

    /**
     * Start clock time.
     */
    private AtomicLong mStartClockTime = new AtomicLong(Global.NO_PTS);

    /**
     * The container which is to be viewed.
     */
    private IContainer mContainer = null;

    /**
     * The authoratative data line used play media.
     */
    private SourceDataLine mDataLine = null;

    /**
     * Audio lines.
     */
    private final Map<Integer, SourceDataLine> mAudioLines =
        new HashMap<Integer, SourceDataLine>();

    /**
     * Video converters.
     */
    private final Map<Integer, IConverter> mVideoConverters =
        new HashMap<Integer, IConverter>();

    /**
     * Video queues.
     */
    private final Map<Integer, VideoQueue> mVideoQueues =
        new HashMap<Integer, VideoQueue>();

    /**
     * Audio queues.
     */
    private final Map<Integer, AudioQueue> mAudioQueues =
        new HashMap<Integer, AudioQueue>();

    /**
     * List of video listeners, typically panels that wait
     * images to display.
     */
    private List<VideoListener> mListeners =
        new ArrayList<VideoListener>();

    /**
     * Is this viewer in the process of closing.
     */
    private boolean mClosing = false;

    /**
     * Last media time.
     */
    private long mLastMediaTime = 0;

    /**
     * If timestamp has been resetted (pause, ...).
     */
    private boolean mResetted = false;

    /**
     * Constructor.
     */
    public MediaCore()
    {
    }

    /**
     * Add a video listeners.
     * @param listener listener to add
     */
    public void addVideoListener(VideoListener listener)
    {
        if(!mListeners.contains(listener))
        {
            mListeners.add(listener);
        }
    }

    /**
     * Remove a video listeners.
     * @param listener listener to remove
     */
    public void removeVideoListener(VideoListener listener)
    {
        if(mListeners.contains(listener))
        {
            mListeners.remove(listener);
        }
    }

    /**
     * Notify all listeners that a new image is available.
     * @param event NewImageEvent to fire
     */
    public void fireNewImageEvent(NewImageEvent event)
    {
        for(VideoListener l : mListeners)
        {
            l.newImage(event);
        }
    }

    /**
     * Notify all listeners the end of the video.
     * @param event VideoEvent to fire
     */
    public void fireEndOfVideoEvent(VideoEvent event)
    {
        for(VideoListener l : mListeners)
        {
            l.endOfVideo(event);
        }
    }

    /**
     * Get media time.  This is time used to choose to delay, present, or
     * drop a media frame.
     *
     * @return the current presentation time of the media
     */
    private long getMediaTime()
    {
        long now = TIME_UNIT.convert(System.nanoTime(), NANOSECONDS);

        if(mResetted && mLastMediaTime != 0)
        {
            mStartClockTime.compareAndSet(Global.NO_PTS, now - mLastMediaTime);
        }
        else
        {
            mStartClockTime.compareAndSet(Global.NO_PTS, now);
        }

        if(mResetted)
        {
            mResetted = false;
        }

        mLastMediaTime = now - mStartClockTime.get();
        return mLastMediaTime;
    }

    /**
     * Get the audio queue for stream index. If not present, a new queue
     * will be created.
     * @param tool ???
     * @param streamIndex index of the audio stream
     * @return audio queue for the stream index
     */
    private AudioQueue getAudioQueue(IMediaGenerator tool, int streamIndex)
    {
        if(!(tool instanceof IMediaCoder))
            throw new UnsupportedOperationException();

        AudioQueue queue = mAudioQueues.get(streamIndex);
        IStream stream = ((IMediaCoder)tool).getContainer().getStream(streamIndex);
        SourceDataLine line = getJavaSoundLine(stream);

        // if no queue (and there is a line), create the queue

        if(null == queue && line != null)
        {
            queue = new AudioQueue(mAudioQueueCapacity, TIME_UNIT, stream, line);
            mAudioQueues.put(streamIndex, queue);
        }

        return queue;
    }

    /**
     * Get video queue for stream index. If not present, a new queue
     * will be created.
     * @param streamIndex index of video stream
     * @return video queue for the stream index
     */
    private VideoQueue getVideoQueue(int streamIndex)
    {
        VideoQueue queue = mVideoQueues.get(streamIndex);

        if(null == queue)
        {
            queue = new VideoQueue(mVideoQueueCapacity, TIME_UNIT, streamIndex);
            mVideoQueues.put(streamIndex, queue);
        }
        return queue;
    }

    /**
     * Open a java audio line out to play the audio samples into.
     *
     * @param stream the stream we'll be decoding in to this line.
     * @return the line
     */
    private SourceDataLine getJavaSoundLine(IStream stream)
    {
        IStreamCoder audioCoder = stream.getStreamCoder();
        int streamIndex = stream.getIndex();
        SourceDataLine line = mAudioLines.get(streamIndex);

        if(line == null)
        {
            try
            {
                // estabish the audio format, NOTE: xuggler defaults to signed 16 bit
                // samples

                AudioFormat audioFormat = new AudioFormat(audioCoder.getSampleRate(),
                        (int) IAudioSamples
                        .findSampleBitDepth(audioCoder.getSampleFormat()), audioCoder
                        .getChannels(), true, false);

                // create the audio line out

                DataLine.Info info = new DataLine.Info(SourceDataLine.class,
                        audioFormat);
                line = (SourceDataLine) AudioSystem.getLine(info);

                // open the line and start the line

                line.open(audioFormat);
                line.start();
                mAudioLines.put(streamIndex, line);

                // if mDataLine is not yet defined, do so

                if(null == mDataLine)
                    mDataLine = line;
            }
            catch (LineUnavailableException lue)
            {
                logger.warning("WARNING: No audio line out available: " + lue);
                line = null;
            }
        }
        return line;
    }

    /**
     * Play audio samples.
     *
     * @param stream the source stream of the audio
     * @param line the audio line to play audio samples on
     * @param samples the audio samples
     */
    private void playAudio(IStream stream, SourceDataLine line,
            IAudioSamples samples)
    {
        if(!mClosing)
        {
            int size = samples.getSize();
            line.write(samples.getData().getByteArray(0, size), 0, size);
        }
    }

    /**
     * Display video picture by notifying listeners that a new
     * image is available.
     *
     * @param picture picture to display on panel
     */
    private void displayVideoImage(IVideoPicture picture, int streamIndex)
    {
        /* convert if any */
        IConverter converter = mVideoConverters.get(streamIndex);

        if(converter != null)
        {
            BufferedImage image = converter.toImage(picture);

            NewImageEvent evt = new NewImageEvent(this, image, picture.getTimeStamp());

            /* notify listeners */
            fireNewImageEvent(evt);
        }
    }

    /**
     * Flush all media buffers.
     */
    private void flush()
    {
        // flush all the video queues

        for(VideoQueue queue : mVideoQueues.values())
            queue.flush();

        // flush all the audio queus

        for(AudioQueue queue : mAudioQueues.values())
            queue.flush();

        // wait for all audio lines to drain

        for(SourceDataLine line : mAudioLines.values())
            line.drain();
    }

    /**
     * Reset internal timestamp.
     */
    public void resetTimeStamp()
    {
        mStartClockTime = new AtomicLong(Global.NO_PTS);
        mResetted = true;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onOpen(IOpenEvent event)
    {
        mContainer = event.getSource().getContainer();
        mStartClockTime = new AtomicLong(Global.NO_PTS);
        mLastMediaTime = 0;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onClose(ICloseEvent event)
    {
        // note that we are closing

        mClosing = true;

        // flush buffers

        flush();

        // close all audio and video queues

        for(AudioQueue queue: mAudioQueues.values())
            queue.close();
        for(VideoQueue queue: mVideoQueues.values())
            queue.close();

        mAudioQueues.clear();
        mVideoQueues.clear();

        mVideoConverters.clear();

        // close audio lines

        for(SourceDataLine line : mAudioLines.values())
        {
            line.stop();
            line.close();
        }
        mAudioLines.clear();

        mDataLine = null;

        // note that we done closing


        VideoEvent evt = new VideoEvent(this);
        fireEndOfVideoEvent(evt);

        mLastMediaTime = 0;
        mContainer = null;
        mClosing = false;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAddStream(IAddStreamEvent event)
    {
        // get the coder, and stream index

        IContainer container = event.getSource().getContainer();
        IStream stream = container.getStream(event.getStreamIndex());
        IStreamCoder coder = stream.getStreamCoder();
        int streamIndex = event.getStreamIndex();

        // if video stream and showing video, configure video stream

        if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_VIDEO)
        {
            // create a converter for this video stream

            IConverter converter = mVideoConverters.get(streamIndex);
            if(null == converter)
            {
                converter = ConverterFactory.createConverter(
                ConverterFactory.XUGGLER_BGR_24, coder.getPixelType(), coder
                    .getWidth(), coder.getHeight());
                mVideoConverters.put(streamIndex, converter);
            }

            // if real time establish video queue
            getVideoQueue(streamIndex);
        }
        // if audio stream and playing audio, configure audio stream
        else if(coder.getCodecType() == ICodec.Type.CODEC_TYPE_AUDIO)
        {
            // if real time establish audio queue
            getAudioQueue(event.getSource(), streamIndex);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onVideoPicture(IVideoPictureEvent event)
    {
        // verify container is defined

        if(null == mContainer)
        {
            // if source does not posses a container then throw exception

            if(!(event.getSource() instanceof IMediaCoder))
                throw new UnsupportedOperationException();

            // establish container

            mContainer = ((IMediaCoder)event.getSource()).getContainer();
        }

        // if in real time, queue the video frame for viewing

        getVideoQueue(event.getStreamIndex())
            .offerMedia(event.getPicture(), event.getTimeStamp(), MICROSECONDS);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void onAudioSamples(IAudioSamplesEvent event)
    {
        // verify container is defined

        if(null == mContainer)
        {
            // if source does not posses a container then throw exception

            if(!(event.getSource() instanceof IMediaCoder))
                throw new UnsupportedOperationException();

            // establish container

            mContainer = ((IMediaCoder)event.getSource()).getContainer();
        }

        // establish the audio samples

        final IAudioSamples samples = event.getAudioSamples();

        // establish audio queue

        AudioQueue queue = getAudioQueue(event.getSource(),
            event.getStreamIndex());

        // enqueue the audio samples

        if(queue != null)
            queue.offerMedia(samples, event.getTimeStamp(), event.getTimeUnit());
    }

    /**
     * A queue of audio sampless which automatically plays audio frames at the
     * correct time.
     *
     * @author Xuggle
     */
    private class AudioQueue extends SelfServicingMediaQueue
    {
        /**
         * Serial version UID.
         */
        public static final long serialVersionUID = 0;

        /**
         * The audio line.
         */
        private final SourceDataLine mLine;

        /**
         * Source audio stream.
         */
        private final IStream mStream;

        /**
         * Construct queue and activate it's internal thread.
         *
         * @param capacity the total duraiton of media stored in the queue
         * @param unit the time unit of the capacity (MILLISECONDS,
         *        MICROSECONDS, etc).
         * @param stream the stream from whence the audio issued forth
         * @param sourceDataLine the swing frame on which samples are
         *          displayed
         */
        public AudioQueue(long capacity, TimeUnit unit, IStream stream,
                SourceDataLine sourceDataLine)
        {
            super(TIME_UNIT.convert(capacity, unit), DEFAULT_AUDIO_EARLY_WINDOW,
                    DEFAULT_AUDIO_LATE_WINDOW, TIME_UNIT, Thread.MIN_PRIORITY,
                    "audio stream " + stream.getIndex() + " " +
                    stream.getStreamCoder().getCodec().getLongName(),
                    stream.getIndex());
            mStream = stream;
            mLine = sourceDataLine;
        }

        /**
         * {@inheritDoc}
         */
        public void dispatch(IMediaData samples, long timeStamp)
        {
            if(samples instanceof IAudioSamples)
            {
                playAudio(mStream, mLine, (IAudioSamples)samples);
            }
        }
    }

    /**
     * A queue of video images which automatically displays video frames at the
     * correct time.
     *
     * @author Xuggle
     */
    private class VideoQueue extends SelfServicingMediaQueue
    {
        /**
         * Serial version UID.
         */
        public static final long serialVersionUID = 0;

        /**
         * Index of the stream.
         */
        private int mStreamIndex = -1;

        /**
         * Construct queue and activate it's internal thread.
         *
         * @param capacity
         *          the total duraiton of media stored in the queue
         * @param unit
         *          the time unit of the capacity (MILLISECONDS, MICROSECONDS, etc).
         * @param streamIndex
         *          index of the stream
         */
        public VideoQueue(long capacity, TimeUnit unit, int streamIndex)
        {
            super(TIME_UNIT.convert(capacity, unit), DEFAULT_VIDEO_EARLY_WINDOW,
                    DEFAULT_VIDEO_LATE_WINDOW, TIME_UNIT, Thread.MIN_PRIORITY,
                    "video stream ", streamIndex);
            mStreamIndex = streamIndex;
        }

        /**
         * {@inheritDoc}
         */
        public void dispatch(IMediaData picture, long timeStamp)
        {
            if(picture instanceof IVideoPicture)
            {
                displayVideoImage((IVideoPicture)picture, mStreamIndex);
            }
        }
    }

    /**
     * When created, this queue start a thread which extracts media frames in a
     * timely way and presents them to the analog hole (viewer).
     *
     * @author Xuggle
     */
    private abstract class SelfServicingMediaQueue
    {
        /**
         * to make warning go away
         */
        private static final long serialVersionUID = 1L;

        /**
         * Queue of delayed items.
         */
        private final Queue<DelayedItem<IMediaData>> mQueue = new
            LinkedList<DelayedItem<IMediaData>>();

        /**
         * Stream index this queue is servicing.
         */
        private final int mStreamIndex;

        /**
         * The lock.
         */
        private ReentrantLock mLock = new ReentrantLock(true);

        /**
         * The locks condition.
         */
        private Condition mCondition = mLock.newCondition();

        /**
         * if true the queue terminates it's thread.
         */
        private boolean mDone = false;

        /**
         * The maximum amount of media which will be stored in the buffer.
         */
        private final long mCapacity;

        /**
         * The time before which media is delayed.
         */
        private final long mEarlyWindow;

        /**
         * The time after which media is dropped.
         */
        private final long mLateWindow;

        /**
         * If the queue is initialized.
         */
        private boolean mIsInitialized = false;

        /**
         * Construct queue and activate it's internal thread.
         *
         * @param capacity the total duraiton of media stored in the queue
         * @param earlyWindow the time before which media is delayed
         * @param lateWindow the time after which media is dropped
         * @param unit the time unit for capacity and window values
         *        (MILLISECONDS, MICROSECONDS, etc).
         * @param priority internal thread priority
         * @param name name which is attached to internal thread
         * @param streamIndex the index of stream this queue is working on
         */
        public SelfServicingMediaQueue(long capacity, long earlyWindow,
                long lateWindow, TimeUnit unit, int priority, String name,
                int streamIndex)
        {
            // record capacity, and window and stream index

            mCapacity = TIME_UNIT.convert(capacity, unit);
            mEarlyWindow = TIME_UNIT.convert(earlyWindow, unit);
            mLateWindow = TIME_UNIT.convert(lateWindow, unit);
            mStreamIndex = streamIndex;

            // create and start the thread

            Thread t = new Thread(name)
            {
                public void run()
                {
                    try
                    {
                        boolean isDone = false;
                        DelayedItem<IMediaData> delayedItem = null;

                        // wait for all the other stream threads to wakeup
                        synchronized (SelfServicingMediaQueue.this)
                        {
                            mIsInitialized = true;
                            SelfServicingMediaQueue.this.notifyAll();
                        }
                        // start processing media

                        while(!isDone)
                        {
                            // synchronized (SelfServicingMediaQueue.this)

                            mLock.lock();
                            try
                            {
                                // while not done, and no item, wait for one

                                while(!mDone && (delayedItem = mQueue.poll()) == null)
                                {
                                    try
                                    {
                                        mCondition.await();
                                    }
                                    catch (InterruptedException e)
                                    {
                                        // interrupt and return
                                        Thread.currentThread().interrupt();

                                        return;
                                    }
                                }

                                // notify the queue that data extracted

                                mCondition.signalAll();

                                // record "atomic" done

                                isDone = mDone;
                            }
                            finally
                            {
                                mLock.unlock();
                            }

                            // if there is an item, dispatch it
                            if(null != delayedItem)
                            {
                                IMediaData item = delayedItem.getItem();

                                try
                                {
                                    do
                                    {
                                        // this is the story of goldilocks testing the the media

                                        long now = getMediaTime();
                                        long delta = delayedItem.getTimeStamp() - now;

                                        // if the media is too new and unripe, goldilocks sleeps
                                        // for a bit

                                        if(delta >= mEarlyWindow)
                                        {
                                            try
                                            {
                                                //sleep(MILLISECONDS.convert(delta - mEarlyWindow, TIME_UNIT));
                                                sleep(MILLISECONDS.convert(delta / 3, TIME_UNIT));
                                            }
                                            catch (InterruptedException e)
                                            {
                                                // interrupt and return
                                                Thread.currentThread().interrupt();
                                                return;
                                            }
                                        }
                                        else
                                        {
                                            // if the media is old and moldy, goldilocks says
                                            // "ick" and drops the media on the floor

                                            if(delta < -mLateWindow)
                                            {
                                            	logger.warning("Stream " + mStreamIndex + ": drop frame");
                                            }

                                            // if the media is just right, goldilocks dispaches it
                                            // for presentiation becuse she's a badass bitch

                                            else
                                            {
                                                dispatch(item, delayedItem.getTimeStamp());

                                                // debug("%5d show [%2d]: %s[%5d] delta: %d",
                                                // MILLISECONDS.convert(getPresentationTime(), TIME_UNIT),
                                                // size(),
                                                // (delayedItem.getItem() instanceof BufferedImage
                                                // ? "IMAGE"
                                                // : "sound"),
                                                // MILLISECONDS.convert(delayedItem.getTimeStamp(),
                                                // TIME_UNIT),
                                                // MILLISECONDS.convert(delta, TIME_UNIT));
                                            }

                                            // and the moral of the story is don't mess with goldilocks

                                            break;
                                        }
                                    }
                                    while(!mDone);
                                }
                                finally
                                {
                                    if(item != null)
                                        item.delete();
                                }
                            }
                        }
                    }
                    finally
                    {
                    }
                }
            };

            t.setPriority(priority);
            t.setDaemon(true);

            synchronized (this)
            {
                t.start();
                try
                {
                    while(!mIsInitialized)
                        this.wait();
                }
                catch (InterruptedException e)
                {
                    // interrupt and return

                    Thread.currentThread().interrupt();

                    throw new RuntimeException("could not start thread");
                }
            }
        }

        /**
         * Block until all data is extracted from the buffer.
         */
        public void flush()
        {
            // synchronized (SelfServicingMediaQueue.this)

            mLock.lock();
            try
            {
                while(!mDone && !mQueue.isEmpty())
                {
                    try
                    {
                        mCondition.await();
                    }
                    catch (InterruptedException e)
                    {
                        // interrupt and return
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                mCondition.signalAll();

            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         * Dispatch an item just removed from the queue.
         *
         * @param item
         *          the item just removed from the queue
         * @param timeStamp
         *          the presentation time stamp of the item
         */
        public abstract void dispatch(IMediaData item, long timeStamp);

        /**
         * Place an item onto the queue, if the queue is full, block.
         *
         * @param item
         *          the media item to be placed on the queue
         * @param timeStamp
         *          the presentation time stamp of the item
         * @param unit
         *          the time unit of the time stamp
         */
        public void offerMedia(IMediaData item, long timeStamp, TimeUnit unit)
        {
            // wait for all the other stream threads to wakeup

            // try {mBarrier.await(250, MILLISECONDS);}
            // catch (InterruptedException ex) { return; }
            // catch (BrokenBarrierException ex) { return; }
            // catch (TimeoutException ex) {}

            // convert time stamp to standar time unit

            long convertedTime = TIME_UNIT.convert(timeStamp, unit);

            // synchronized (SelfServicingMediaQueue.this)

            mLock.lock();
            try
            {
                // while not done, and over the buffer capacity, wait till media
                // is draied below it's capacity

                while(!mDone && !mQueue.isEmpty()
                        && (convertedTime - mQueue.peek().getTimeStamp()) > mCapacity)
                {
                    try
                    {
                        // log.debug("Reader blocking; ts:{}; media:{}",
                        // timeStamp, item);
                        mCondition.await();
                    }
                    catch (InterruptedException e)
                    {
                        // interrupt and return
                        Thread.currentThread().interrupt();
                        return;
                    }
                }

                // if not done, put item on the queue

                if(!mDone)
                {
                    // debug("1     queue[%2d]: %s[%5d]",
                    // size(),
                    // (item instanceof BufferedImage ? "IMAGE" : "SOUND"),
                    // MILLISECONDS.convert(convertedTime, TIME_UNIT));

                    // put a COPY on the queue

                    mQueue.offer(new DelayedItem<IMediaData>(item.copyReference(),
                                convertedTime));

                    // debug("2     queue[%2d]: %s[%5d]",
                    // size(),
                    // (item instanceof BufferedImage ? "IMAGE" : "SOUND"),
                    // MILLISECONDS.convert(convertedTime, TIME_UNIT));
                }

                // notify that things have changed

                mCondition.signalAll();
            }
            finally
            {
                mLock.unlock();
            }
        }

        /**
         * Stipulate that this queue is terminate it's internal thread.
         */
        public void close()
        {
            mLock.lock();

            try
            {
                mDone = true;
                mCondition.signalAll();
            }
            finally
            {
                mLock.unlock();
            }
        }
    }

    /**
     * A place to put frames which will be delayed.
     *
     * @author Xuggle
     */
    private class DelayedItem<Item extends IMediaData>
    {
        /**
         * Buffered image.
         */
        private final Item mItem;

        /**
         * Timestamp.
         */
        private final long mTimeStamp;

        /**
         * Construct a delayed frame.
         * @param item item to be delayed
         * @param timeStamp timestamp of the item
         */
        public DelayedItem(Item item, long timeStamp)
        {
            mItem = item;
            mTimeStamp = timeStamp;
        }

        /**
         * Get the buffered item.
         * @return delayed buffered item
         */
        public Item getItem()
        {
            return mItem;
        }

        /**
         * Get the timestamp.
         * @return timestamp
         */
        public long getTimeStamp()
        {
            return mTimeStamp;
        }
    }
}

