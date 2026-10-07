package org.fedorahosted.freeotp.main.share;

/** Chooses a transport without performing any send or discovery. */
public enum ShareRoute {
    NONE, CLIPBOARD, JELLING, KEYBOARD, CHOOSER;
    public static ShareRoute choose(boolean clipboard,boolean jelling,boolean keyboard) {
        int count=(clipboard?1:0)+(jelling?1:0)+(keyboard?1:0);
        if(count==0) return NONE;
        if(count>1) return CHOOSER;
        return clipboard?CLIPBOARD:jelling?JELLING:KEYBOARD;
    }
}
