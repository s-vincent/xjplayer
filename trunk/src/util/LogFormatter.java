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

package util;

import java.text.*;
import java.util.*;
import java.util.logging.*;

/**
 * Definition for log format output.
 *
 * @author Sebastien Vincent
 */
public final class LogFormatter extends java.util.logging.Formatter
{
    /**
     * New line character string value.
     */
    private static final String mNewLine = System.getProperty("line.separator");

    /**
     * To print decimal value with two number (i.e. 07).
     */
    private static final DecimalFormat mTwoNumber = new DecimalFormat("00");

    /**
     * To print decimal value with three number (i.e. 007).
     */
    private static final DecimalFormat mThreeNumber = new DecimalFormat("000");

    /**
     * Get the formatted string.
     * Format is hour:min:sec.msec (package.ClassName) LEVEL: message.
     * @param record logging request
     * @return formatted string
     */
    public final String format(LogRecord record)
    {
        StringBuffer buf = new StringBuffer();
        Calendar calendar = Calendar.getInstance();
        int hour = 0;
        int min = 0;
        int sec = 0;
        int msec = 0;

        /* get time elements */
        hour = calendar.get(Calendar.HOUR_OF_DAY);
        min = calendar.get(Calendar.MINUTE);
        sec = calendar.get(Calendar.SECOND);
        msec = calendar.get(Calendar.MILLISECOND);

        /* time */
        buf.append(mTwoNumber.format(hour));
        buf.append(":");
        buf.append(mTwoNumber.format(min));
        buf.append(":");
        buf.append(mTwoNumber.format(sec));
        buf.append(".");
        buf.append(mThreeNumber.format(msec));
        buf.append(" ");

        /* source class name */
        buf.append("(");
        buf.append(record.getSourceClassName());
        buf.append(") ");

        /* level */
        buf.append(record.getLevel().getName());
        buf.append(": ");

        /* finally the message */
        buf.append(record.getMessage());
        buf.append(mNewLine);

        return buf.toString();
    }
}

