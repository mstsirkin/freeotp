/* SPDX-License-Identifier: Apache-2.0 */
package org.fedorahosted.freeotp.icons;

import android.graphics.Bitmap;
import android.net.Uri;
import android.util.Base64;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.fedorahosted.freeotp.Token;
import org.fedorahosted.freeotp.TokenIcon;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.ByteArrayOutputStream;

import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class FaviconAccountTest {
    @Test public void customIconSurvivesBackupMetadataWithoutReplacingOriginal() throws Exception {
        String original = "https://example.com/original.png";
        Token token = Token.parseUnsafe(Uri.parse("otpauth://totp/Work:test?secret=GEZDGNBVGY3TQOJQ"
                + "&issuer=Work&image=" + Uri.encode(original))).second;
        Bitmap bitmap = Bitmap.createBitmap(2, 2, Bitmap.Config.ARGB_8888);
        bitmap.eraseColor(0xff123456);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
        String custom = TokenIcon.PNG_PREFIX + Base64.encodeToString(png.toByteArray(), Base64.NO_WRAP);
        token.setIconOverride(custom);

        // This JSON is the metadata carried by encrypted backups.
        Token restored = Token.deserialize(token.serialize());
        assertEquals(original, restored.getImage());
        assertEquals(custom, restored.getDisplayImage());
        assertEquals(0xff123456, TokenIcon.embeddedImage(restored.getDisplayImage()).getPixel(0, 0));

        restored.setIconOverride("");
        restored = Token.deserialize(restored.serialize());
        assertEquals("", restored.getDisplayImage());
        assertEquals(original, restored.getImage());
        restored.setIconOverride(null);
        assertEquals(original, Token.deserialize(restored.serialize()).getDisplayImage());
    }

    @Test public void pngDecodesAndMalformedIconIsRejected() {
        Bitmap bitmap = Bitmap.createBitmap(16, 16, Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, png);
        assertEquals(16, FaviconLoader.decode(png.toByteArray()).getWidth());
        assertNull(FaviconLoader.decode(new byte[]{0, 1, 2, 3}));
        assertNull(TokenIcon.embeddedImage(TokenIcon.PNG_PREFIX + "bad data"));
    }
}
