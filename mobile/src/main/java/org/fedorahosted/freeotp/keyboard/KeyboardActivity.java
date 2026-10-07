package org.fedorahosted.freeotp.keyboard;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.main.share.SharingSettings;

import android.Manifest;
import android.app.*;
import android.bluetooth.*;
import android.content.*;
import android.content.pm.*;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class KeyboardActivity extends Activity implements KeyboardLink.Listener {
    private KeyboardLink link;
    private android.content.SharedPreferences prefs;
    private EditText text;
    private TextView status;
    private LinearLayout targets;
    private Button send;
    private String selected, sharedText;
    private boolean autoPending, pairWaiting;
    private long pairingUntil;
    private boolean scanWaiting;
    private BluetoothDevice pairingDevice;
    private AlertDialog scanDialog;
    private ArrayAdapter<String> scanRows;
    private final List<BluetoothDevice> foundDevices=new ArrayList<>();
    private final Handler uiHandler=new Handler(Looper.getMainLooper());
    private final BroadcastReceiver bluetoothChanges=new BroadcastReceiver() {
        @Override public void onReceive(Context context,Intent intent) {
            String action=intent.getAction();
            BluetoothDevice device=intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
            if(BluetoothDevice.ACTION_FOUND.equals(action)) {
                if(scanDialog!=null && device!=null && eligible(device) && !foundDevices.contains(device)) {
                    foundDevices.add(device);
                    scanRows.add(deviceName(device)+"\n"+device.getAddress());
                }
            } else if(BluetoothAdapter.ACTION_DISCOVERY_FINISHED.equals(action)) {
                if(scanDialog!=null) scanDialog.setTitle(foundDevices.isEmpty()?"No computers found · Try again":"Choose a computer");
            } else if(BluetoothDevice.ACTION_PAIRING_REQUEST.equals(action)) {
                status.setText("Confirm pairing on both devices. If no phone prompt appears, open notifications and tap the Bluetooth pairing request.");
                Toast.makeText(KeyboardActivity.this,"Check notifications for the Bluetooth pairing confirmation",Toast.LENGTH_LONG).show();
            } else if(BluetoothDevice.ACTION_BOND_STATE_CHANGED.equals(action)) {
                refresh();
                if(device!=null && device.equals(pairingDevice)) {
                    int bond=intent.getIntExtra(BluetoothDevice.EXTRA_BOND_STATE,BluetoothDevice.BOND_NONE);
                    if(bond==BluetoothDevice.BOND_BONDED) finishPairing();
                    else if(bond==BluetoothDevice.BOND_NONE) { pairingDevice=null; status.setText("Pairing failed or was cancelled. Confirm the pairing prompt on both devices and retry."); }
                    else status.setText("Confirm pairing on both devices. Check the phone's notifications if the prompt is hidden.");
                }
            }
            else if(BluetoothAdapter.ACTION_STATE_CHANGED.equals(intent.getAction())) {
                if(allowed()) link.prepare();
                refresh();
            }
        }
    };
    private int ink, muted, surface, background;
    private final int blue=Color.rgb(71,93,235);
    private int dp(int n) { return Math.round(n*getResources().getDisplayMetrics().density); }
    private BluetoothAdapter adapter() { BluetoothManager m=getSystemService(BluetoothManager.class); return m==null?null:m.getAdapter(); }
    private boolean allowed() { return Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)==PackageManager.PERMISSION_GRANTED; }
    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        if(Build.VERSION.SDK_INT<28 || !SharingSettings.keyboardEnabled(this)) { finish(); return; }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        prefs=getSharedPreferences("keyboard_destinations",MODE_PRIVATE);
        selected=state==null?prefs.getString("default",null):state.getString("selected");
        boolean dark=(getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)==Configuration.UI_MODE_NIGHT_YES;
        background=Color.parseColor(dark?"#111521":"#F5F6FC"); surface=Color.parseColor(dark?"#202638":"#FFFFFF"); ink=Color.parseColor(dark?"#F2F4FF":"#172039"); muted=Color.parseColor(dark?"#B0B8D0":"#68738E");
        link=(KeyboardLink)getLastNonConfigurationInstance(); if(link==null) link=new KeyboardLink(this);
        buildUi();
        if(state!=null) { text.setText(state.getString("text","")); autoPending=state.getBoolean("autoPending"); String pairingAddress=state.getString("pairingDevice"); if(pairingAddress!=null && adapter()!=null) pairingDevice=adapter().getRemoteDevice(pairingAddress); }
        else receive(getIntent());
        link.attach(this);
        if(!allowed()) requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},10);
    }
    private void receive(Intent intent) {
        if(Intent.ACTION_SEND.equals(intent.getAction())) {
            CharSequence extra=intent.getCharSequenceExtra(Intent.EXTRA_TEXT);
            if(extra!=null) { sharedText=extra.toString(); text.setText(sharedText); text.setVisibility(View.VISIBLE); send.setVisibility(View.VISIBLE); autoPending=true; }

        }
        String address=intent.getStringExtra("computer"); if(address!=null && BluetoothAdapter.checkBluetoothAddress(address)) selected=address;
    }
    @Override public void onNewIntent(Intent intent) { super.onNewIntent(intent); if(link.isBusy()) { Toast.makeText(this,"Finish this send before sending another code",Toast.LENGTH_LONG).show(); return; } setIntent(intent); receive(intent); refresh(); maybeAutoSend(); }
    @Override public void onStart() {
        super.onStart(); if(link==null) return;
        IntentFilter changes=new IntentFilter(BluetoothDevice.ACTION_BOND_STATE_CHANGED);
        changes.addAction(BluetoothAdapter.ACTION_STATE_CHANGED);
        changes.addAction(BluetoothDevice.ACTION_FOUND);
        changes.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        changes.addAction(BluetoothDevice.ACTION_PAIRING_REQUEST);
        if(Build.VERSION.SDK_INT>=33) registerReceiver(bluetoothChanges,changes,Context.RECEIVER_EXPORTED);
        else registerReceiver(bluetoothChanges,changes);
    }
    @Override public void onStop() { if(link==null) { super.onStop(); return; } stopDiscovery(); if(scanDialog!=null) scanDialog.dismiss(); unregisterReceiver(bluetoothChanges); super.onStop(); }
    @Override public void onResume() { super.onResume(); if(link==null) return; refresh(); if(allowed()) link.prepare(); if(pairingDevice!=null && allowed() && pairingDevice.getBondState()==BluetoothDevice.BOND_BONDED) finishPairing(); maybeAutoSend(); }
    @Override public void onRequestPermissionsResult(int r,String[] p,int[] result) {
        super.onRequestPermissionsResult(r,p,result); refresh();
        if(allowed()) link.prepare();
        if(r==12) {
            if(scanAllowed() && allowed()) findComputer();
            else status.setText("Allow permission to search for nearby computers.");
        } else if(r==11) {
            if(Build.VERSION.SDK_INT<31 || checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)==PackageManager.PERMISSION_GRANTED) makeVisible();
            else status.setText("Allow Nearby devices to make this phone discoverable.");
        } else maybeAutoSend();
    }
    private void maybeAutoSend() { if(autoPending && allowed() && selected!=null && !link.isBusy()) { autoPending=false; transmit(); } }
    @Override public Object onRetainNonConfigurationInstance() { link.listener=null; return link; }
    @Override protected void onDestroy() { uiHandler.removeCallbacksAndMessages(null); if(link!=null) { if(!isChangingConfigurations()) link.close(); else link.listener=null; } super.onDestroy(); }
    @Override protected void onSaveInstanceState(Bundle out) { super.onSaveInstanceState(out); out.putString("selected",selected); out.putString("text",text.getText().toString()); out.putBoolean("autoPending",autoPending); if(pairingDevice!=null) out.putString("pairingDevice",pairingDevice.getAddress()); }
    private GradientDrawable round(int color,int radius) { GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(dp(radius)); return d; }
    private TextView label(String value,int size,int color) { TextView t=new TextView(this); t.setText(value); t.setTextSize(size); t.setTextColor(color); return t; }
    private Button button(String title,Runnable action) { Button b=new Button(this); b.setText(title); b.setAllCaps(false); b.setTextSize(16); b.setTextColor(ink); b.setBackground(round(surface,16)); b.setPadding(dp(16),0,dp(16),0); b.setMinimumHeight(dp(52)); b.setOnClickListener(v->action.run()); return b; }
    private void buildUi() {
        ScrollView scroll=new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(background);
        LinearLayout body=new LinearLayout(this); body.setOrientation(1); body.setPadding(dp(24),dp(24),dp(24),dp(24)); scroll.addView(body); setContentView(scroll);
        scroll.setOnApplyWindowInsetsListener((v,insets)->{ body.setPadding(dp(24),dp(24)+insets.getSystemWindowInsetTop(),dp(24),dp(24)+insets.getSystemWindowInsetBottom()); return insets; });
        LinearLayout header=new LinearLayout(this); header.setGravity(Gravity.CENTER_VERTICAL);
        TextView name=label("Keyboard destinations",22,ink); name.setTypeface(null,Typeface.BOLD);
        Button back=button("‹",this::finish); back.setContentDescription("Back"); header.addView(back,new LinearLayout.LayoutParams(dp(48),dp(48)));
        header.addView(name,new LinearLayout.LayoutParams(0,-2,1));
        Button menu=button("⋮",()->{}); menu.setContentDescription("More options");
        menu.setOnClickListener(v->{
            PopupMenu options=new PopupMenu(this,menu);
            options.getMenu().add(0,2,1,"Show all devices").setCheckable(true).setChecked(prefs.getBoolean("showAll",false));
            options.getMenu().add(0,3,2,"Copy connection diagnostics");
            options.getMenu().add(0,4,3,"Keyboard layout");
            options.setOnMenuItemClickListener(item->{
                if(item.getItemId()==2) { prefs.edit().putBoolean("showAll",!prefs.getBoolean("showAll",false)).apply(); refresh(); }
                else if(item.getItemId()==3) {
                    getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText("FreeOTP Plus diagnostics",link.diagnostics()));
                    Toast.makeText(this,"Diagnostics copied",Toast.LENGTH_SHORT).show();
                } else new AlertDialog.Builder(this).setTitle("Keyboard layout").setMessage("Use a US English keyboard layout with Caps Lock off on the receiving device. Printable ASCII, tabs, and line breaks are supported. No Enter is added automatically.").setPositiveButton("OK",null).show();
                return true;
            }); options.show();
        });
        header.addView(menu,new LinearLayout.LayoutParams(dp(52),dp(48))); body.addView(header);
        text=new EditText(this); text.setHint("Code to send"); text.setTextColor(ink); text.setHintTextColor(muted); text.setTextSize(18);
        text.setMinLines(1); text.setMaxLines(2); text.setGravity(Gravity.TOP|Gravity.START);
        text.setInputType(android.text.InputType.TYPE_CLASS_TEXT|android.text.InputType.TYPE_TEXT_FLAG_MULTI_LINE|android.text.InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        text.setSaveEnabled(false);
        boolean sharing=Intent.ACTION_SEND.equals(getIntent().getAction()) || sharedText!=null;
        text.setVisibility(sharing?View.VISIBLE:View.GONE);
        body.addView(text,new LinearLayout.LayoutParams(-1,-2));
        send=button("Send",()->{ autoPending=false; transmit(); }); send.setTextColor(Color.WHITE); send.setBackground(round(blue,14));
        LinearLayout.LayoutParams sendParams=new LinearLayout.LayoutParams(-1,dp(52)); sendParams.topMargin=dp(12); body.addView(send,sendParams); send.setVisibility(sharing?View.VISIBLE:View.GONE);
        status=label("Ready",14,muted); status.setPadding(0,dp(10),0,dp(20)); body.addView(status);
        LinearLayout deviceHeader=new LinearLayout(this); deviceHeader.setGravity(Gravity.CENTER_VERTICAL);
        TextView devicesLabel=label("Devices",18,ink); devicesLabel.setTypeface(null,Typeface.BOLD); deviceHeader.addView(devicesLabel,new LinearLayout.LayoutParams(0,-2,1));
        deviceHeader.addView(button("＋ Add",this::pair)); body.addView(deviceHeader);
        targets=new LinearLayout(this); targets.setOrientation(1); body.addView(targets);
    }
    private boolean eligible(BluetoothDevice device) {
        if(prefs.getBoolean("showAll",false)) return true;
        BluetoothClass type=device.getBluetoothClass();
        if(type==null) return true;
        switch(type.getMajorDeviceClass()) {
            case BluetoothClass.Device.Major.AUDIO_VIDEO:
            case BluetoothClass.Device.Major.WEARABLE:
            case BluetoothClass.Device.Major.PERIPHERAL:
            case BluetoothClass.Device.Major.HEALTH:
            case BluetoothClass.Device.Major.TOY:
            case BluetoothClass.Device.Major.IMAGING: return false;
            default: return true;
        }
    }
    private List<BluetoothDevice> devices() {
        List<BluetoothDevice> list=new ArrayList<>();
        if(!allowed() || adapter()==null) return list;
        for(BluetoothDevice device:adapter().getBondedDevices()) if(eligible(device)) list.add(device);
        list.sort((a,b)->Long.compare(prefs.getLong("recent:"+b.getAddress(),0),prefs.getLong("recent:"+a.getAddress(),0))); return list;
    }
    private BluetoothDevice current() { if(selected==null) return null; for(BluetoothDevice d:devices()) if(d.getAddress().equals(selected)) return d; return null; }
    private void refresh() {
        if(targets==null) return;
        targets.removeAllViews();
        if(!allowed()) { targets.addView(button("Allow Nearby devices",()->requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},10))); return; }
        List<BluetoothDevice> all=devices();
        if(all.isEmpty()) { TextView empty=label("No paired devices",15,muted); empty.setPadding(0,dp(16),0,dp(16)); targets.addView(empty); }
        for(BluetoothDevice dev:all) {
            LinearLayout row=new LinearLayout(this); row.setGravity(Gravity.CENTER_VERTICAL); row.setPadding(dp(16),dp(14),dp(16),dp(14)); row.setBackground(round(surface,18)); LinearLayout.LayoutParams rp=new LinearLayout.LayoutParams(-1,-2); rp.setMargins(0,dp(10),0,0); targets.addView(row,rp);
            ImageView icon=new ImageView(this); icon.setImageResource(R.drawable.ic_keyboard_destination); row.addView(icon,new LinearLayout.LayoutParams(dp(32),dp(32)));
            LinearLayout names=new LinearLayout(this); names.setOrientation(1); names.setPadding(dp(14),0,0,0); names.addView(label(dev.getName()==null?dev.getAddress():dev.getName(),17,ink)); row.addView(names,new LinearLayout.LayoutParams(0,-2,1)); row.addView(label(dev.getAddress().equals(selected)?"✓":"",22,blue)); row.setContentDescription("Select "+dev.getName()); row.setOnClickListener(v->select(dev));
        }

    }
    private void select(BluetoothDevice d) { if(link.isBusy()) return; selected=d.getAddress(); prefs.edit().putString("default",selected).apply(); refresh(); maybeAutoSend(); }
    private void choose() {
        if(link.isBusy()) return;
        if(!allowed()) { requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},10); return; }
        List<BluetoothDevice> list=devices();
        if(list.isEmpty()) { pair(); return; }
        ArrayAdapter<BluetoothDevice> rows=new ArrayAdapter<BluetoothDevice>(this,android.R.layout.simple_list_item_1,list) {
            @Override public View getView(int position,View recycled,ViewGroup parent) {
                BluetoothDevice device=getItem(position);
                LinearLayout row=new LinearLayout(KeyboardActivity.this); row.setOrientation(LinearLayout.HORIZONTAL); row.setGravity(Gravity.CENTER_VERTICAL);
                row.setPadding(dp(20),dp(16),dp(20),dp(16));
                LinearLayout names=new LinearLayout(KeyboardActivity.this); names.setOrientation(LinearLayout.VERTICAL);
                TextView name=label(deviceName(device),17,ink); names.addView(name);
                TextView address=label(device.getAddress(),12,muted); address.setPadding(0,dp(4),0,0); names.addView(address);
                row.addView(names,new LinearLayout.LayoutParams(0,-2,1));
                row.addView(label(device.getAddress().equals(selected)?"✓":"",22,blue));
                return row;
            }
        };
        AlertDialog dialog=new AlertDialog.Builder(this).setTitle("Choose destination")
            .setAdapter(rows,(d,index)->select(list.get(index)))
            .setNeutralButton("Add destination",(d,index)->pair()).setNegativeButton("Cancel",null).create();
        dialog.show();
        dialog.getListView().setDivider(new ColorDrawable(Color.argb(65,128,128,128)));
        dialog.getListView().setDividerHeight(dp(1));
    }
    private void pair() {
        if(link.isBusy()) return;
        new AlertDialog.Builder(this).setTitle("Pair a computer")
            .setMessage("Make your computer visible in its Bluetooth settings, then find it from this phone. Keep FreeOTP Plus open during pairing.")
            .setPositiveButton("Find computer",(d,w)->findComputer())
            .setNeutralButton("Make phone visible",(d,w)->makeVisible())
            .setNegativeButton("Cancel",null).show();
    }
    private String deviceName(BluetoothDevice device) { String name=device.getName(); return name==null?"Unnamed device":name; }
    private boolean scanAllowed() {
        return checkSelfPermission(Build.VERSION.SDK_INT>=31?Manifest.permission.BLUETOOTH_SCAN:Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED;
    }
    private void findComputer() {
        if(!allowed() || !scanAllowed()) {
            requestPermissions(Build.VERSION.SDK_INT>=31?new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_SCAN}:new String[]{Manifest.permission.ACCESS_FINE_LOCATION},12);
            return;
        }
        if(adapter()==null) { status.setText("This phone has no Bluetooth adapter."); return; }
        if(Build.VERSION.SDK_INT<31 && !getSystemService(android.location.LocationManager.class).isLocationEnabled()) {
            new AlertDialog.Builder(this).setMessage("Android requires Location to be on to find Bluetooth devices. Enable it, then try Find computer again.")
                .setPositiveButton("Open settings",(d,w)->startActivity(new Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))).setNegativeButton("Cancel",null).show(); return;
        }
        scanWaiting=true;
        if(!adapter().isEnabled()) startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),20);
        else { link.prepare(); ready(); }
    }
    private void showScan() {
        stopDiscovery(); foundDevices.clear();
        scanRows=new ArrayAdapter<>(this,android.R.layout.simple_list_item_1);
        scanDialog=new AlertDialog.Builder(this).setTitle("Searching for computers…")
            .setAdapter(scanRows,(d,index)->beginPairing(foundDevices.get(index)))
            .setPositiveButton("Search again",null).setNegativeButton("Cancel",null).create();
        scanDialog.setOnDismissListener(d->{ stopDiscovery(); scanDialog=null; });
        scanDialog.show();
        scanDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v->{ stopDiscovery(); foundDevices.clear(); scanRows.clear(); startDiscovery(); });
        status.setText("Make your computer visible, then select it here.");
        startDiscovery();
    }
    private void startDiscovery() {
        try { if(!adapter().startDiscovery()) scanDialog.setTitle("Search could not start · Try again"); else scanDialog.setTitle("Searching for computers…"); }
        catch(SecurityException e) { status.setText("Bluetooth search permission was removed."); }
    }
    private void stopDiscovery() {
        if(adapter()!=null && scanAllowed()) try { adapter().cancelDiscovery(); } catch(SecurityException ignored) {}
    }
    private void beginPairing(BluetoothDevice device) {
        stopDiscovery(); pairingDevice=device;
        if(device.getBondState()==BluetoothDevice.BOND_BONDED) { finishPairing(); return; }
        status.setText("Pairing with "+deviceName(device)+". Confirm on both devices; check the phone's notifications for the pairing prompt.");
        try { if(!device.createBond()) { pairingDevice=null; status.setText("Could not start pairing. Try again from Bluetooth settings."); } }
        catch(SecurityException e) { pairingDevice=null; status.setText("Allow Nearby devices permission, then retry."); }
    }
    private void finishPairing() {
        BluetoothDevice device=pairingDevice; pairingDevice=null;
        select(device);
        if(!link.isBusy()) status.setText("Paired with "+deviceName(device)+". On the computer, allow keyboard access and mark this phone trusted for reconnection.");
    }
    private void makeVisible() {
        if(!allowed() || (Build.VERSION.SDK_INT>=31 && checkSelfPermission(Manifest.permission.BLUETOOTH_ADVERTISE)!=PackageManager.PERMISSION_GRANTED)) {
            requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT,Manifest.permission.BLUETOOTH_ADVERTISE},11); return;
        }
        if(adapter()==null) { status.setText("This phone has no Bluetooth adapter."); return; }
        pairWaiting=true;
        if(!adapter().isEnabled()) startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE),20);
        else { link.prepare(); ready(); }
    }
    @Override public void ready() {
        if(!link.isReady()) return;
        if(pairingDevice!=null) status.setText("Confirm pairing on both devices. Check the phone's notifications if the prompt is hidden.");
        if(scanWaiting) { scanWaiting=false; showScan(); return; }
        if(!pairWaiting) return;
        pairWaiting=false;
        startActivityForResult(new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE)
            .putExtra(BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,120),21);
    }
    @Override public void onActivityResult(int request,int result,Intent data) {
        super.onActivityResult(request,result,data);
        if(request==20) {
            if(result==RESULT_OK) { link.prepare(); ready(); }
            else { pairWaiting=false; scanWaiting=false; status.setText("Bluetooth was not enabled."); }
        } else if(request==21) {
            if(result>0) {
                pairingUntil=SystemClock.elapsedRealtime()+result*1000L;
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
                uiHandler.postDelayed(()->{ if(!link.isBusy()) getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); },result*1000L);
                status.setText("Pair from your computer now. Keep this screen open.");
            } else status.setText("Phone visibility was not enabled.");
        }
    }
    private void transmit() {
        if(link.isBusy()) return;
        if(!allowed()) { requestPermissions(new String[]{Manifest.permission.BLUETOOTH_CONNECT},10); return; }
        if(adapter()==null) { status.setText("This phone has no Bluetooth adapter."); return; }
        if(!adapter().isEnabled()) { status.setText("Enable Bluetooth, then tap Send."); startActivity(new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)); return; }
        BluetoothDevice device=current(); if(device==null) { choose(); return; }
        String value=text.getText().toString(); if(value.isEmpty()) { status.setText("Add some text first."); return; }
        try { link.send(device,value); } catch(IllegalArgumentException e) { status.setText(e.getMessage()); }
    }
    @Override public void status(String message,boolean busy) { status.setText(message); send.setEnabled(!busy); text.setEnabled(!busy); if(busy || SystemClock.elapsedRealtime()<pairingUntil) getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); else getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); }
    @Override public void sent() { if(selected!=null) { prefs.edit().putLong("recent:"+selected,System.currentTimeMillis()).apply();  } text.setText(""); refresh(); }
}
