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
