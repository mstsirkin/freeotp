/*
 * FreeOTP
 *
 * Authors: Nathaniel McCallum <npmccallum@redhat.com>
 *
 * Copyright (C) 2018  Nathaniel McCallum, Red Hat
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

import android.animation.ObjectAnimator;
import android.os.Handler;
import android.text.BidiFormatter;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewPropertyAnimator;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.DecelerateInterpolator;
import android.view.animation.LinearInterpolator;
import android.widget.ImageButton;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import org.fedorahosted.freeotp.main.share.ShareRoute;
import org.fedorahosted.freeotp.Code;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.Token;

import androidx.recyclerview.widget.RecyclerView;

class ViewHolder extends RecyclerView.ViewHolder {
    interface EventListener {
        boolean onSelectionToggled(ViewHolder holder);
        void onActivated(ViewHolder holder);
        void onShare(ViewHolder holder, String code);
        boolean onStartupGenerationToggled(ViewHolder holder);
        void onDestinationToggled(ViewHolder holder, ShareRoute route);
    }
    private static final String LOGTAG = "Adapter";

    private EventListener mEventListener;
    private ObjectAnimator mCountdown;
    private Handler mHandler;
    private BidiFormatter mBidiFormatter;

    private ProgressBar mProgress;
    private ImageButton mShare;
    private ViewGroup mPassive;
    private ViewGroup mActive;
    private ViewGroup mIcons;
    private ViewGroup mIconsActive;
    private ImageView mCheck;
    private ImageView mCheckActive;
    private ImageView mImage;
    private ImageView mImageActive;
    private ImageView mLock;
    private TextView mIssuer;
    private TextView mLabel;
    private TextView mCode;
    private CheckBox mStartupGeneration, mKeyboard, mClipboard, mJelling;
    private View mAutomationControls;

    private View mView;

    private static final long SELECT_HALF_DURATION = 100; // milliseconds
    private static final long FADE_DURATION = 500; // milliseconds

    private final View.OnClickListener mSelectClick = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            v.animate()
                .setInterpolator(new AccelerateInterpolator())
                .setDuration(SELECT_HALF_DURATION)
                .rotationY(90)
                .withLayer()
                .withEndAction(() -> {
                    setSelected(mEventListener.onSelectionToggled(ViewHolder.this));
                    v.setRotationY(-90);
                    v.animate()
                        .setInterpolator(new DecelerateInterpolator())
                        .setDuration(SELECT_HALF_DURATION)
                        .rotationY(0)
                        .withLayer()
                        .start();
                }).start();
        }
    };

    private static void fade(final View view, final boolean in, long duration) {
        ViewPropertyAnimator animator = view.animate();

        // Just in case, cancel possible previous animation
        animator.cancel();

        if (duration > 0) {
            if (in) {
                view.setVisibility(View.VISIBLE);
            }

            animator.setInterpolator(new AccelerateDecelerateInterpolator())
                .setDuration(duration)
                .alpha(in ? 1f : 0f)
                .withLayer()
                .withEndAction(() -> {
                    if (!in) {
                        view.setVisibility(View.GONE);
                    }
                })
                .start();
        } else {
            // Not really a fade in this case, just do it right now
            if (in) {
                view.setAlpha(1f);
                view.setVisibility(View.VISIBLE);
            } else {
                view.setAlpha(0f);
                view.setVisibility(View.GONE);
            }
        }
    }

    private void displayCode(Code code, long animationDuration) {
        if (code == null)
            return;

        String text = mBidiFormatter.unicodeWrap(code.getCode());
        Log.i(LOGTAG, "displaying Code");
        /* Add spaces for readability */
        for (int segment : new int[] { 7, 5, 4, 3 }) {
            if (text.length() % segment != 0)
                continue;

            int count = text.length() / segment;
            if (count < 2)
                break;

            StringBuilder sb = new StringBuilder();

            sb.append(" ");
            for (int i = 0; i < text.length(); i += segment) {
                if (i % 13 > (i + segment) % 13)
                    sb.append("\n ");

                sb.append(text.substring(i, i + segment));
                sb.append(" ");
            }

            text = sb.toString();
            break;
        }

        mCountdown.cancel();
        mCode.setText(text);
        long timeLeft = code.timeLeft();
        mCountdown.setDuration(timeLeft);

        mHandler.removeCallbacksAndMessages(null);
        mHandler.postDelayed(() -> fadeOut(animationDuration), timeLeft);
        fadeIn(animationDuration);

        mCountdown.setIntValues(code.getProgress(mProgress.getMax()), 0);
        mCountdown.start();
    }

    void fadeOut(long duration) {
        /* Fade out */
        fade(mPassive, true, duration);
        fade(mActive, false, duration);
    }

    void fadeIn(long duration) {
        fade(mPassive, false, duration);
        fade(mActive, true, duration);
    }

    ViewHolder(View itemView, EventListener listener) {
        super(itemView);

        mBidiFormatter = BidiFormatter.getInstance();
        mEventListener = listener;
        mCountdown = new ObjectAnimator();
        mHandler = new Handler();
        mProgress = itemView.findViewById(R.id.progress_linear);
        mPassive = itemView.findViewById(R.id.passive);
        mActive = itemView.findViewById(R.id.active);
        mIssuer = itemView.findViewById(R.id.issuer);
        mLabel = itemView.findViewById(R.id.label);
        mIcons = itemView.findViewById(R.id.icons);
        mIconsActive = itemView.findViewById(R.id.icons_active);
        mCheck = itemView.findViewById(R.id.check);
        mCheckActive = itemView.findViewById(R.id.check_active);
        mImage = itemView.findViewById(R.id.image);
        mImageActive = itemView.findViewById(R.id.image_active);
        mLock = itemView.findViewById(R.id.lock);
        mShare = itemView.findViewById(R.id.share);
        mCode = itemView.findViewById(R.id.code);
        mView = itemView;
        mStartupGeneration = itemView.findViewById(R.id.startup_generation);
        mKeyboard = itemView.findViewById(R.id.destination_keyboard);
        mClipboard = itemView.findViewById(R.id.destination_clipboard);
        mJelling = itemView.findViewById(R.id.destination_jelling);
        mAutomationControls = itemView.findViewById(R.id.automation_controls);
        mKeyboard.setOnClickListener(v -> mEventListener.onDestinationToggled(this, ShareRoute.KEYBOARD));
        mClipboard.setOnClickListener(v -> mEventListener.onDestinationToggled(this, ShareRoute.CLIPBOARD));
        mJelling.setOnClickListener(v -> mEventListener.onDestinationToggled(this, ShareRoute.JELLING));
        mStartupGeneration.setOnClickListener(v -> mStartupGeneration.setChecked(
                mEventListener.onStartupGenerationToggled(ViewHolder.this)));


        mCountdown.setInterpolator(new LinearInterpolator());
        mCountdown.setPropertyName("progress");
        mCountdown.setTarget(mProgress);
        mCountdown.setAutoCancel(true);

        mIcons.setOnClickListener(mSelectClick);
        mIconsActive.setOnClickListener(mSelectClick);

        mShare.setOnClickListener(v -> {
            String code = mCode.getText().toString();
            mEventListener.onShare(this, code.replaceAll("\\s+", ""));
        });

        mView.setOnClickListener(v -> {
            mEventListener.onActivated(ViewHolder.this);
        });

        mView.setOnLongClickListener(v -> {
            mSelectClick.onClick(mIcons);
            return true;
        });
    }

    private void setSelected(boolean selected) {
        mImage.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
        mImageActive.setVisibility(selected ? View.INVISIBLE : View.VISIBLE);
        mCheck.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
        mCheckActive.setVisibility(selected ? View.VISIBLE : View.INVISIBLE);
    }

    void reset() {
        mHandler.removeCallbacksAndMessages(null);
        mCountdown.cancel();
        Picasso.get().cancelRequest(mImage);
        Picasso.get().cancelRequest(mImageActive);
    }

    void bind(Token token, int color, int image_id, String image_url, Code code, boolean selected, Token.Type type) {
        String issuer = token.getIssuer();
        if (issuer == null)
            issuer = mView.getResources().getString(R.string.unknown_issuer);

        mLock.setVisibility(token.getLock() ? View.VISIBLE : View.GONE);
        mIssuer.setText(issuer);
        mLabel.setText(token.getLabel());
        mImage.setBackgroundColor(color);
        mImageActive.setBackgroundColor(color);

        if (image_url == null || image_url.isEmpty()) {
            mImage.setImageResource(image_id);
            mImageActive.setImageResource(image_id);
        } else {
            Picasso.get().load(image_url).error(image_id).into(mImage);
            Picasso.get().load(image_url).error(image_id).into(mImageActive);
        }

        setSelected(selected);
        if (code != null) {
            displayCode(code, 0);
        } else {
            // As the view may have been recycled from a previous one with code displayed,
            // do not forget to reset this part to avoid issue
            fadeOut(0);
        }
    }

    void bindAutomation(boolean startup, ShareRoute destination) {
        mAutomationControls.setVisibility(View.VISIBLE);
        mStartupGeneration.setChecked(startup);
        mKeyboard.setChecked(destination == ShareRoute.KEYBOARD); mKeyboard.setEnabled(android.os.Build.VERSION.SDK_INT >= 28);
        mClipboard.setChecked(destination == ShareRoute.CLIPBOARD); mClipboard.setEnabled(true);
        mJelling.setChecked(destination == ShareRoute.JELLING); mJelling.setEnabled(true);
        CheckBox[] controls = {mStartupGeneration, mKeyboard, mClipboard, mJelling};
        int[] labels = {R.string.auto_generate_startup, R.string.share_settings_keyboard,
                R.string.share_settings_clipboard, R.string.share_settings_jelling};
        for (int i = 0; i < controls.length; i++) {
            String label = mView.getResources().getString(labels[i]);
            controls[i].setContentDescription(label + ": " + mIssuer.getText() + " " + mLabel.getText());
            if (android.os.Build.VERSION.SDK_INT >= 26) controls[i].setTooltipText(label);
        }
    }

    void displayCode(Code code) {
        displayCode(code, FADE_DURATION);
    }

    CharSequence getIssuer() {
        return mIssuer.getText();
    }

    CharSequence getLabel() {
        return mLabel.getText();
    }
}
