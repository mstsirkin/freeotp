package org.fedorahosted.freeotp.main.share;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Toast;
import androidx.fragment.app.FragmentActivity;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.keyboard.KeyboardActivity;

public final class ShareActions {
    private ShareActions() {}
    public static void share(FragmentActivity activity,String code) {
        ShareRoute route=ShareRoute.choose(SharingSettings.clipboardEnabled(activity),SharingSettings.jellingEnabled(activity),SharingSettings.keyboardEnabled(activity));
        switch(route) {
            case CLIPBOARD:
                activity.getSystemService(ClipboardManager.class).setPrimaryClip(ClipData.newPlainText(null,code));
                Toast.makeText(activity,R.string.share_code_copied,Toast.LENGTH_SHORT).show(); return;
            case KEYBOARD:
                activity.startActivity(new Intent(activity,KeyboardActivity.class).setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT,code)); return;
            case NONE:
                Toast.makeText(activity,R.string.share_none_enabled,Toast.LENGTH_LONG).show();
                activity.startActivity(new Intent(activity,SharingSettingsActivity.class)); return;
            default:
                Bundle arguments=new Bundle(); arguments.putString(ShareFragment.CODE_ID,code);
                arguments.putBoolean(ShareFragment.DIRECT_JELLING,route==ShareRoute.JELLING);
                ShareFragment fragment=new ShareFragment(); fragment.setArguments(arguments);
                fragment.show(activity.getSupportFragmentManager(),"share");
        }
    }
}
