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

import org.jezve.notepad.text.document.MText;
import org.jezve.notepad.text.format.TextOffset;

abstract class TextCommand extends Command {

    protected EditBehavior fBehavior;
    protected MText text;
    protected int fAffectedRangeStart;
    protected TextOffset fSelStartBefore;
    protected TextOffset fSelEndBefore;

    public TextCommand(EditBehavior behavior, MText originalText, int affectedRangeStart,
            TextOffset selStartBefore, TextOffset selEndBefore) {

        fBehavior = behavior;
        text = originalText;
        fAffectedRangeStart = affectedRangeStart;
        fSelStartBefore = new TextOffset();
        fSelStartBefore.assign(selStartBefore);
        fSelEndBefore = new TextOffset();
        fSelEndBefore.assign(selEndBefore);
    }

    public abstract int affectedRangeEnd();

    public void undo() {
        fBehavior.doReplaceText(fAffectedRangeStart, affectedRangeEnd(), text, fSelStartBefore,
                fSelEndBefore);
    }
}
