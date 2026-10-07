package org.fedorahosted.freeotp.main.share;

import android.content.Context;
import android.content.Intent;
import org.fedorahosted.freeotp.R;
import org.fedorahosted.freeotp.keyboard.KeyboardActivity;

/** Launches a foreground keyboard session with only the displayed code. */
final class Keyboard extends Discoverable {
    Keyboard(Context context,DiscoveryCallback callback) {
        super(context,callback);
        Adapter.Item item=new Adapter.Item();
        item.setImage(R.drawable.ic_keyboard_destination);
        item.setTitle(context.getString(R.string.share_keyboard_title));
        item.setSubtitle(context.getString(R.string.share_keyboard_subtitle));
        item.setPriority(101);
        appear(item,(code,done)->{
            context.startActivity(new Intent(context,KeyboardActivity.class)
                .setAction(Intent.ACTION_SEND).putExtra(Intent.EXTRA_TEXT,code));
            done.onShareCompleted(true);
        });
    }
}
