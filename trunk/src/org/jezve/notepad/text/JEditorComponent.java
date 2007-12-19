package org.jezve.notepad.text;

import org.jezve.notepad.text.document.AttributeMap;
import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.format.*;

import java.awt.*;
import java.awt.event.*;
import java.awt.font.*;
import java.awt.geom.*;
import javax.swing.*;

public class JEditorComponent extends JComponent {

    private static final Color STRONG_CARET_COLOR = Color.black;
    private static final Color WEAK_CARET_COLOR = Color.darkGray;
    private final int TOP_MARGIN = 36;

    Behavior behavior;
    Listener listener = new Listener();
    EventBroadcaster broadcaster = new EventBroadcaster();
    SelectionBehavior selection;
    EditBehavior editor;

    MText text;
    FDimension size;
    FPoint origin;
    FontRenderContext frc;
    Formatter format;
    StyledTextClipboard clipboard;

    public JEditorComponent(MText text, AttributeMap style, FDimension bounds) {
        super();
        this.text = text;
        this.size = bounds;
        origin = new FPoint(0, TOP_MARGIN);
        this.frc = new FontRenderContext(null, true, true);
        this.format = new Formatter(text, style, bounds.width, true, frc);

        clipboard = StyledTextClipboard.getClipboardFor(null);

        // if selectable || editable
        selection = new SelectionBehavior(this, broadcaster);
        selection.addToOwner(this);
        // if editable
        editor = new EditBehavior(this, selection, broadcaster);
        editor.addToOwner(this);
    }

    public void addNotify() {
        super.addNotify();
        addFocusListener(listener);
        addKeyListener(listener);
        addMouseListener(listener);
        addMouseMotionListener(listener);
        addComponentListener(listener);
    }

    public void removeNotify() {
        removeComponentListener(listener);
        removeMouseMotionListener(listener);
        removeMouseListener(listener);
        removeKeyListener(listener);
        removeFocusListener(listener);
        super.removeNotify();
    }

    public void paint(Graphics g) {
        Graphics2D g2d = (Graphics2D)g;

        assert RenderingHints.VALUE_TEXT_ANTIALIAS_ON
                .equals(g2d.getRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING));
        assert RenderingHints.VALUE_FRACTIONALMETRICS_ON
                .equals(g2d.getRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS));

        AffineTransform at = g2d.getTransform();

        double scale = getWidth() / size.getWidth();
        g2d.transform(AffineTransform.getScaleInstance(scale, scale));

        FRectangle repaint = new FRectangle(origin, size);
        if (behavior == null || !behavior.paint(g2d, repaint)) format.draw(g2d, repaint, origin);

        g2d.setTransform(at);
    }

    // Interface with behaviors:

    MConstText getText() {
        return text;
    }

    MText getModifiableText() {
        return text;
    }

    StyledTextClipboard getClipboard() {
        return clipboard;
    }

    TextOffset getCaretOffset(TextOffset o, TextOffset initial, TextOffset previous, short dir) {
        return format.findNewInsertionOffset(o, initial, previous, dir);
    }

    int lineContaining(TextOffset offset) {
        return format.lineContaining(offset);
    }

    int lineRangeLimit(int lineNumber) {
        return format.lineRangeLimit(lineNumber);
    }

    int lineRangeLow(int lineNumber) {
        return format.lineRangeLow(lineNumber);
    }

    FRectangle getBoundingRect(TextOffset offset1, TextOffset offset2) {
        return format.getBoundingRect(offset1, offset2, origin, IFormatter.TIGHT);
    }

    private AffineTransform at = null;

    Graphics2D lock() {
        assert at == null : "nested lock()";
        Graphics2D g2d = (Graphics2D)super.getGraphics();
        if (g2d == null) return null;
        at = g2d.getTransform();
        double scale = (double)getWidth() / size.getWidth();
        g2d.transform(AffineTransform.getScaleInstance(scale, scale));
        return g2d;
    }

    void unlock(Graphics2D g2d) {
        assert at != null : "no saved transform in unlock()";
        if (g2d != null) g2d.setTransform(at);
        at = null;
    }

    void reformatAndDrawText(int cp, int cch, TextOffset ss, TextOffset se, FRectangle dirty,
            Color color) {

        FRectangle visibleBounds = new FRectangle(origin, size);
        FRectangle redrawRect = format.updateFormat(cp, cch, visibleBounds, origin);

        if (dirty != null) redrawRect.add(dirty);

        boolean haveSelection = (ss != null && se != null);
        if (haveSelection) redrawRect.add(format.getBoundingRect(ss, se, origin, false));

        Graphics2D g2d = lock();
        drawText(g2d, redrawRect, haveSelection, ss, se, color);
        unlock(g2d);
    }

    void drawText(Graphics2D g, FRectangle drawRect, boolean selectionVisible, TextOffset selStart,
            TextOffset selEnd, Color hiliteColor) {
        if (g != null) {
            if (isOpaque()) {
                Color oldColor = g.getColor();
                g.setColor(getBackground());
                g.fill(drawRect);
                g.setColor(oldColor);
            }
            g.clip(drawRect);
            if (selectionVisible) {
                format.draw(g, drawRect, origin, selStart, selEnd, hiliteColor);
            }
            else {
                format.draw(g, drawRect, origin, null, null, null);
            }
            if (selStart != null && selStart.equals(selEnd) && selectionVisible) {
                format.drawCaret(g, selStart, origin, STRONG_CARET_COLOR, WEAK_CARET_COLOR);
            }
        }
//        textSizeMightHaveChanged();
    }

//    FRectangle textBounds = new FRectangle();
//
//    private void calcBoundsRect() {
//        float minX = format.minX();
//        float minY = format.minY();
//        textBounds.setBounds(origin.x + minX, origin.y + minY, format.maxX() - minX, format.maxY() - minY);
//    }
//
//    private void textSizeMightHaveChanged() {
//        boolean changed = false;
//        float textHeight = format.maxY() - format.minY();
//
//        if (textHeight != textBounds.height) {
//            textBounds.height = textHeight;
//            changed = true;
//        }
//        if (!format.wrap()) {
//            float textWidth = format.maxX() - format.minX();
//            if (textWidth != textBounds.width) {
//                textBounds.width = textWidth;
//                changed = true;
//            }
//        }
//        if (changed) {
//            calcBoundsRect();
//        }
//    }

    TextOffset pointToTextOffset(TextOffset result, float x, float y, TextOffset anchor,
            boolean infiniteMode) {
        return format.pointToTextOffset(result, x, y, origin, anchor, infiniteMode);
    }

    void scrollToShow(FRectangle sel) {
    }

    void scrollToShow(float showX, float showY) {
//        float dx = 0, dy = 0;
//
//        Rectangle bounds = getBounds();
//        if (showX < bounds.x) {
//            dx = showX - bounds.x;
//        }
//        else if (showX > bounds.x + bounds.width) {
//            dx = showX - (bounds.x + bounds.width);
//        }
//
//        if (showY < bounds.y) {
//            dy = showY - bounds.y;
//        }
//        else if (showY > bounds.y + bounds.height) {
//            dy = showY - (bounds.y + bounds.height);
//        }
//        scrollSelf(dx, dy);
    }

    FRectangle getCaretRect(TextOffset offset) {
        return format.getCaretRect(offset, origin);
    }

    class Listener extends ComponentAdapter
            implements FocusListener, KeyListener, MouseListener, MouseMotionListener {

        public void componentResized(ComponentEvent e) {
        }

        public void focusGained(FocusEvent e) {
            behavior.focusGained(e);
        }

        public void focusLost(FocusEvent e) {
            behavior.focusLost(e);
        }

        public void keyTyped(KeyEvent e) {
            behavior.keyTyped(e);
        }

        public void keyPressed(KeyEvent e) {
            behavior.keyPressed(e);
        }

        public void keyReleased(KeyEvent e) {
            behavior.keyReleased(e);
        }

        public void mouseClicked(MouseEvent e) {
        }

        public void mousePressed(MouseEvent e) {
            behavior.mousePressed(F(e));
        }

        public void mouseReleased(MouseEvent e) {
            behavior.mouseReleased(F(e));
        }

        public void mouseEntered(MouseEvent e) {
            behavior.mouseEntered(F(e));
        }

        public void mouseExited(MouseEvent e) {
            behavior.mouseExited(F(e));
        }

        public void mouseDragged(MouseEvent e) {
            behavior.mouseDragged(F(e));
        }

        public void mouseMoved(MouseEvent e) {
            behavior.mouseMoved(F(e));
        }
    }

    FMouseEvent F(MouseEvent e) {
        float x = e.getX() * size.width / getWidth();
        float y = e.getY() * size.height / getHeight();
        return new FMouseEvent(e, new FPoint(x, y));
    }

    public Behavior getBehavior() {
        return behavior;
    }

    public void setBehavior(Behavior b) {
        behavior = b;
    }

    static Rectangle r2r(FRectangle fr) {
        int x = (int)Math.floor(fr.x);
        int w = (int)Math.ceil(fr.x + fr.width) - x;
        int y = (int)Math.floor(fr.y);
        int h = (int)Math.ceil(fr.y + fr.height) - y;
        return new Rectangle(x, y, w, h);
    }

}
