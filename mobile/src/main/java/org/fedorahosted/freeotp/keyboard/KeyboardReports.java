package org.fedorahosted.freeotp.keyboard;

import java.util.Arrays;

/** HID control report sizes must agree with the keyboard's report descriptor. */
public final class KeyboardReports {
    private KeyboardReports() {}
    public static byte[] get(int type, int id, int maxSize, byte[] input, byte leds) {
        if(id!=0) throw new IllegalArgumentException("Unknown report ID");
        byte[] data;
        if(type==1) data=input.clone();
        else if(type==2) data=new byte[]{leds};
        else throw new IllegalArgumentException("Unsupported report type");
        return maxSize>0 && maxSize<data.length?Arrays.copyOf(data,maxSize):data;
    }
}
