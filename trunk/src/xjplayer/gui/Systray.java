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

package xjplayer.gui;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.event.*;

/**
 * Systray implementation with Swing popup menu.
 *
 * @author Sebastien Vincent
 */
public final class Systray extends TrayIcon
    implements MouseListener, MouseMotionListener
{
    /**
     * Popup menu for the systray.
     */
    private JPopupMenu mPopupMenu = null;

    /**
     * Active state of frame.
     *
     * This is a hack for some systems:
     * When clicking on the systray, the frame lost focus (win32 issue).
     */
    private boolean mIsFrameActive = false;

    /**
     * Invoker for the systray.
     *
     * This is used as a hack to correctly hide JPopupMenu when clicking other
     * part than the systray.
     * Based on http://weblogs.java.net/blog/alexfromsun/archive/2008/02/jtrayicon_updat.html
     * page.
     */
    private JWindow mWindow = null;

    /**
     * Main frame reference.
     */
    private JFrame mFrame = null;

    /**
     * Title string.
     */
    private String mTitle = null;

    /**
     * Constructor.
     * @param frame main frame
     * @param title frame title
     * @param logo logo image
     */
    public Systray(JFrame frame, String title, Image logo)
    {
        super(logo, title, null);

        mFrame = frame;
        mTitle = title;

        addMouseListener(this);
        addMouseMotionListener(this);

        mWindow = new JWindow();
        mWindow.setAlwaysOnTop(true);
    }

    /**
     * Dispose the systray (destructor).
     *
     * Only call it when exiting the application!
     */
    protected void dispose()
    {
        mWindow.dispose();
        mWindow = null;
        mPopupMenu = null;
    }

    /**
     * Build the JPopupMenu and all of its item.
     * @param menu popup menu to display when right-click on systray
     */
    public void setPopupMenu(JPopupMenu menu)
    {
        /* TrayIcon support PopupMenu but not JPopupMenu. We follow method from
         * http://weblogs.java.net/blog/ixmal/archive/2006/05/using_jpopupmen.html
         * to have JPopupMenu support with TrayIcon.
         */
        if(mPopupMenu != null)
        {
            mPopupMenu = null;
        }

        mPopupMenu = menu;

        /* mPopupMenu.setInvoker(mFrame); */
        mPopupMenu.addPopupMenuListener(new PopupMenuListener()
                {
                    public void popupMenuWillBecomeVisible(PopupMenuEvent e)
                    {
                    }

                    public void popupMenuWillBecomeInvisible(PopupMenuEvent e)
                    {
                        mWindow.setVisible(false);
                    }

                    public void popupMenuCanceled(PopupMenuEvent e)
                    {
                        mWindow.setVisible(false);
                    }
                });
    }

    /**
     * Show the JPopupMenu.
     * @param x x location on the screen
     * @param y y location on the screen
     */
    private void showJPopupMenu(int x, int y)
    {
        /* On JVM < JDK6 u7(b20) when the popup show up,
         * a ClassCastException for TrayIcon (Cannot cast
         * to java.awt.Component) is raised in AWT-EventQueue-0 thread.
         */
        if(!mPopupMenu.isVisible())
        {
            mWindow.setVisible(true);
            mWindow.setLocation(x, y);
            mPopupMenu.show(mWindow, 0, 0);
            mWindow.toFront();
        }
    }

    /**
     * Callback when mouse clicks occurs.
     * @param event mouse event
     */
    public void mouseClicked(MouseEvent event)
    {
        /* left single click we hide/activate frame */
        if(!event.isPopupTrigger() && event.getClickCount() == 1 &&
                !mPopupMenu.isVisible())
        {
            if((mFrame.getExtendedState() & Frame.ICONIFIED) > 0 ||
                    !mFrame.isVisible())
            {
                /* frame is iconified or not visible, back on top and focus
                 * it
                 */
                mIsFrameActive = true;
                mFrame.setExtendedState(Frame.NORMAL);
                mFrame.toFront();
                mFrame.setVisible(true);
                mFrame.requestFocus();
            }
            else if(mIsFrameActive)
            {
                /* frame has the focus, hide the frame */
                mFrame.setVisible(false);
                mIsFrameActive = false;
            }
            else
            {
                /* frame is visible but does not have the focus, bring it to
                 * the top
                 */

                /* hack: to make it work on all system (especially on Linux),
                 * hide and show again the frame
                 */
                mFrame.setVisible(false);
                mIsFrameActive = true;
                mFrame.setExtendedState(Frame.NORMAL);
                mFrame.toFront();
                mFrame.setVisible(true);
                mFrame.requestFocus();
            }
        }
    }

    /**
     * Callback when mouse enter in systray zone.
     *
     * This is not supported for TrayIcon.
     * @param event mouse event
     */
    public void mouseEntered(MouseEvent event)
    {
        /* this method is not supported by TrayIcon class,
         * do not put code here, it will not be proceeded
         */
    }

    /**
     * Callback when mouse exit systray zone.
     *
     * This is not supported for TrayIcon.
     * @param event mouse event
     */
    public void mouseExited(MouseEvent event)
    {
        /* this method is not supported by TrayIcon class,
         * do not put code here, it will not be proceeded
         */
    }

    /**
     * Callback when mouse button is pressed.
     * @param event mouse event
     */
    public void mousePressed(MouseEvent event)
    {
        if(event.isPopupTrigger())
        {
            Point p = event.getLocationOnScreen();
            showJPopupMenu((int)p.getX(), (int)p.getY());
        }
    }

    /**
     * Callback when mouse button is released.
     * @param event mouse event
     */
    public void mouseReleased(MouseEvent event)
    {
        if(event.isPopupTrigger())
        {
            Point p = event.getLocationOnScreen();
            showJPopupMenu((int)p.getX(), (int)p.getY());
        }
    }

    /**
     * Callback when mouse wheel is used.
     * @param event mouse event
     */
    public void mouseWheelMoved(MouseEvent event)
    {
    }

    /**
     * Callback when user dragged over systray.
     * @param event mouse event
     */
    public void mouseDragged(MouseEvent event)
    {
    }

    /**
     * Callback when mouse move in systray zone.
     * @param event mouse event
     */
    public void mouseMoved(MouseEvent event)
    {
        /* hack: to know the real active state on the frame before
         * "mouse clicked" event (MS Windows deactivates frame when
         * proceeding an mouse click!)
         */
        mIsFrameActive = mFrame.isActive();
    }

    /**
     * Show information message.
     * @param msg message to display
     */
    public void displayInfoMessage(String msg)
    {
        displayMessage(mTitle, msg, TrayIcon.MessageType.INFO);
    }

    /**
     * Show warning message.
     * @param msg message to display
     */
    public void displayWarningMessage(String msg)
    {
        displayMessage(mTitle, msg, TrayIcon.MessageType.WARNING);
    }

    /**
     * Show error message.
     * @param msg message to display
     */
    public void displayErrorMessage(String msg)
    {
        displayMessage(mTitle, msg, TrayIcon.MessageType.ERROR);
    }

    /**
     * Show message.
     * @param msg message to display
     */
    public void displayMessage(String msg)
    {
        displayMessage(mTitle, msg, TrayIcon.MessageType.NONE);
    }
}
