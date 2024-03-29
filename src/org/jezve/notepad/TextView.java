/**
* Copyright (c) 2007-2008, jezve.org and its Contributors
* All rights reserved.
*
* Redistribution and use in source and binary forms, with or without
* modification, are permitted provided that the following conditions are met:
*     * Redistributions of source code must retain the above copyright
*       notice, this list of conditions and the following disclaimer.
*     * Redistributions in binary form must reproduce the above copyright
*       notice, this list of conditions and the following disclaimer in the
*       documentation and/or other materials provided with the distribution.
*     * Neither the name of the jezve.org nor the
*       names of its contributors may be used to endorse or promote products
*       derived from this software without specific prior written permission.
*
* THIS SOFTWARE IS PROVIDED BY jezve.org AND SOFTWARE CONTRIBUTORS ``AS IS'' AND ANY
* EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
* WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
* DISCLAIMED. IN NO EVENT SHALL jezve.org or CONTRIBUTORS BE LIABLE FOR ANY
* DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
* (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
* LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
* ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
* (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
* SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
*/

package org.jezve.notepad;

import org.jezve.notepad.text.JEditorComponent;
import org.jezve.notepad.text.document.*;
import org.jezve.notepad.text.format.FDimension;

import java.awt.*;
import java.awt.event.*;
import java.awt.geom.*;
import java.util.*;
import javax.swing.*;

class TextView extends JComponent {

    FDimension page_size = new FDimension(612, 792);

    Dimension size = new Dimension();
    Point offset = new Point();

    double zoom;
    double screen_dpi;

    MText text;
    JEditorComponent editor;

    TextView() {
        zoom = Notepad.user.getDouble("view.scale", 1);

        Map attrs = new HashMap();
        attrs.put(TextAttribute.FAMILY, "Times New Roman");
        attrs.put(TextAttribute.SIZE, new Float(20));
        attrs.put(TextAttribute.WEIGHT, TextAttribute.WEIGHT_REGULAR);
        attrs.put(TextAttribute.POSTURE, TextAttribute.POSTURE_REGULAR);

        attrs.put(TextAttribute.LEADING_MARGIN, new Float(36));
        attrs.put(TextAttribute.TRAILING_MARGIN, new Float(36));
        attrs.put(TextAttribute.FIRST_LINE_INDENT, new Float(72));
        attrs.put(TextAttribute.MIN_LINE_SPACING, new Float(20));
        attrs.put(TextAttribute.EXTRA_LINE_SPACING, new Float(0));
        attrs.put(TextAttribute.LINE_FLUSH, TextAttribute.FULLY_JUSTIFIED);
        attrs.put(TextAttribute.RUN_DIRECTION, TextAttribute.RUN_DIRECTION_LTR);
        attrs.put(TextAttribute.TAB_RULER, new TabRuler(36));

        AttributeMap style = new AttributeMap(attrs);
        text = new StyledText("The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog. " +
                "The quick brown fox jumps over the lazy dog.", style);

        screen_dpi = 72; //Toolkit.getDefaultToolkit().getScreenResolution();
        if (screen_dpi < 72) screen_dpi = 72;

        editor = new JEditorComponent(text, style, page_size);
        editor.setOpaque(true);
        editor.setBackground(Color.white);
        editor.setVisible(true);
        add(editor);
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;
        g2d.setColor(new Color(144, 153, 174));
        g2d.fill(g2d.getClipBounds());

        AffineTransform at = g2d.getTransform();
        AffineTransform ti = AffineTransform.getTranslateInstance(offset.x, offset.y);
        g2d.transform(ti);

        g2d.setColor(Color.WHITE);
        g2d.fillRect(0, 0, size.width, size.height);
        g2d.setColor(Color.BLACK);
        g2d.drawRect(-1, -1, size.width + 1, size.height + 1);
        g2d.fillRect(size.width + 1, 1, 2, size.height + 2);
        g2d.fillRect(1, size.height + 1, size.width, 2);

        g2d.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING,
                RenderingHints.VALUE_TEXT_ANTIALIAS_ON);
        g2d.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS,
                RenderingHints.VALUE_FRACTIONALMETRICS_ON);

        g2d.setTransform(at);
        super.paint(g);
    }

    public Dimension getPreferredSize() {
        return new Dimension(size.width + 14, size.height + 14);
    }

    double scale() {
        return zoom * screen_dpi / 72;
    }

    double getZoom() {
        return zoom;
    }

    void setZoom(double z) {
        zoom = z;
        adjustEditorPosition();
        Notepad.user.putDouble("view.scale", zoom);
        updateTitle(zoom);
    }

    void updateTitle(double z) {
        float f = Math.round(z * 100) / 100f;
        if (Notepad.frame != null) Notepad.frame.updateTitle("" + f);
    }

    void adjustEditorPosition() {
        Rectangle bounds = getBounds();

        size.width = (int)Math.ceil(page_size.width * scale());
        size.height = (int)Math.ceil(page_size.height * scale());

        offset.x = (bounds.width - size.width - 14) / 2 + 6;
        if (offset.x < 6) offset.x = 6;
        offset.y = (bounds.height - size.height - 14) / 2 + 6;
        if (offset.y < 6) offset.y = 6;

        editor.setBounds(offset.x, offset.y, size.width, size.height);
    }

    public void addNotify() {
        super.addNotify();
        updateTitle(zoom);
        addComponentListener(new ComponentAdapter() {
            public void componentResized(ComponentEvent e) {
                adjustEditorPosition();
            }
        });
        editor.requestFocus();
    }
}
