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
import org.jezve.notepad.text.format.TextOffset;

/**
 * This class is used to pass a REPLACE command to Behaviors.
 */
public final class TextReplacement {

    private int fStart;
    private int fLimit;
    private MConstText fText;
    private TextOffset fSelStart;
    private TextOffset fSelLimit;

    public TextReplacement(int start, int limit, MConstText text, TextOffset selStart,
            TextOffset selLimit) {

        fStart = start;
        fLimit = limit;
        fText = text;
        fSelStart = selStart;
        fSelLimit = selLimit;
    }

    public int getStart() {
        return fStart;
    }

    public int getLimit() {
        return fLimit;
    }

    public MConstText getText() {
        return fText;
    }

    public TextOffset getSelectionStart() {
        return fSelStart;
    }

    public TextOffset getSelectionLimit() {
        return fSelLimit;
    }
}
