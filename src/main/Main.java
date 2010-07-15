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

package main;

import java.util.logging.*;

import javax.swing.*;

import util.*;
import gui.*;

/**
 * Application launcher.
 *
 * @author Sebastien Vincent
 */
public final class Main
{
    /**
     * The logger.
     */
    private static final Logger logger = Logger.getLogger(Main.class.getName());

    /**
     * Program name.
     */
    public static final String PROGRAM_NAME = "XJPlayer";

    /**
     * Program version.
     */
    public static final String PROGRAM_VERSION = "0.1";

    /**
     * EXIT_SUCCESS.
     */
    public static final int EXIT_SUCCESS = 0;

    /**
     * EXIT_FAILURE.
     */
    public static final int EXIT_FAILURE = 1;

    /**
     * Entry point of the program.
     * @param argv array containing command line parameter
     */
    public static void main(String argv[])
    {
        logger.info("Welcome to " + PROGRAM_NAME + " " + PROGRAM_VERSION);
        logger.info("Running on " + OSUtils.getOSName() + " " + OSUtils.getOSVersion() + 
                " (" + OSUtils.getOSArch() + "-bit - " + OSUtils.getOSArchName() + ")");

        /* register a cleanup exit point */
        Runtime.getRuntime().addShutdownHook(new CleanupThread());

        /* create the GUI in the Event Dispatch Thread (EDT) */
        SwingUtilities.invokeLater(new GUIBuilderThread());
    }

    /**
     * GUI Builder thread.
     *
     * Call it in the main() function to build the GUI
     * in the Event Dispatch Thread:
     * SwingUtilities.invokeLater(new GUIBuilderThread());
     *
     * @author Sebastien Vincent
     */
    private static final class GUIBuilderThread extends Thread
    {
        /**
         * The logger.
         */
        private static final Logger logger = 
            Logger.getLogger(GUIBuilderThread.class.getName());

        /**
         * Constructor.
         */
        GUIBuilderThread()
        {
            setName("GUIBuilderThread");
        }

        /**
         * Thread entry point.
         */
        public void run()
        {
            logger.info("Create GUI");

            /* configure look and feel */
            try
            {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            }
            catch(Exception e)
            {
                logger.warning("Could not set system look and feel");
            }

            MainFrame mainFrame = new MainFrame(PROGRAM_NAME + " " + PROGRAM_VERSION);
            mainFrame.setVisible(true);
            mainFrame = null;
        }
    }

    /**
     * Cleanup thread.
     *
     * This is called when the program exit either normally
     * or via signal reception (SIGTERM, ...).
     *
     * @author Sebastien Vincent
     */
    private static final class CleanupThread extends Thread
    {
        /**
         * The logger.
         */
        private static final Logger logger = 
            Logger.getLogger(CleanupThread.class.getName());

        /**
         * Constructor.
         */
        CleanupThread()
        {
            setName("CleanupThread");
        }

        /**
         * Thread entry point.
         */
        public void run()
        {
            logger.info("Exiting...");

            /* run the garbage collector */
            System.gc();
        }
    } 
}

