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

package xjplayer.gui.dialog;

import java.awt.*;
import java.awt.event.*;

import javax.swing.*;

import xjplayer.resources.*;

/**
 * GUI help dialog.
 *
 * @author Sebastien Vincent
 */
public class HelpDialog extends JDialog implements ActionListener
{
    /**
     * Serial version UID.
     */
    private static final long serialVersionUID = 1L;

    /**
     * Resource manager instance.
     */
    private static final ResourceManager mRes = ResourceManager.getInstance();

    /**
     * Close button.
     */
    private JButton mBtnClose = null;

    /**
     * Parent frame reference.
     */
    protected JFrame mParent = null;

    /**
     * Constructor.
     * @param parent parent frame
     */
    public HelpDialog(JFrame parent)
    {
        super(parent, mRes.getString("gui.dialog.help"), false);

        mParent = parent;
        buildDialog();
        setMinimumSize(new Dimension(300, 300));
        pack();
    }

    /**
     * Build the dialog component.
     */
    private void buildDialog()
    {
        JPanel panelMain = new JPanel(new BorderLayout());
        JPanel panelBtn = new JPanel(new FlowLayout(FlowLayout.RIGHT));

        /* close button */
        mBtnClose = new JButton(mRes.getString("gui.btn.close"));
        mBtnClose.setSize(mBtnClose.getMinimumSize());
        mBtnClose.addActionListener(this);
        panelBtn.add(mBtnClose);
        panelMain.add(panelBtn, BorderLayout.SOUTH);

        this.getContentPane().add(panelMain);
    }

    /**
     * Callback when user do an action (button click, ...).
     * @param event action event
     */
    public void actionPerformed(ActionEvent event)
    {
        Object obj = event.getSource();

        if(obj == mBtnClose)
        {
            setVisible(false);
        }
    }
}

