package org.jezve.notepad.text;

public abstract class Command {

    private Command fPreviousCommand = null;

    // fModified is used to keep a textModified flag for clients
    private boolean fModified;

    public Command() {
        fModified = true;
    }

    public Command previousCommand() {
        return fPreviousCommand;
    }

    public void setPreviousCommand(Command previousCommand) {
        fPreviousCommand = previousCommand;
    }

    public abstract void execute();

    public abstract void undo();

    public void redo() {
        execute();
    }

    public final boolean isModified() {
        return fModified;
    }

    public final void setModified(boolean modified) {
        fModified = modified;
    }
}
