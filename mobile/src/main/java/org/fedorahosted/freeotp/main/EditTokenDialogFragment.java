/*
 * FreeOTP
 *
 * Authors: Justin Stephenson <jstephen@redhat.com>
 *
 * Copyright (C) 2022  Justin Stephenson, Red Hat
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.fedorahosted.freeotp.main;

import android.content.DialogInterface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.TokenIcon;
import org.fedorahosted.freeotp.databinding.FragmentEditBinding;
import org.fedorahosted.freeotp.icons.FaviconDiscovery;
import org.fedorahosted.freeotp.icons.FaviconLoader;

import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

public class EditTokenDialogFragment extends DialogFragment {
    private FragmentEditBinding mBinding;
    private String icon;
    private String website = "";
    private ExecutorService executor;
    private Future<?> download;
    private AlertDialog iconDialog;
    private int request;

    static EditTokenDialogFragment newInstance(String account, String issuer, int imageId, int color,
                                               String uuid, String original, String override) {
        EditTokenDialogFragment f = new EditTokenDialogFragment();
        Bundle args = new Bundle();
        args.putString("account", account);
        args.putString("issuer", issuer);
        args.putInt("image_id", imageId);
        args.putInt("color", color);
        args.putString("uuid", uuid);
        args.putString("original", original);
        args.putString("icon", override);
        f.setArguments(args);
        return f;
    }

    @Nullable @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        mBinding = FragmentEditBinding.inflate(inflater);
        Bundle args = requireArguments();
        Bundle state = savedInstanceState == null ? args : savedInstanceState;
        icon = state.getString("icon");
        website = state.getString("website", "");
        executor = Executors.newSingleThreadExecutor();
        mBinding.account.setText(state.getString("account"));
        mBinding.issuer.setText(state.getString("issuer"));
        mBinding.image.setBackgroundColor(args.getInt("color"));
        preview();
        mBinding.image.setOnClickListener(v -> chooseIcon());
        mBinding.save.setOnClickListener(v -> {
            Bundle result = new Bundle();
            result.putString("uuid", args.getString("uuid"));
            result.putString("account", mBinding.account.getText().toString());
            result.putString("issuer", mBinding.issuer.getText().toString());
            result.putString("icon", icon);
            getParentFragmentManager().setFragmentResult("requestKey", result);
            dismiss();
        });
        return mBinding.getRoot();
    }

    private void preview() {
        TokenIcon.load(mBinding.image, requireArguments().getInt("image_id"),
                icon == null ? requireArguments().getString("original") : icon);
    }

    private void chooseIcon() {
        boolean hasOriginal = requireArguments().getString("original") != null;
        int[] labels = hasOriginal
                ? new int[]{R.string.icon_website,
                    R.string.icon_original, R.string.icon_default}
                : new int[]{R.string.icon_website, R.string.icon_default};
        String[] choices = new String[labels.length];
        for (int i = 0; i < labels.length; i++) choices[i] = getString(labels[i]);
        iconDialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.icon_edit)
                .setItems(choices, (dialog, which) -> {
                    if (which == 0) { websiteDialog(); return; }
                    icon = hasOriginal && which == 1 ? null : "";
                    preview();
                }).show();
    }

    private void websiteDialog() {
        LinearLayout body = new LinearLayout(requireContext());
        body.setOrientation(LinearLayout.VERTICAL);
        int padding = (int) (24 * getResources().getDisplayMetrics().density);
        body.setPadding(padding, padding / 2, padding, 0);
        TextView help = new TextView(requireContext());
        help.setText(R.string.icon_website_help);
        body.addView(help);
        EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI);
        input.setSingleLine(true);
        input.setHint(R.string.icon_url_hint);
        input.setContentDescription(getString(R.string.icon_website));
        input.setText(website);
        body.addView(input);
        TextView status = new TextView(requireContext());
        status.setAccessibilityLiveRegion(View.ACCESSIBILITY_LIVE_REGION_POLITE);
        body.addView(status);
        AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.icon_website).setView(body)
                .setNegativeButton(R.string.cancel, null)
                .setPositiveButton(R.string.icon_load, null).create();
        iconDialog = dialog;
        dialog.setOnShowListener(ignored -> dialog.getButton(DialogInterface.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    website = input.getText().toString().trim();
                    try { FaviconDiscovery.website(website); }
                    catch (IllegalArgumentException e) {
                        input.setError(getString(R.string.icon_invalid_url));
                        return;
                    }
                    input.setError(null);
                    input.setEnabled(false);
                    dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(false);
                    status.setText(R.string.icon_loading);
                    mBinding.save.setEnabled(false);
                    int current = ++request;
                    String url = website;
                    Handler handler = new Handler(Looper.getMainLooper());
                    download = executor.submit(() -> {
                        String loaded;
                        try { loaded = FaviconLoader.load(url); }
                        catch (IOException | IllegalArgumentException e) { loaded = null; }
                        String result = loaded;
                        handler.post(() -> {
                            if (mBinding == null || current != request || !dialog.isShowing()) return;
                            mBinding.save.setEnabled(true);
                            if (result != null) {
                                icon = result;
                                preview();
                                dialog.dismiss();
                            } else {
                                input.setEnabled(true);
                                dialog.getButton(DialogInterface.BUTTON_POSITIVE).setEnabled(true);
                                status.setText(R.string.icon_load_failed);
                            }
                        });
                    });
                }));
        dialog.show();
        dialog.setOnDismissListener(ignored -> {
            request++;
            if (download != null) download.cancel(true);
            if (mBinding != null) mBinding.save.setEnabled(true);
        });
    }

    @Override public void onSaveInstanceState(@NonNull Bundle out) {
        super.onSaveInstanceState(out);
        out.putString("icon", icon);
        out.putString("website", website);
        if (mBinding != null) {
            out.putString("account", mBinding.account.getText().toString());
            out.putString("issuer", mBinding.issuer.getText().toString());
        }
    }

    @Override public void onDestroyView() {
        request++;
        if (download != null) download.cancel(true);
        if (executor != null) executor.shutdownNow();
        if (iconDialog != null) iconDialog.dismiss();
        if (mBinding != null) Picasso.get().cancelRequest(mBinding.image);
        mBinding = null;
        super.onDestroyView();
    }
}
