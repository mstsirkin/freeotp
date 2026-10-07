package org.fedorahosted.freeotp.keyboard;

/** USB HID usages for a US keyboard. No Android dependency: reusable by FreeOTP. */
public final class KeyboardCodec {
    private KeyboardCodec() {}
    public static byte[] report(char c) {
        int key = 0, modifier = 0;
        if (c >= 'a' && c <= 'z') key = 4 + c - 'a';
        else if (c >= 'A' && c <= 'Z') { key = 4 + c - 'A'; modifier = 2; }
        else if (c >= '1' && c <= '9') key = 30 + c - '1';
        else if (c == '0') key = 39;
        else {
            String plain = "\n\t -=[]\\;'/`,.";
            int[] usages = {40,43,44,45,46,47,48,49,51,52,56,53,54,55};
            int i = plain.indexOf(c);
            if (i >= 0) key = usages[i];
            else {
                String shifted = "!@#$%^&*()_+{}|:\"?~<>";
                int[] shiftKeys = {30,31,32,33,34,35,36,37,38,39,45,46,47,48,49,51,52,56,53,54,55};
                i = shifted.indexOf(c);
                if (i >= 0) { key = shiftKeys[i]; modifier = 2; }
            }
        }
        if (key == 0) throw new IllegalArgumentException(String.format("Unsupported character U+%04X. Use the computer's US English keyboard layout.", (int)c));
        return new byte[]{(byte)modifier,0,(byte)key,0,0,0,0,0};
    }
    public static String normalize(String text) { return text.replace("\r\n", "\n").replace('\r', '\n'); }
    public static void validate(String text) { for (int i=0;i<text.length();i++) report(text.charAt(i)); }
}
