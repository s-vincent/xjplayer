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

package xjplayer.util;

/**
 * OS detection class.
 *
 * @author Sebastien Vincent
 */
public class OSUtils
{
    /**
     * OS name.
     */
    private static final String mOSName = System.getProperty("os.name");

    /**
     * Name of the architecture (x86, x86_64, ...).
     */
    private static final String mOSArchName = System.getProperty("os.arch");

    /**
     * Architecture of the JVM (32 or 64).
     */
    private static final String mOSArch;

    /**
     * Version of OS.
     */
    private static final String mOSVersion = System.getProperty("os.version");

    /**
     * If the running OS is Linux.
     */
    private static final boolean mIsLinux = mOSName.startsWith("Linux");

    /**
     * If the running OS is Mac OS X.
     */
    private static final boolean mIsMac = mOSName.startsWith("Mac");

    /**
     * If the running OS is Microsoft Windows.
     */
    private static final boolean mIsWindows = mOSName.startsWith("Windows");

    /**
     * If the running OS is FreeBSD.
     */
    private static final boolean mIsFreeBSD = mOSName.startsWith("FreeBSD");

    static
    {
        /* determine architecture (32 or 64-bit) */
        String arch = System.getProperty("sun.arch.data.model");

        /* as this property is SUN proprietary ones, some JVM could
         * not implement it, propose a workaround if this property
         * is missing
         */
        if(arch != null)
        {
            mOSArch = arch;
        }
        else
        {
            /* OK try to guess it */
            mOSArch = (mOSArchName.contains("64") ||
                    mOSArchName.startsWith("alpha")) ? "64" : "32";
        }
    }
    /**
     * Get OS name.
     * @return OS name
     */
    public static final String getOSName()
    {
        return mOSName;
    }

    /**
     * Get OS version.
     * @return OS version
     */
    public static final String getOSVersion()
    {
        return mOSVersion;
    }

    /**
     * Get OS architecture.
     * @return architecture (32 or 64)
     */
    public static final String getOSArch()
    {
        return mOSArch;
    }

    /**
     * Get OS architecture name.
     * @return architecture name
     */
    public static final String getOSArchName()
    {
        return mOSArchName;
    }

    /**
     * Returns if the running OS is Linux.
     * @return true if OS is Linux, false otherwise
     */
    public static final boolean isLinux()
    {
        return mIsLinux;
    }
    /**
     * Returns if the running OS is MacOS X.
     * @return true if OS is MacOS X, false otherwise
     */
    public static final boolean isMac()
    {
        return mIsMac;
    }

    /**
     * Returns if the running OS is Windows.
     * @return true if OS is Windows, false otherwise
     */
    public static final boolean isWindows()
    {
        return mIsWindows;
    }

    /**
     * Returns if the running OS is FreeBSD.
     * @return true if OS is FreeBSD, false otherwise
     */
    public static final boolean isFreeBSD()
    {
        return mIsFreeBSD;
    }
}
