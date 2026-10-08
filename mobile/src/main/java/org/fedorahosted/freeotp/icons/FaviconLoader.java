/* SPDX-License-Identifier: Apache-2.0 */
package org.fedorahosted.freeotp.icons;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;

import org.fedorahosted.freeotp.TokenIcon;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.List;

/** Fetch directly from the website, never through an external favicon service. */
public final class FaviconLoader {
    private static final int MAX_DOWNLOAD = 2 * 1024 * 1024;
    private static final int MAX_ICON = 128;

    private static final class Response {
        URI uri;
        byte[] bytes;
        String contentType;
    }

    private static Response fetch(URI uri, long deadline) throws IOException {
        for (int redirect = 0; redirect < 6; redirect++) {
            if (Thread.currentThread().isInterrupted() || System.nanoTime() > deadline)
                throw new IOException("Cancelled or timed out");
            HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
            int timeout = (int) Math.max(1, Math.min(6000, (deadline - System.nanoTime()) / 1_000_000));
            connection.setConnectTimeout(timeout);
            connection.setReadTimeout(timeout);
            connection.setInstanceFollowRedirects(false);
            connection.setRequestProperty("User-Agent", "FreeOTP++ favicon loader");
            try {
                int status = connection.getResponseCode();
                if (status >= 300 && status <= 399) {
                    String location = connection.getHeaderField("Location");
                    if (location == null) throw new IOException("Missing redirect");
                    URI target = uri.resolve(location);
                    if (!FaviconDiscovery.isWeb(target) || target.getUserInfo() != null
                            || ("https".equalsIgnoreCase(uri.getScheme()) && !"https".equalsIgnoreCase(target.getScheme())))
                        throw new IOException("Unsafe redirect");
                    uri = target;
                    continue;
                }
                if (status != 200 || connection.getContentLength() > MAX_DOWNLOAD)
                    throw new IOException("Website returned no usable icon");
                ByteArrayOutputStream output = new ByteArrayOutputStream();
                try (InputStream input = connection.getInputStream()) {
                    byte[] buffer = new byte[8192];
                    int size;
                    while ((size = input.read(buffer)) != -1) {
                        if (output.size() + size > MAX_DOWNLOAD || Thread.currentThread().isInterrupted()
                                || System.nanoTime() > deadline) throw new IOException("Download limit reached");
                        output.write(buffer, 0, size);
                    }
                }
                Response response = new Response();
                response.uri = uri;
                response.bytes = output.toByteArray();
                response.contentType = connection.getContentType();
                return response;
            } finally { connection.disconnect(); }
        }
        throw new IOException("Too many redirects");
    }

    private static Bitmap raster(byte[] bytes, int offset, int length) {
        BitmapFactory.Options bounds = new BitmapFactory.Options();
        bounds.inJustDecodeBounds = true;
        BitmapFactory.decodeByteArray(bytes, offset, length, bounds);
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0
                || bounds.outWidth > 4096 || bounds.outHeight > 4096) return null;
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inSampleSize = 1;
        while (Math.max(bounds.outWidth, bounds.outHeight) / options.inSampleSize > 256)
            options.inSampleSize *= 2;
        return BitmapFactory.decodeByteArray(bytes, offset, length, options);
    }

    public static Bitmap decode(byte[] bytes) {
        List<IcoDecoder.Frame> frames = IcoDecoder.frames(bytes);
        for (IcoDecoder.Frame frame : frames) {
            Bitmap bitmap = raster(bytes, frame.offset, frame.length);
            if (bitmap != null) return bitmap;
            int[] pixels = IcoDecoder.pixels(bytes, frame);
            if (pixels != null) return Bitmap.createBitmap(pixels, frame.width, frame.height, Bitmap.Config.ARGB_8888);
        }
        return raster(bytes, 0, bytes.length);
    }

    public static String load(String website) throws IOException {
        URI uri = FaviconDiscovery.website(website);
        long deadline = System.nanoTime() + 30_000_000_000L;
        Response page = null;
        try { page = fetch(uri, deadline); } catch (IOException ignored) { }
        Bitmap icon = page != null && page.contentType != null && page.contentType.startsWith("image/")
                ? decode(page.bytes) : null;
        List<URI> candidates = FaviconDiscovery.candidates(page == null ? uri : page.uri,
                page == null ? "" : new String(page.bytes, StandardCharsets.UTF_8));
        for (URI candidate : candidates) {
            if (icon != null) break;
            try { icon = decode(fetch(candidate, deadline).bytes); } catch (IOException ignored) { }
        }
        if (icon == null) throw new IOException("No supported favicon found");
        int side = Math.max(icon.getWidth(), icon.getHeight());
        if (side > MAX_ICON) {
            Bitmap scaled = Bitmap.createScaledBitmap(icon, Math.max(1, icon.getWidth() * MAX_ICON / side),
                    Math.max(1, icon.getHeight() * MAX_ICON / side), true);
            icon.recycle();
            icon = scaled;
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        icon.compress(Bitmap.CompressFormat.PNG, 100, output);
        icon.recycle();
        return TokenIcon.PNG_PREFIX + Base64.encodeToString(output.toByteArray(), Base64.NO_WRAP);
    }
}
