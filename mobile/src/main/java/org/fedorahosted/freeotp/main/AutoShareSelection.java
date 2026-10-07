package org.fedorahosted.freeotp.main;

/** One stable account UUID, independent of account ordering or visibility. */
public final class AutoShareSelection {
    private AutoShareSelection() {}
    public static String toggle(String selected,String clicked) { return clicked.equals(selected)?null:clicked; }
    public static String remove(String selected,String deleted) { return deleted.equals(selected)?null:selected; }
    public static boolean shouldShare(boolean enabled,String selected,String generated) { return enabled && generated.equals(selected); }
}
