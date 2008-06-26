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

final class SimpleCommandLog {

    private Command fLastCommand = null;
    private Command fCurrentCommand = null;
    private EventBroadcaster fListener;
    private boolean fBaseIsModified;
    private int fLogSize = 14;

    public SimpleCommandLog(EventBroadcaster listener) {
        fListener = listener;
        fBaseIsModified = false;
    }

    /*
     * adds the specfied command to the top of the command stack
     * (any undone commands on the stack are removed)
     * This function assumes the command has already been executed (i.e., its execute() method
     * has been called, or an equivalent action has been taken)
     */
    void add(Command newCommand) {
        // if there are commands on the stack that have been undone, they are
        // dropped on the floor here
        newCommand.setPreviousCommand(fCurrentCommand);
        final Command oldLastCommand = fLastCommand;
        fLastCommand = null;
        fCurrentCommand = newCommand;
        limitCommands(fLogSize);
        if (oldLastCommand != null) {
            fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
        }
    }

    /*
     * If the command list is longer than logSize, truncate it.
     * This method traverses the list each time, and is not a model
     * of efficiency.  It's a temporary way to plug this memory leak
     * until I can implement a bounded command log.
     */
    private void limitCommands(int logSize) {
        if (logSize == 0) {
            fCurrentCommand = null;
        }
        else {
            Command currentCommand = fCurrentCommand;
            int remaining = logSize - 1;
            while (currentCommand != null && remaining > 0) {
                currentCommand = currentCommand.previousCommand();
                remaining -= 1;
            }
            if (currentCommand != null) {
                currentCommand.setPreviousCommand(null);
            }
        }
    }

    // adds the specfied command to the top of the command stack and executes it
    void addAndDo(Command newCommand) {
        add(newCommand);
        newCommand.execute();
        fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
    }

    /**
     * undoes the command on the top of the command stack, if there is one
     */
    void undo() {
        if (fCurrentCommand != null) {
            Command current = fCurrentCommand;
            current.undo();
            fCurrentCommand = current.previousCommand();
            current.setPreviousCommand(fLastCommand);
            fLastCommand = current;
            fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
        }
    }

    /**
     * redoes the last undone command on the command stack, if there are any
     */
    void redo() {
        if (fLastCommand != null) {
            Command last = fLastCommand;
            last.redo();
            fLastCommand = last.previousCommand();
            last.setPreviousCommand(fCurrentCommand);
            fCurrentCommand = last;
            fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
        }
    }

    public boolean canUndo() {
        return fCurrentCommand != null;
    }

    public boolean canRedo() {
        return fLastCommand != null;
    }

    public boolean isModified() {
        if (fCurrentCommand == null) {
            return fBaseIsModified;
        }
        else {
            return fCurrentCommand.isModified();
        }
    }

    public void setModified(boolean modified) {
        if (fCurrentCommand == null) {
            fBaseIsModified = modified;
        }
        else {
            fCurrentCommand.setModified(modified);
        }
    }

    public void clearLog() {
        if (fCurrentCommand != null) {
            fBaseIsModified = fCurrentCommand.isModified();
        }
        // variable not used boolean changed = fCurrentCommand != null || fLastCommand != null;
        fCurrentCommand = null;
        fLastCommand = null;
        fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
    }

    public void setLogSize(int size) {
        if (size < 0) {
            throw new IllegalArgumentException("log size cannot be negative");
        }
        if (size < fLogSize) {
            limitCommands(size);
        }
        fLogSize = size;
        if (fLastCommand != null || size == 0) {
            fLastCommand = null;
            fListener.textStateChanged(TextEvent.UNDO_STATE_CHANGED);
        }
    }

    public int getLogSize() {
        return fLogSize;
    }
}
