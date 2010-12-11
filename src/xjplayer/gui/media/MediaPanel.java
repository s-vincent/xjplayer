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

package xjplayer.gui.media;

import java.util.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;
import javax.swing.filechooser.*;

import xjplayer.media.*;
import xjplayer.media.event.*;

import xjplayer.resources.*;

/**
 * Media panel that contains video part and some controls such as
 * volume or open/play/pause/stop buttons.
 *
 * @author Sebastien Vincent
 */
public class MediaPanel extends JPanel implements ActionListener,
       ChangeListener, VideoListener
{
    /**
     * Serial version UID.
     */
    public static final long serialVersionUID = 0L;

    /**
     * Resource manager instance.
     */
    private static final ResourceManager mRes = ResourceManager.getInstance();

    /**
     * Icon of the play button.
     */
    private static final Icon mIconPlay = new ImageIcon(mRes.loadImage("MEDIA_CONTROL_PLAY"));

    /**
     * Icon of the pause button.
     */
    private static final Icon mIconPause = new ImageIcon(mRes.loadImage("MEDIA_CONTROL_PAUSE"));

    /**
     * Icon of the stop button.
     */
    private static final Icon mIconStop = new ImageIcon(mRes.loadImage("MEDIA_CONTROL_STOP"));

    /**
     * Media control that will interact with MediaCore to
     * provide decoded images to panel and play sound.
     */
    private MediaControl mControl = new MediaControl();

    /**
     * Video panel.
     */
    private VideoPanel mPanelVideo = new VideoPanel();

    /**
     * Stop button.
     */
    private JButton mBtnStop = new JButton(mIconStop);

    /**
     * Play/pause button.
     */
    private JButton mBtnPlayPause = new JButton(mIconPlay);

    /**
     * Open media button.
     */
    private JButton mBtnOpen = new JButton("...");

    /**
     * Volume slider.
     */
    private JSlider mSliderVolume = null;

    /**
     * Media seek slider.
     */
    private JSlider mSliderSeek = null;

    /**
     * Text that display current media time.
     */
    private JTextField mTxtSeek = null;

    /**
     * Current media time.
     */
    private long mLastSeekTime = 0;

    /**
     * Reference timestamp if we have seek at least one time.
     */
    private long mRefTimeStamp = 0;

    /**
     * If user has seek media.
     */
    private boolean mHasSeeked = false;

    /**
     * If user has explicitely seek slider.
     */
    private boolean mHasSeekSlider = false;

    /**
     * Synchronization object when seeking.
     */
    private final Object mSyncSlider = new Object();

    /**
     * Current presentation timestamp.
     */
    private long mPts = 0;

    /**
     * Current time.
     */
    private double mCurrentTime = 0;

    /**
     * If panel is in fullscreen mode or not.
     */
    private boolean mFullScreen = false;

    /**
     * Constructor.
     */
    public MediaPanel()
    {
        super(new BorderLayout());
        JPanel panelSouth = new JPanel(new GridLayout(2, 0));
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.CENTER));
        JPanel panelSeek = new JPanel(new GridBagLayout());
        GridBagConstraints c = new GridBagConstraints();

        mControl.addVideoListener(mPanelVideo);
        mControl.addVideoListener(this);
        mPanelVideo.addKeyListener(new KeyAdapter()
                {
                    public void keyPressed(KeyEvent event)
                    {
                        if(event.getSource() == mPanelVideo)
                        {
                            if(mFullScreen)
                            {
                                if(event.getKeyCode() == KeyEvent.VK_ESCAPE)
                                {
                                    setFullScreen(false);
                                }
                            }
                        }
                    }
                });

        this.add(mPanelVideo, BorderLayout.CENTER);

        mBtnPlayPause.setAlignmentX(Component.CENTER_ALIGNMENT);
        mBtnPlayPause.addActionListener(this);
        mBtnPlayPause.setSize(mBtnPlayPause.getMinimumSize());
        mBtnPlayPause.setEnabled(false);
        mBtnStop.setAlignmentX(Component.CENTER_ALIGNMENT);
        mBtnStop.addActionListener(this);
        mBtnStop.setSize(mBtnStop.getMinimumSize());
        mBtnOpen.setAlignmentX(Component.CENTER_ALIGNMENT);
        mBtnOpen.addActionListener(this);
        mBtnOpen.setSize(mBtnOpen.getMinimumSize());

        mSliderVolume = new JSlider(JSlider.HORIZONTAL, 0, 100, 100);
        mSliderVolume.setEnabled(false);
        mSliderVolume.setMinorTickSpacing(1);
        mSliderVolume.setMajorTickSpacing(1);
        mSliderVolume.addChangeListener(this);

        mSliderSeek = new JSlider(JSlider.HORIZONTAL, 0, 100, 0);
        mSliderSeek.setEnabled(false);
        mSliderSeek.setMinorTickSpacing(1);
        mSliderSeek.setMajorTickSpacing(1);
        mSliderSeek.addChangeListener(this);
        mSliderSeek.addMouseListener(new MouseAdapter()
                {
                    @Override
                    public void mousePressed(MouseEvent e)
                    {
                        mHasSeekSlider = true;
                    }

                    @Override
                    public void mouseReleased(MouseEvent e)
                    {
                        synchronized(mSyncSlider)
                        {
                            mHasSeeked = true;
                            mHasSeekSlider = false;
                        }
                    }
                });

        mTxtSeek = new JTextField("00:00 / 00:00");
        mTxtSeek.setEnabled(false);

        panelBtn.add(mBtnStop);
        panelBtn.add(mBtnPlayPause);
        panelBtn.add(mBtnOpen);
        panelBtn.add(mSliderVolume);

        c.fill = GridBagConstraints.HORIZONTAL;
        c.gridx = 0;
        c.gridy = 0;
        c.gridwidth = 2;
        c.weightx = 0.99;
        panelSeek.add(mSliderSeek, c);

        c.gridx = 2;
        c.gridy = 0;
        c.weightx = 0.01;
        c.gridwidth = GridBagConstraints.REMAINDER;
        panelSeek.add(mTxtSeek, c);

        panelSouth.add(panelSeek);
        panelSouth.add(panelBtn);
        this.add(panelSouth, BorderLayout.SOUTH);
    }

    /**
     * Dispose the panel and all media controls.
     */
    public void dispose()
    {
        unloadMedia();

        mBtnStop = null;
        mBtnPlayPause = null;
        mBtnOpen = null;

        mSliderSeek = null;
        mSliderVolume = null;

        mTxtSeek = null;

        mControl = null;
        mPanelVideo = null;
    }

    /**
     * Repaint the panel.
     */
    public void repaint()
    {
        super.repaint();

        if(mControl == null || mTxtSeek == null || mSliderSeek == null)
        {
            return;
        }

        /* display time */
        long pts = mPts / 1000000;
        long duration = mControl.getDuration() / 1000000;
        long m = 0;
        long s = 0;
        long m2 = 0;
        long s2 = 0;

        m = pts / 60;
        s = pts % 60;

        m2 = duration / 60;
        s2 = duration % 60;

        Formatter formater = new Formatter();
        mTxtSeek.setText(formater.format(
                    "%1$02d:%2$02d: / %3$02d:%4$02d", m, s,
                    m2, s2).toString());

        if(!mHasSeekSlider)
        {
            mSliderSeek.setValue((int)(mCurrentTime * 100));
        }
    }

    /**
     * Load a media.
     * @param media media to load
     */
    public void loadMedia(String media)
    {
        unloadMedia();
        mControl.loadMedia(media);
        mControl.start();
        mControl.setVolume(100);
        mBtnPlayPause.setIcon(mIconPause);
        mBtnPlayPause.setEnabled(true);
        mSliderSeek.setEnabled(true);
        mSliderVolume.setEnabled(true);
    }

    /**
     * Unload media.
     */
    public void unloadMedia()
    {
        mBtnPlayPause.setIcon(mIconPlay);
        mBtnPlayPause.setEnabled(false);
        mControl.unloadMedia();
        mSliderSeek.setValue(0);
        mTxtSeek.setText("00:00 / 00:00");
        mLastSeekTime = 0;
        mRefTimeStamp = 0;
        mPts = 0;
        mSliderSeek.setEnabled(false);
        mSliderVolume.setEnabled(false);
        mCurrentTime = 0;
    }

    /**
     * Show the file chooser and load the media.
     */
    public void showOpenFile()
    {
        JFileChooser chooser = new JFileChooser();
        FileFilter defaultFilter = chooser.getFileFilter();

        /* default filter is all files */
        chooser.setFileFilter(defaultFilter);
        chooser.setMultiSelectionEnabled(false);

        if(chooser.showOpenDialog(this) == JFileChooser.APPROVE_OPTION)
        {
            try
            {
                loadMedia(chooser.getSelectedFile().toString());
            }
            catch(Exception e)
            {
                JOptionPane.showMessageDialog(null, "Cannot open file",
                        "XJPlayer error", JOptionPane.ERROR_MESSAGE);
            }
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public synchronized void newImage(NewImageEvent event)
    {
        if(mControl.getState() != MediaState.STOPPED)
        {
            synchronized(mSyncSlider)
            {
                if(mHasSeeked)
                {
                    mRefTimeStamp = event.getPts();
                    mHasSeeked = false;
                }

                double pts = mLastSeekTime + (event.getPts() - mRefTimeStamp);
                double duration = mControl.getDuration();
                mCurrentTime = pts / duration;

                /* System.out.println("Percent of media: " + time * 100); */
                mPts = (long)pts;
            }
        }
        repaint();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void endOfVideo(VideoEvent event)
    {
        /* do nothing */
    }

    /**
     * Callback when user clicked on a button/menu.
     * @param event action event
     */
    @Override
    public void actionPerformed(ActionEvent event)
    {
        Object src = event.getSource();

        if(src == mBtnPlayPause)
        {
            if(mControl.getState() == MediaState.STARTED)
            {
                mControl.pause();
                mBtnPlayPause.setIcon(mIconPlay);
            }
            else
            {
                mControl.start();
                mBtnPlayPause.setIcon(mIconPause);
            }
        }
        else if(src == mBtnStop)
        {
            mControl.stop();
            mBtnPlayPause.setIcon(mIconPlay);
            mSliderSeek.setValue(0);
            mLastSeekTime = 0;
            mRefTimeStamp = 0;
            mPts = 0;
            mHasSeeked = false;
            mHasSeekSlider = false;
            mCurrentTime = 0;
        }
        else if(src == mBtnOpen)
        {
            showOpenFile();
        }

        repaint();
    }

    /**
     * Callback when slider change its state.
     * @param event ChangeEvent received
     */
    @Override
    public void stateChanged(ChangeEvent event)
    {
        if(event.getSource() == mSliderVolume)
        {
            int value = mSliderVolume.getValue();
            mControl.setVolume(value);
        }
        else if(event.getSource() == mSliderSeek)
        {
            if(mHasSeekSlider)
            {
                /* compute current media time */
                mControl.seek(mSliderSeek.getValue());
                mLastSeekTime = (mControl.getDuration() * mSliderSeek.getValue()) / 100;
            }
        }
    }

    /**
     * Change to or exit fullscreen mode.
     * @param fullscreen true to set fullscreen, false to exit fullscreen mode
     */
    private void setFullScreen(boolean fullscreen)
    {
        if(mFullScreen == fullscreen)
        {
            return;
        }

        /* TODO */

        mFullScreen = fullscreen;
    }
}

