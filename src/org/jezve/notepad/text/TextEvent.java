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

public class TextEvent {

    // This event's WHAT parameter is a TextRange instance
    static final TextEvent SELECT = new TextEvent();

    // No WHAT param for these:
    static final TextEvent CUT = new TextEvent();
    static final TextEvent COPY = new TextEvent();
    static final TextEvent PASTE = new TextEvent();
    static final TextEvent CLEAR = new TextEvent();
    static final TextEvent UNDO = new TextEvent();
    static final TextEvent REDO = new TextEvent();
    static final TextEvent CLEAR_COMMAND_LOG = new TextEvent();

    // WHAT param is a StyleModifier
    static final TextEvent CHARACTER_STYLE_MOD = new TextEvent();
    static final TextEvent PARAGRAPH_STYLE_MOD = new TextEvent();

    // With this event, values of the WHAT parameter are
    // either Boolean.TRUE or Boolean.FALSE
    static final TextEvent SET_MODIFIED = new TextEvent();

    // WHAT param is a TextReplacement
    static final TextEvent REPLACE = new TextEvent();

    // WHAT param is an Integer
    static final TextEvent SET_COMMAND_LOG_SIZE = new TextEvent();


    public static final int SELECTION_RANGE_CHANGED = 11;

    /**
     * Events of this type are sent when the selection range becomes
     * 0-length after not being 0-length, or vice versa.  This event
     * is a special case of SELECTION_RANGE_CHANGED.
     */
    public static final int SELECTION_EMPTY_CHANGED = 12;

    /**
     * Events of this type indicate that the text in the TextPanel changed.
     * This type of event occurs often.
     */
    public static final int TEXT_CHANGED = 13;

    /**
     * Events of this type are sent when the styles in the current
     * selection change.
     */
    public static final int SELECTION_STYLES_CHANGED = 14;

    /**
     * Events of this type are sent when the undo/redo state changes.
     */
    public static final int UNDO_STATE_CHANGED = 15;

    /**
     * Events of this type are sent when the clipboard state changes.
     */
    public static final int CLIPBOARD_CHANGED = 16;

    /**
     * Events of this type are sent when
     * the wrap width of the text changes.
     */
    public static final int FORMAT_WIDTH_CHANGED = 17;

    /**
     * Events of this type are sent when the key remap changes.
     */
    public static final int KEYREMAP_CHANGED = 18;
}
