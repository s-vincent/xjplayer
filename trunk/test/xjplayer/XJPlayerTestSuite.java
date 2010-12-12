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

package xjplayer;

import junit.framework.*;

/**
 * Unit test suite.
 *
 * @author Sebastien Vincent
 */
public class XJPlayerTestSuite
{
    /**
     * Constructor.
     */
    public XJPlayerTestSuite()
    {
    }

    /**
     * Suite of unit tests.
     * @return the TestSuite containing all unit tests
     */
    public static Test suite()
    {
        TestSuite suite = new TestSuite();
        suite.addTestSuite(XJPlayerTest.class);
        return suite;
    }
}
