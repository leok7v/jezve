package org.jezve.notepad.text;

import org.jezve.notepad.text.document.AttributeMap;
import org.jezve.notepad.text.document.MConstText;
import org.jezve.notepad.text.document.StyledText;

import java.awt.*;
import java.awt.datatransfer.*;
import java.io.IOException;
import java.io.InputStream;

/**
 * Wrapper for java.awt.datatransfer.Clipboard
 * Packages an MConstText in a transferable, and puts it on the clipboard.
 */
class StyledTextClipboard implements ClipboardOwner {

    // This class has a workaround for a bug in the Windows system clipboard.
    // The system clipboard will only return String content, even
    // though it has a reference to the contents.  So if our
    // clipboard is the system clipboard, we'll keep a reference
    // to the content and use that instead of what the Clipboard returns.

    private static Clipboard SYSTEM = null;

    static {
        try {
            SYSTEM = Toolkit.getDefaultToolkit().getSystemClipboard();
        }
        catch (Throwable th) {
        }
    }

    private static StyledTextClipboard fgSystemClipboard = null;

    public static StyledTextClipboard getClipboardFor(Clipboard clipboard) {

        if (clipboard == null && SYSTEM != null) {
            synchronized (SYSTEM) {
                if (fgSystemClipboard == null) {
                    fgSystemClipboard = new StyledTextClipboard(SYSTEM, true);
                }
            }
            return fgSystemClipboard;
        }
        else {
            return new StyledTextClipboard(clipboard, false);
        }
    }

    private Clipboard fClipboard;
    private boolean fUseLocalContents;
    private Transferable fContents = null;

    private StyledTextClipboard(Clipboard clipboard, boolean useLocalContents) {
        if (clipboard == null) {
            fClipboard = new Clipboard("TextPanel clipboard");
        }
        else {
            fClipboard = clipboard;
        }
        fUseLocalContents = useLocalContents;
    }

    public void lostOwnership(Clipboard clipboard, Transferable contents) {
        if (contents == fContents) {
            this.fContents = null;
        }
    }

    public void setContents(MConstText newContents) {

        TransferableText contents = new TransferableText(newContents);
        if (fClipboard == SYSTEM) {
            fContents = contents;
        }
        fClipboard.setContents(contents, this);
    }

    private Transferable getClipboardContents() {
        if (fUseLocalContents && fContents != null) {
            return fContents;
        }
        return fClipboard.getContents(this);
    }

    // Has contents - faster than getContents for finding out whether the clipboard has text.
    public boolean hasContents() {
        Transferable contents = getClipboardContents();
        return contents != null && (contents.isDataFlavorSupported(MConstText.styledTextFlavor) ||
                contents.isDataFlavorSupported(DataFlavor.stringFlavor) ||
                contents.isDataFlavorSupported(DataFlavor.plainTextFlavor));
    }

    private String getString(InputStream inStream) throws IOException {
        String value = new String();
        int bytesRead;
        do {
            byte inBytes[] = new byte[inStream.available()];
            bytesRead = inStream.read(inBytes);
            if (bytesRead != -1) {
                value = value + new String(inBytes);
            }
        }
        while (bytesRead != -1);
        return value;
    }

    /*
     * If the Clipboard has text content, return it as an
     * MConstText.  Otherwise return null.
     *
     * @param defaultStyle the style to apply to unstyled
     *                     text (such as a String).  If the clipboard
     *                     has styled text this parameter is not used.
     */
    public MConstText getContents(AttributeMap defaultStyle) {
        Transferable contents = getClipboardContents();
        if (contents == null) {
            return null;
        }
        DataFlavor flavors[] = contents.getTransferDataFlavors();
        // search flavors for our flavor, String flavor and raw text flavor
        Exception ex = null;
        try {
            int i;
            for (i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(MConstText.styledTextFlavor)) {
                    break;
                }
            }
            if (i < flavors.length) {
                Object data = contents.getTransferData(MConstText.styledTextFlavor);
                if (data == null) {
                    System.out.println("Data is null.");
                }
                return (MConstText)data;
            }
            for (i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(DataFlavor.stringFlavor)) {
                    break;
                }
            }
            if (i < flavors.length) {
                Object data = contents.getTransferData(DataFlavor.stringFlavor);
                return new StyledText((String)data, defaultStyle);
            }
            for (i = 0; i < flavors.length; i++) {
                if (flavors[i].equals(DataFlavor.plainTextFlavor)) {
                    break;
                }
            }
            if (i < flavors.length) {
                Object data = contents.getTransferData(DataFlavor.plainTextFlavor);
                String textString = getString((InputStream)data);
                return new StyledText(textString, defaultStyle);
            }
        }
        catch (UnsupportedFlavorException e) {
            ex = e;
        }
        catch (IOException e) {
            ex = e;
        }
        System.out.println("Exception when retrieving data.  Exception:" + ex);
        return null;
    }
}
