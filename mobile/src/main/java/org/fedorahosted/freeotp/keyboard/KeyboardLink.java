package org.fedorahosted.freeotp.keyboard;

import android.bluetooth.*;
import android.content.Context;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import java.util.ArrayDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Foreground HID session, retained across Activity recreation. */
public final class KeyboardLink implements BluetoothProfile.ServiceListener {
    public interface Listener { void status(String message, boolean busy); void sent(); default void ready() {} }
    private final Context context;
    private final BluetoothAdapter adapter;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService callbackExecutor = Executors.newSingleThreadExecutor();
    private final Object typingTasks = new Object();
    private final ArrayDeque<String> events = new ArrayDeque<>();
    private BluetoothHidDevice hid;
    private BluetoothDevice target;
    private boolean registered, registering, proxyRequested, busy, closed, typing, settling;
    private String pending;
    private int position, reports;
    private byte[] currentReport = new byte[8];
    private byte leds;
    public Listener listener;
    private String message = "Ready to connect";
    private final byte[] released = new byte[8];
    // Standard boot-compatible keyboard report; no report ID.
    private static final byte[] DESCRIPTOR = new byte[]{
        5,1,9,6,(byte)0xa1,1,5,7,0x19,(byte)0xe0,0x29,(byte)0xe7,0x15,0,0x25,1,0x75,1,(byte)0x95,8,(byte)0x81,2,
        (byte)0x95,1,0x75,8,(byte)0x81,1,(byte)0x95,5,0x75,1,5,8,0x19,1,0x29,5,(byte)0x91,2,
        (byte)0x95,1,0x75,3,(byte)0x91,1,(byte)0x95,6,0x75,8,0x15,0,0x25,0x65,5,7,0x19,0,0x29,0x65,(byte)0x81,0,(byte)0xc0};
    public KeyboardLink(Context context) {
        this.context = context.getApplicationContext();
        BluetoothManager manager = context.getSystemService(BluetoothManager.class);
        adapter = manager == null ? null : manager.getAdapter();
    }
    public boolean isBusy() { return busy; }
    public void attach(Listener l) { listener=l; l.status(message,busy); }
    private void trace(String value) {
        if(events.size()>=60) events.removeFirst();
        events.addLast(SystemClock.elapsedRealtime()+" " + value);
    }
    public String diagnostics() {
        StringBuilder result=new StringBuilder("FreeOTP Plus keyboard 2.0.6-plus.3\nAndroid API "+android.os.Build.VERSION.SDK_INT+"\n");
        for(String event:events) result.append(event).append('\n');
        return result.toString(); // No text, key values, or Bluetooth addresses.
    }
    private void state(String m) { message=m; if (listener!=null) listener.status(m,busy); }
    private void later(Runnable task, long delay) { main.postAtTime(task,typingTasks,SystemClock.uptimeMillis()+delay); }
    public void prepare() {
        if(closed || registered || registering || proxyRequested) return;
        try {
            if(adapter==null) { state("This phone has no Bluetooth adapter."); return; }
            if(!adapter.isEnabled()) { state("Enable Bluetooth to make the keyboard available."); return; }
            if(!busy) state("Preparing Bluetooth keyboard…");
            if(hid!=null) register();
            else {
                proxyRequested=adapter.getProfileProxy(context,this,BluetoothProfile.HID_DEVICE);
                if(!proxyRequested) fail("Bluetooth keyboard mode is unavailable on this phone.");
            }
        } catch(SecurityException e) { fail("Allow Nearby devices permission, then retry."); }
    }
    public boolean isReady() { return registered && !closed; }
    public void send(BluetoothDevice device, String text) {
        if (busy || closed) return;
        text = KeyboardCodec.normalize(text);
        KeyboardCodec.validate(text);
        if(hid!=null && target!=null && !target.equals(device)) hid.disconnect(target);
        target=device; pending=text; busy=true; position=0; reports=0; typing=false; settling=false;
        trace("Send requested; characters="+text.length());
        state("Connecting keyboard to " + device.getName() + "…");
        main.postDelayed(timeout, 20000);
        try {
            if (hid != null && registered) connect();
            else prepare();
        } catch (SecurityException e) { fail("Allow Nearby devices permission, then retry."); }
    }
    private final Runnable timeout = () -> fail("Keyboard connection timed out. Check that the computer accepts this phone as an input device.");
    private void register() {
        if(registering || registered || closed) return;
        trace("Register keyboard"); registering=true;
        if(!hid.registerApp(new BluetoothHidDeviceAppSdpSettings("FreeOTP Plus","Text keyboard","FreeOTP Plus",BluetoothHidDevice.SUBCLASS1_KEYBOARD,DESCRIPTOR),null,null,callbackExecutor,callback)) {
            registering=false;
            fail("Could not register Bluetooth keyboard mode. Close other keyboard apps, then retry.");
        }
    }
    @Override public void onServiceConnected(int profile, BluetoothProfile proxy) {
        main.post(() -> {
            if (closed) { adapter.closeProfileProxy(profile,proxy); return; }
            proxyRequested=false;
            trace("HID profile available"); hid=(BluetoothHidDevice)proxy;
            try { register(); } catch(SecurityException e) { fail("Bluetooth permission was removed."); }
        });
    }
    @Override public void onServiceDisconnected(int profile) { main.post(() -> { if(closed) return; trace("HID profile lost"); hid=null; registered=false; registering=false; proxyRequested=false; fail("Bluetooth keyboard service disconnected."); }); }
    private final BluetoothHidDevice.Callback callback = new BluetoothHidDevice.Callback() {
        @Override public void onAppStatusChanged(BluetoothDevice device, boolean ok) { main.post(() -> {
            if(closed) return;
            trace("Registered="+ok+"; existing host="+(device!=null)); registered=ok; registering=false;
            if(ok) {
                if(busy) connect();
                else state("Keyboard ready");
                if(listener!=null) listener.ready();
            } else if(!ok) {
                if(busy) fail("Keyboard session ended. Keep FreeOTP Plus visible and retry.");
                else state("Keyboard session ended. Tap Send to register again.");
            }
        }); }
        @Override public void onConnectionStateChanged(BluetoothDevice device, int connectionState) {
            main.post(() -> {
                if(closed || target==null || !target.equals(device)) return;
                trace("HID connection="+connectionState+"; accepted reports="+reports);
                if(connectionState==BluetoothProfile.STATE_CONNECTED) { if(busy) ready(); else state("Keyboard connected"); }
                else if(connectionState==BluetoothProfile.STATE_CONNECTING && busy) state("Connecting keyboard…");
                else if(connectionState==BluetoothProfile.STATE_DISCONNECTED) {
                    if(busy) fail(reports==0?"Computer disconnected before typing. Check keyboard/input authorization in Bluetooth settings. Copy diagnostics below if it repeats.":"Keyboard disconnected after "+position+" characters. Check for partial text before retrying.");
                    else state("Keyboard disconnected. Tap Send to reconnect.");
                }
            });
        }
        @Override public void onGetReport(BluetoothDevice device, byte type, byte id, int size) { main.post(() -> {
            if(hid==null || closed) return;
            trace("GET_REPORT type="+type+" id="+id+" size="+size);
            try {
                if(id!=0) { hid.reportError(device,BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID); return; }
                if(type!=BluetoothHidDevice.REPORT_TYPE_INPUT && type!=BluetoothHidDevice.REPORT_TYPE_OUTPUT) {
                    hid.reportError(device,BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ); return;
                }
                byte[] data=KeyboardReports.get(type,id,size,currentReport,leds);
                trace("GET_REPORT reply="+hid.replyReport(device,type,id,data));
            } catch(SecurityException e) { fail("Bluetooth permission was removed."); }
        }); }
        @Override public void onSetReport(BluetoothDevice device, byte type, byte id, byte[] data) { main.post(() -> {
            if(hid==null || closed) return;
            trace("SET_REPORT type="+type+" id="+id+" bytes="+(data==null?0:data.length));
            try {
                if(id!=0) hid.reportError(device,BluetoothHidDevice.ERROR_RSP_INVALID_RPT_ID);
                else if(type!=BluetoothHidDevice.REPORT_TYPE_OUTPUT) hid.reportError(device,BluetoothHidDevice.ERROR_RSP_UNSUPPORTED_REQ);
                else if(data==null || data.length!=1) hid.reportError(device,BluetoothHidDevice.ERROR_RSP_INVALID_PARAM);
                else leds=data[0];
            } catch(SecurityException e) { fail("Bluetooth permission was removed."); }
        }); }
        @Override public void onInterruptData(BluetoothDevice device, byte id, byte[] data) { main.post(() -> { if(!closed && id==0 && data!=null && data.length==1) leds=data[0]; }); }
        @Override public void onSetProtocol(BluetoothDevice device, byte protocol) { main.post(() -> { if(!closed) trace("Protocol="+protocol); }); } // Same eight-byte format in both modes.
        @Override public void onVirtualCableUnplug(BluetoothDevice device) { main.post(() -> { if(!closed) { trace("Host removed virtual keyboard"); if(busy) fail("Computer removed the keyboard connection. Reconnect from Bluetooth settings."); } }); }
    };
    private void connect() {
        try {
            int connectionState=hid.getConnectionState(target); trace("Connect; current HID state="+connectionState);
            if(connectionState==BluetoothProfile.STATE_CONNECTED) ready();
            else if(connectionState!=BluetoothProfile.STATE_CONNECTING && !hid.connect(target)) fail("Keyboard connection rejected. Pair this phone in the computer's Bluetooth settings.");
        } catch(SecurityException e) { fail("Bluetooth permission was removed."); }
    }
    private void ready() {
        if(typing || settling || !busy || closed) return;
        settling=true; state("Keyboard connected. Preparing to type…");
        // Give the host time to create its input device before the first key.
        later(() -> {
            settling=false;
            if(!busy || closed || hid==null) return;
            try {
                if(hid.getConnectionState(target)!=BluetoothProfile.STATE_CONNECTED) { fail("Keyboard disconnected before typing."); return; }
                typing=true; main.removeCallbacks(timeout); state("Typing…"); typeNext();
            } catch(SecurityException e) { fail("Bluetooth permission was removed."); }
        },750);
    }
    private boolean report(byte[] data) {
        if(hid==null || hid.getConnectionState(target)!=BluetoothProfile.STATE_CONNECTED) return false;
        boolean accepted=hid.sendReport(target,0,data);
        if(accepted) { currentReport=data.clone(); reports++; }
        return accepted;
    }
    private void typeNext() {
        if(!busy || closed) return;
        try {
            if(position>=pending.length()) {
                int count=pending.length(); busy=false; typing=false; pending=null;
                trace("Complete; characters="+count+" accepted reports="+reports);
                state("Keyboard reports sent · "+count+" characters"); if(listener!=null) listener.sent(); return;
            }
            if(!report(KeyboardCodec.report(pending.charAt(position)))) { fail("Key press rejected after "+position+" characters. Check for partial text before retrying."); return; }
            later(() -> {
                if(!busy || closed) return;
                try {
                    if(!report(released)) { fail("Key release rejected. Check for partial text before retrying."); return; }
                    position++; later(this::typeNext,40);
                } catch(Exception e) { trace("Release exception="+e.getClass().getSimpleName()); fail("Key release failed. Check the computer before retrying."); }
            },40);
        } catch(Exception e) { trace("Typing exception="+e.getClass().getSimpleName()); fail("Typing stopped after "+position+" characters. Check the computer before retrying."); }
    }
    private void fail(String m) {
        trace("Failed; characters="+position+" accepted reports="+reports);
        main.removeCallbacks(timeout); main.removeCallbacksAndMessages(typingTasks);
        if(hid!=null && target!=null) { try { hid.sendReport(target,0,released); } catch(Exception ignored) {} }
        currentReport=released.clone(); busy=false; typing=false; settling=false; pending=null; state(m);
    }
    public void close() {
        trace("Activity closed"); closed=true; listener=null; main.removeCallbacksAndMessages(null);
        if(hid!=null) { try { if(target!=null) { hid.sendReport(target,0,released); hid.disconnect(target); } hid.unregisterApp(); adapter.closeProfileProxy(BluetoothProfile.HID_DEVICE,hid); } catch(Exception ignored) {} }
        callbackExecutor.shutdown();
    }
}
