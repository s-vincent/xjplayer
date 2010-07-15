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

package gui;

import java.util.logging.*;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;
import javax.swing.border.*;

import gui.media.*;
import gui.dialog.*;
import resources.*;

/**
 * Main frame of the application.
 *
 * @author Sebastien Vincent
 */
public class MainFrame extends JFrame implements WindowListener, ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(MainFrame.class.getName());

    /**
     * Resource manager instance.
     */
    private static final ResourceManager mRes = ResourceManager.getInstance();

    /**
     * Frame title.
     */
    private String mTitle = null;

    /**
     * Systray.
     */
    private Systray mSystray = null;

    /**
     * Menu bar.
     */
    private JMenuBar mMenuBar = null;

    /**
     * Open item.
     */
    private JMenuItem mOpenItem = null;

    /**
     * Close item.
     */
    private JMenuItem mCloseItem = null;

    /**
     * Preferences item.
     */
    private JMenuItem mPreferencesItem = null;

    /**
     * Help item.
     */
    private JMenuItem mHelpItem = null;

    /**
     * About item.
     */
    private JMenuItem mAboutItem = null;

    /**
     * Close item for systray.
     */
    private JMenuItem mCloseItemTray = null;

    /**
     * Preferences item for systray.
     */
    private JMenuItem mPreferencesItemTray = null;

    /**
     * Help item for mSystray.
     */
    private JMenuItem mHelpItemTray = null;

    /**
     * About item for systray.
     */
    private JMenuItem mAboutItemTray = null;

    /**
     * ConfigurationDialog reference.
     */
    private ConfigurationDialog mConfigurationDialog = null;

    /**
     * HelpDialog reference.
     */
    private HelpDialog mHelpDialog = null;

    /**
     * AboutDialog reference.
     */
    private AboutDialog mAboutDialog = null;

    /**
     * Indication bar.
     */
    private JLabel mStatusBar = null;

    /**
     * Media panel.
     */
    private MediaPanel mMediaPanel = null;

    /**
     * Constructor.
     * @param title frame title
     */
    public MainFrame(String title)
    {
        super(title);

        mTitle = title;

        /* main panel */
        JPanel mainPanel = new JPanel(new BorderLayout());
        Border border = BorderFactory.createLoweredBevelBorder();

        mStatusBar = new JLabel();
        mStatusBar.setBorder(border);
        setStatusBarText(title);

        /* add application logo */
        setIconImage(mRes.loadImage("APPLICATION_LOGO_ICON"));

        /* add a menubar */
        addMenuBar();

        /* add media panel */
        mMediaPanel = new MediaPanel();
        mainPanel.add(mMediaPanel, BorderLayout.CENTER);

        /* add a systray with menu */
        addSystray();

        mainPanel.add(mStatusBar, BorderLayout.SOUTH);
        getContentPane().add(mainPanel);

        addWindowListener(this);
        validate();
        pack();
    }

    /**
     * Add systray.
     */
    private void addSystray()
    {
        /* check version of java to include or not the systray */
        if(!System.getProperty("java.version").contains("1.6.0") ||
                !SystemTray.isSupported())
        {
            logger.info("Systray not available, upgrade to Java 6!");
            setDefaultCloseOperation(EXIT_ON_CLOSE);
            return;
        }

        if(mSystray == null)
        {
            try
            {
                JPopupMenu menu = new JPopupMenu();
                mCloseItemTray = new JMenuItem(mRes.getString("gui.menu.file.exit"));
                mCloseItemTray.addActionListener(this);
                mPreferencesItemTray = new JMenuItem(mRes.getString("gui.menu.edit.preferences"));
                mPreferencesItemTray.addActionListener(this);
                mHelpItemTray = new JMenuItem(mRes.getString("gui.menu.help"));
                mHelpItemTray.addActionListener(this);
                mAboutItemTray = new JMenuItem(mRes.getString("gui.menu.help.about"));
                mAboutItemTray.addActionListener(this);

                menu.add(mPreferencesItemTray);
                menu.add(mHelpItemTray);
                menu.add(mAboutItemTray);
                menu.add(mCloseItemTray);

                mSystray = new Systray(this, mTitle, mRes.loadImage("APPLICATION_LOGO_SYSTRAY"));
                mSystray.setPopupMenu(menu);
                SystemTray.getSystemTray().add(mSystray);
                setDefaultCloseOperation(HIDE_ON_CLOSE);
            }
            catch(AWTException e)
            {
                logger.severe("Cannot add systray!");
                setDefaultCloseOperation(DISPOSE_ON_CLOSE);
            }
        }
        else
        {
            logger.warning("Cannot instantiate another systray!");
        }
    }

    /**
     * Add menubar.
     */
    private void addMenuBar()
    {
        if(mMenuBar == null)
        {
            JMenu menuFile = new JMenu(mRes.getString("gui.menu.file"));
            JMenu menuEdit = new JMenu(mRes.getString("gui.menu.edit"));
            JMenu menuHelp = new JMenu(mRes.getString("gui.menu.help"));
            mMenuBar = new JMenuBar();

            mOpenItem = new JMenuItem(mRes.getString("gui.menu.file.open"));
            mOpenItem.addActionListener(this);
            mCloseItem = new JMenuItem(mRes.getString("gui.menu.file.exit"));
            mCloseItem.addActionListener(this);
            mPreferencesItem = new JMenuItem(mRes.getString("gui.menu.edit.preferences"));
            mPreferencesItem.addActionListener(this);
            mHelpItem = new JMenuItem(mRes.getString("gui.menu.help"));
            mHelpItem.addActionListener(this);
            mAboutItem = new JMenuItem(mRes.getString("gui.menu.help.about"));
            mAboutItem.addActionListener(this);

            menuFile.add(mOpenItem);
            menuFile.addSeparator();
            menuFile.add(mCloseItem);

            menuEdit.add(mPreferencesItem);

            menuHelp.add(mHelpItem);
            menuHelp.add(mAboutItem);

            mMenuBar.add(menuFile);
            mMenuBar.add(menuEdit);
            mMenuBar.add(menuHelp);

            setJMenuBar(mMenuBar);
        }
        else
        {
            logger.warning("Cannot instantiate another menubar!");
        }
    }

    /**
     * Close frame and release resources.
     */
    public void dispose()
    {
        setVisible(false);

        if(mSystray != null)
        {
            SystemTray.getSystemTray().remove(mSystray);
            mSystray = null;
        }

        mMenuBar = null;
        mStatusBar = null;
        mTitle = null;

        mPreferencesItem = null;
        mHelpItem = null;
        mAboutItem = null;
        mOpenItem = null;
        mCloseItem = null;

        mPreferencesItemTray = null;
        mHelpItemTray = null;
        mAboutItemTray = null;
        mCloseItemTray = null;

        mMediaPanel.dispose();
        mMediaPanel = null;

        if(mConfigurationDialog != null)
        {
            mConfigurationDialog.dispose();
            mConfigurationDialog = null;
        }

        if(mHelpDialog != null)
        {
            mHelpDialog.dispose();
            mHelpDialog = null;
        }

        if(mAboutDialog != null)
        {
            mAboutDialog.dispose();
            mAboutDialog = null;
        }

        super.dispose();
        System.exit(0);
    }

    /**
     * Set text in the task bar.
     * @param text text to set
     */
    private void setStatusBarText(final String text)
    {
        mStatusBar.setText(text);
    }

    /**
     * Open help dialog.
     */
    public void openHelpDialog()
    {
        if(mHelpDialog == null)
        {
            mHelpDialog = new HelpDialog(this);
        }
        mHelpDialog.setVisible(true);
    }

    /**
     * Open about dialog.
     */
    public void openAboutDialog()
    {
        if(mAboutDialog == null)
        {
            mAboutDialog = new AboutDialog(this);
        }
        mAboutDialog.setVisible(true);
    }

    /**
     * Open confiugration dialog.
     */
    public void openConfigurationDialog()
    {
        if(mConfigurationDialog == null)
        {
            mConfigurationDialog = new ConfigurationDialog(this);
        }
        mConfigurationDialog.setVisible(true);
    }

    /**
     * Callback when the window is the active one.
     * @param event window event
     * @see WindowListener#windowActivated(WindowEvent)
     */
    public void windowActivated(WindowEvent event)
    {
    }

    /**
     * Callback when the window has been closed.
     * @param event window event
     * @see WindowListener#windowClosed(WindowEvent)
     */
    public void windowClosed(WindowEvent event)
    {
    }

    /**
     * Callback when the window is being closed.
     * @param event window event
     * @see WindowListener#windowClosing(WindowEvent)
     */
    public void windowClosing(WindowEvent event)
    {
    }

    /**
     * Callback when the window is not the active one.
     * @param event window event
     * @see WindowListener#windowDeactivated(WindowEvent)
     */
    public void windowDeactivated(WindowEvent event)
    {
    }

    /**
     * Callback when the window is deiconified.
     * @param event window event
     * @see WindowListener#windowDeiconified(WindowEvent)
     */
    public void windowDeiconified(WindowEvent event)
    {
    }

    /**
     * Callback when the window is iconified
     * @param event window event
     * @see WindowListener#windowIconified(WindowEvent)
     */
    public void windowIconified(WindowEvent event)
    {
    }

    /**
     * Callback when the window is opened.
     * @param event window event
     * @see WindowListener#windowOpened(WindowEvent)
     */
    public void windowOpened(WindowEvent event)
    {
    }

    /**
     * Callback when user clicked on a button/menu.
     * @param event action event
     */
    public void actionPerformed(ActionEvent event)
    {
        Object src = event.getSource();

        if(src == mOpenItem)
        {
            mMediaPanel.showOpenFile();
        }
        else if(src == mCloseItem || src == mCloseItemTray)
        {
            dispose();
        }
        else if(src == mHelpItem || src == mHelpItemTray)
        {
            openHelpDialog();
        }
        else if(src == mPreferencesItem || src == mPreferencesItemTray)
        {
            openConfigurationDialog();
        }
        else if(src == mAboutItem || src == mAboutItemTray)
        {
            openAboutDialog();
        }
    }
}

