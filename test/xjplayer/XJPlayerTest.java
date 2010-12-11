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
 * Unit test for XJPlayer.
 *
 * @author Sebastien Vincent
 */
public class XJPlayerTest extends TestCase
{
    /**
     * Unit test Constructor.
     * @param name name of the testcase
     */
    public XJPlayerTest(String name)
    {
        super(name);
    }

    /**
     * Method executed prior to run tests.
     */
    @Override
    protected void setUp()
    {
    }

    /**
     * Method executed at the end of tests.
     */
    @Override
    protected void tearDown()
    {
    }

    /**
     * Test.
     */
    public void testInitialization()
    {
        assertTrue("Test", true);
    }
}

