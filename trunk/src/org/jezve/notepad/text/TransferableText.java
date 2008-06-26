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
