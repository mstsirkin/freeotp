/* SPDX-License-Identifier: Apache-2.0 */
package org.fedorahosted.freeotp.icons;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/** Bounds-checked ICO directory and uncompressed Windows DIB decoding. */
public final class IcoDecoder {
    public static final class Frame {
        public final int width, height, offset, length;
        Frame(int w, int h, int o, int n) { width = w; height = h; offset = o; length = n; }
    }

    private static int u16(byte[] b, int o) { return (b[o] & 255) | ((b[o + 1] & 255) << 8); }
    private static long u32(byte[] b, int o) {
        return u16(b, o) | ((long) u16(b, o + 2) << 16);
    }

    public static List<Frame> frames(byte[] b) {
        List<Frame> result = new ArrayList<>();
        if (b.length < 6 || u16(b, 0) != 0 || u16(b, 2) != 1) return result;
        int count = u16(b, 4);
        if (count > 256 || 6L + count * 16L > b.length) return result;
        for (int i = 0; i < count; i++) {
            int p = 6 + i * 16;
            int w = b[p] & 255, h = b[p + 1] & 255;
            long n = u32(b, p + 8), o = u32(b, p + 12);
            if (n > 0 && o >= 6 + count * 16 && o + n <= b.length)
                result.add(new Frame(w == 0 ? 256 : w, h == 0 ? 256 : h, (int) o, (int) n));
        }
        result.sort(Comparator.comparingInt((Frame f) -> f.width * f.height).reversed());
        return result;
    }

    public static int[] pixels(byte[] b, Frame frame) {
        int start = frame.offset, end = start + frame.length;
        if (frame.length < 40 || u32(b, start) < 40 || u32(b, start) > frame.length) return null;
        int w = frame.width, h = frame.height, bits = u16(b, start + 14);
        if (u32(b, start + 4) != w || u32(b, start + 8) != h * 2L
                || u16(b, start + 12) != 1 || u32(b, start + 16) != 0
                || (bits != 1 && bits != 4 && bits != 8 && bits != 24 && bits != 32)) return null;
        int palette = start + (int) u32(b, start);
        long colors = bits <= 8 ? u32(b, start + 32) : 0;
        if (bits <= 8 && colors == 0) colors = 1 << bits;
        if (colors > 256 || palette + colors * 4L > end) return null;
        int data = palette + (int) colors * 4;
        int stride = ((w * bits + 31) / 32) * 4, maskStride = ((w + 31) / 32) * 4;
        if (data + (long) stride * h > end) return null;
        int mask = data + stride * h;
        boolean hasMask = mask + (long) maskStride * h <= end;
        int[] pixels = new int[w * h];
        boolean hasAlpha = false;
        for (int y = 0; y < h; y++) {
            int row = data + (h - 1 - y) * stride;
            for (int x = 0; x < w; x++) {
                int p, alpha = 255;
                if (bits <= 8) {
                    int index = ((b[row + x * bits / 8] & 255) >> (8 - bits - x * bits % 8)) & ((1 << bits) - 1);
                    if (index >= colors) return null;
                    p = palette + index * 4;
                } else {
                    p = row + x * (bits / 8);
                    if (bits == 32) { alpha = b[p + 3] & 255; hasAlpha |= alpha != 0; }
                }
                pixels[y * w + x] = (alpha << 24) | ((b[p + 2] & 255) << 16)
                        | ((b[p + 1] & 255) << 8) | (b[p] & 255);
            }
        }
        if (bits != 32 || !hasAlpha) {
            for (int y = 0; y < h; y++) for (int x = 0; x < w; x++) {
                boolean transparent = hasMask && ((b[mask + (h - 1 - y) * maskStride + x / 8] >> (7 - x % 8)) & 1) != 0;
                pixels[y * w + x] = (pixels[y * w + x] & 0xffffff) | (transparent ? 0 : 0xff000000);
            }
        }
        return pixels;
    }
}
