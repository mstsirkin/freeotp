package org.fedorahosted.freeotp.shortcuts;

import android.Manifest;
import android.app.Dialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.Context;
import android.content.pm.PackageManager;
import android.content.pm.ShortcutInfo;
import android.content.pm.ShortcutManager;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.Icon;
import android.os.Build;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Gravity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenIcon;
import org.fedorahosted.freeotp.main.Adapter;
import org.fedorahosted.freeotp.main.share.ShareRoute;
import org.fedorahosted.freeotp.main.share.SharingSettings;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class ShortcutDialog extends DialogFragment {
    private EditText name, emoji;
    private Spinner destination, device, color, symbol;
    private LinearLayout keyboardOptions;
    private ImageView preview;
    private TokenIcon accountIcon;
    private final List<String> addresses = new ArrayList<>();
    private String restoredAddress;
    private final ShareRoute[] routes = {ShareRoute.KEYBOARD, ShareRoute.CLIPBOARD, ShareRoute.JELLING};
    private final int[] colors = {0, 0xff3949ab, 0xff00695c, 0xffad1457, 0xff6a1b9a, 0xffbf360c, 0xff37474f};
    private final String[] symbols = {"", "⌨", "🔑", "★", "🛡", ""};
    private final ActivityResultLauncher<String> bluetoothPermission = registerForActivityResult(
            new ActivityResultContracts.RequestPermission(), granted -> {
                if (granted) loadDevices();
                else if (isAdded()) Toast.makeText(requireContext(), R.string.shortcut_bluetooth_permission, Toast.LENGTH_LONG).show();
            });

    public static ShortcutDialog newInstance(String account) {
        ShortcutDialog dialog = new ShortcutDialog();
        Bundle args = new Bundle();
        args.putString("account", account);
        dialog.setArguments(args);
        return dialog;
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }

    @NonNull @Override public Dialog onCreateDialog(Bundle state) {
        Context context = requireContext();
        Token token;
        try {
            Adapter adapter = new Adapter(context, selected -> {});
            int position = adapter.positionOf(requireArguments().getString("account"));
            token = position < 0 ? null : adapter.getTokenInfo(position);
            adapter.close();
        } catch (GeneralSecurityException | IOException e) { token = null; }
        if (token == null) return new AlertDialog.Builder(context)
                .setMessage(R.string.shortcut_account_missing).setPositiveButton(R.string.close, null).create();
        accountIcon = new TokenIcon(token, context);

        ScrollView scroll = new ScrollView(context);
        LinearLayout body = new LinearLayout(context);
        body.setOrientation(LinearLayout.VERTICAL);
        body.setPadding(dp(24), dp(8), dp(24), dp(8));
        scroll.addView(body);
        preview = new ImageView(context);
        LinearLayout.LayoutParams previewParams = new LinearLayout.LayoutParams(dp(72), dp(72));
        previewParams.gravity = Gravity.CENTER_HORIZONTAL;
        body.addView(preview, previewParams);
        preview.setImportantForAccessibility(View.IMPORTANT_FOR_ACCESSIBILITY_NO);
        TextView explanation = new TextView(context);
        explanation.setText(R.string.shortcut_explanation);
        body.addView(explanation);

        name = new EditText(context);
        name.setTag("shortcut_name");
        name.setSingleLine(true);
        name.setFilters(new InputFilter[]{new InputFilter.LengthFilter(80)});
        String label = token.getIssuer();
        label = label == null || label.isEmpty() ? token.getLabel() : label + " " + token.getLabel();
        name.setText(state == null ? label : state.getString("name", label));
        field(body, R.string.shortcut_name, name);
        destination = spinner(new String[]{getString(R.string.share_settings_keyboard),
                getString(R.string.share_settings_clipboard), getString(R.string.share_settings_jelling)});
        ShareRoute current = SharingSettings.destination(context, requireArguments().getString("account"));
        destination.setSelection(state == null ? (current == ShareRoute.CLIPBOARD ? 1 : current == ShareRoute.JELLING ? 2 : 0)
                : state.getInt("destination"));
        destination.setTag("shortcut_destination");
        field(body, R.string.shortcut_destination, destination);

        keyboardOptions = new LinearLayout(context);
        keyboardOptions.setOrientation(LinearLayout.VERTICAL);
        body.addView(keyboardOptions);
        device = spinner(new String[]{getString(R.string.shortcut_default_device)});
        addresses.add(null);
        restoredAddress = state == null ? null : state.getString("computer");
        field(keyboardOptions, R.string.shortcut_keyboard_device, device);
        Button paired = new Button(context);
        paired.setText(R.string.shortcut_load_devices);
        paired.setAllCaps(false);
        paired.setOnClickListener(v -> {
            if (Build.VERSION.SDK_INT >= 31 && ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                    != PackageManager.PERMISSION_GRANTED) bluetoothPermission.launch(Manifest.permission.BLUETOOTH_CONNECT);
            else loadDevices();
        });
        keyboardOptions.addView(paired);
        if (Build.VERSION.SDK_INT < 31 || ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT)
                == PackageManager.PERMISSION_GRANTED) loadDevices();

        color = spinner(getResources().getStringArray(R.array.shortcut_colors));
        color.setSelection(state == null ? 0 : state.getInt("color"));
        color.setTag("shortcut_color");
        field(body, R.string.shortcut_color, color);
        symbol = spinner(getResources().getStringArray(R.array.shortcut_symbols));
        symbol.setSelection(state == null ? 0 : state.getInt("symbol"));
        symbol.setTag("shortcut_symbol");
        field(body, R.string.shortcut_icon, symbol);
        emoji = new EditText(context);
        emoji.setTag("shortcut_emoji");
        emoji.setSingleLine(true);
        emoji.setHint(R.string.shortcut_emoji_hint);
        emoji.setContentDescription(getString(R.string.shortcut_custom_emoji));
        emoji.setFilters(new InputFilter[]{new InputFilter.LengthFilter(16)});
        emoji.setText(state == null ? "" : state.getString("emoji", ""));
        body.addView(emoji);
        TextView badge = new TextView(context);
        badge.setText(R.string.shortcut_launcher_badge);
        body.addView(badge);

        AdapterView.OnItemSelectedListener changed = new AdapterView.OnItemSelectedListener() {
            @Override public void onItemSelected(AdapterView<?> parent, View view, int position, long id) { updatePreview(); }
            @Override public void onNothingSelected(AdapterView<?> parent) {}
        };
        destination.setOnItemSelectedListener(changed);
        color.setOnItemSelectedListener(changed);
        symbol.setOnItemSelectedListener(changed);
        emoji.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) { updatePreview(); }
            @Override public void afterTextChanged(Editable s) {}
        });
        updatePreview();
        return new AlertDialog.Builder(context).setTitle(R.string.shortcut_create).setView(scroll)
                .setNegativeButton(R.string.cancel, null).setPositiveButton(R.string.shortcut_add, null).create();
    }

    @Override public void onStart() {
        super.onStart();
        requireDialog().getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN);
        if (name != null) ((AlertDialog) requireDialog()).getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> pin());
    }

    private Spinner spinner(String[] labels) {
        Spinner view = new Spinner(requireContext());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        view.setAdapter(adapter);
        view.setMinimumHeight(dp(48));
        return view;
    }

    private void field(LinearLayout body, int title, View view) {
        TextView label = new TextView(requireContext());
        label.setText(title);
        label.setPadding(0, dp(12), 0, 0);
        int id = View.generateViewId();
        view.setId(id);
        label.setLabelFor(id);
        body.addView(label);
        body.addView(view, new LinearLayout.LayoutParams(-1, -2));
    }

    private void loadDevices() {
        if (!isAdded() || device == null) return;
        String selected = restoredAddress != null ? restoredAddress : addresses.get(device.getSelectedItemPosition());
        restoredAddress = selected;
        List<String> labels = new ArrayList<>();
        labels.add(getString(R.string.shortcut_default_device));
        addresses.clear();
        addresses.add(null);
        try {
            BluetoothManager manager = requireContext().getSystemService(BluetoothManager.class);
            BluetoothAdapter adapter = manager == null ? null : manager.getAdapter();
            if (adapter != null) {
                List<BluetoothDevice> devices = new ArrayList<>(adapter.getBondedDevices());
                devices.sort(Comparator.comparing(BluetoothDevice::getAddress));
                for (BluetoothDevice item : devices) {
                    labels.add((item.getName() == null ? getString(R.string.shortcut_unnamed_device) : item.getName())
                            + "\n" + item.getAddress());
                    addresses.add(item.getAddress());
                }
            }
        } catch (SecurityException ignored) { /* The default option remains usable. */ }
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_item, labels);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        device.setAdapter(adapter);
        int index = addresses.indexOf(selected);
        device.setSelection(index < 0 ? 0 : index);
        restoredAddress = null;
    }

    private void updatePreview() {
        if (symbol == null || emoji == null) return;
        keyboardOptions.setVisibility(destination.getSelectedItemPosition() == 0 ? View.VISIBLE : View.GONE);
        emoji.setVisibility(symbol.getSelectedItemPosition() == 5 ? View.VISIBLE : View.GONE);
        preview.setImageBitmap(iconBitmap(false));
    }

    private Bitmap iconBitmap(boolean adaptive) {
        Bitmap bitmap = Bitmap.createBitmap(192, 192, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        int background = color.getSelectedItemPosition() == 0 ? accountIcon.mColor : colors[color.getSelectedItemPosition()];
        paint.setColor(background);
        if (adaptive) canvas.drawColor(background); else canvas.drawCircle(96, 96, 96, paint);
        if (symbol.getSelectedItemPosition() == 0) {
            Bitmap custom = TokenIcon.embeddedImage(accountIcon.mImage.second);
            Drawable drawable = custom == null
                    ? ContextCompat.getDrawable(requireContext(), accountIcon.mImage.first)
                    : new android.graphics.drawable.BitmapDrawable(getResources(), custom);
            if (drawable != null) { drawable.setBounds(48, 48, 144, 144); drawable.draw(canvas); }
        } else {
            String glyph = symbol.getSelectedItemPosition() == 5 ? emoji.getText().toString().trim() : symbols[symbol.getSelectedItemPosition()];
            paint.setColor(Color.WHITE);
            paint.setTypeface(Typeface.DEFAULT_BOLD);
            paint.setTextAlign(Paint.Align.CENTER);
            paint.setTextSize(76);
            float width = paint.measureText(glyph);
            if (width > 108) paint.setTextSize(76 * 108 / width);
            canvas.drawText(glyph, 96, 96 - (paint.ascent() + paint.descent()) / 2, paint);
        }
        return bitmap;
    }

    private void pin() {
        String label = name.getText().toString().trim();
        if (label.isEmpty()) { name.setError(getString(R.string.shortcut_name_required)); return; }
        if (symbol.getSelectedItemPosition() == 5 && emoji.getText().toString().trim().isEmpty()) {
            emoji.setError(getString(R.string.shortcut_emoji_required)); return;
        }
        if (Build.VERSION.SDK_INT < 26) { message(R.string.shortcut_not_supported); return; }
        ShortcutManager manager = requireContext().getSystemService(ShortcutManager.class);
        if (manager == null || !manager.isRequestPinShortcutSupported()) { message(R.string.shortcut_not_supported); return; }
        ShareRoute route = routes[destination.getSelectedItemPosition()];
        if (!SharingSettings.enabled(requireContext(), route)) { message(R.string.share_destination_disabled); return; }
        String address = route == ShareRoute.KEYBOARD ? addresses.get(device.getSelectedItemPosition()) : null;
        String id = null;
        try {
            id = AccountShortcuts.save(requireContext(), new AccountShortcuts.Config(requireArguments().getString("account"), route, address));
            ShortcutInfo info = new ShortcutInfo.Builder(requireContext(), id).setShortLabel(label).setLongLabel(label)
                    .setIcon(Icon.createWithAdaptiveBitmap(iconBitmap(true)))
                    .setIntent(AccountShortcuts.intent(requireContext(), id)).build();
            if (!manager.requestPinShortcut(info, null)) {
                AccountShortcuts.remove(requireContext(), id);
                message(R.string.shortcut_not_supported);
                return;
            }
            dismiss();
        } catch (IllegalStateException | IllegalArgumentException e) {
            if (id != null) AccountShortcuts.remove(requireContext(), id);
            message(R.string.shortcut_create_failed);
        }
    }

    private void message(int resource) { Toast.makeText(requireContext(), resource, Toast.LENGTH_LONG).show(); }

    @Override public void onSaveInstanceState(@NonNull Bundle state) {
        super.onSaveInstanceState(state);
        if (name == null) return;
        state.putString("name", name.getText().toString());
        state.putInt("destination", destination.getSelectedItemPosition());
        state.putString("computer", restoredAddress != null ? restoredAddress : addresses.get(device.getSelectedItemPosition()));
        state.putInt("color", color.getSelectedItemPosition());
        state.putInt("symbol", symbol.getSelectedItemPosition());
        state.putString("emoji", emoji.getText().toString());
    }
}
