package org.jezve.notepad.text;

import org.jezve.notepad.text.document.MConstText;

import java.awt.datatransfer.*;
import java.io.IOException;

/**
 * This class allows MConstText instances to be the contents
 * of a Clipboard.  To store an MConstText on the clipboard,
 * construct a TransferableText from the MConstText, and make
 * the TransferableText the clipboard contents.
 */
/*
 * Note:  this class inherits from StringSelection because of
 * a bug in the 1.1.7 system clipboard implementation.  The
 * system clipboard won't put text on the OS clipboard unless
 * the content is a StringSelection.
 */
public final class TransferableText extends StringSelection {

    private MConstText fText;

    private static String textToString(MConstText text) {
        char[] chars = new char[text.length()];
        text.extractChars(0, chars.length, chars, 0);
        return new String(chars);
    }

    /**
     * Create a TransferableText for the given text.
     *
     * @param text the text to go on the Clipboard.  The text is adopted by this object.
     */
    public TransferableText(MConstText text) {
        super(textToString(text));
        fText = text;
    }

    public DataFlavor[] getTransferDataFlavors() {

        DataFlavor[] flavors = super.getTransferDataFlavors();
        DataFlavor[] result = new DataFlavor[flavors.length + 1];
        result[0] = MConstText.styledTextFlavor;
        System.arraycopy(flavors, 0, result, 1, flavors.length);
        return result;
    }

    public boolean isDataFlavorSupported(DataFlavor flavor) {
        return flavor.equals(MConstText.styledTextFlavor) || super.isDataFlavorSupported(flavor);
    }

    public Object getTransferData(DataFlavor flavor)
            throws UnsupportedFlavorException, IOException {
        if (flavor.equals(MConstText.styledTextFlavor)) {
            return fText;
        }
        else {
            return super.getTransferData(flavor);
        }
    }
}
