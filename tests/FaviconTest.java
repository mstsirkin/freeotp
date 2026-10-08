import org.fedorahosted.freeotp.icons.FaviconDiscovery;
import org.fedorahosted.freeotp.icons.IcoDecoder;

import java.net.URI;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.util.Arrays;
import java.util.List;

public class FaviconTest {
    private static void check(boolean value) { if (!value) throw new AssertionError(); }

    private static byte[] ico(int bits, int pixel, int mask, int alpha) {
        ByteBuffer b = ByteBuffer.allocate(70).order(ByteOrder.LITTLE_ENDIAN);
        b.putShort((short) 0).putShort((short) 1).putShort((short) 1);
        b.put((byte) 1).put((byte) 1).put((byte) 0).put((byte) 0);
        b.putShort((short) 1).putShort((short) bits).putInt(48).putInt(22);
        b.putInt(40).putInt(1).putInt(2).putShort((short) 1).putShort((short) bits);
        b.position(62);
        b.put((byte) pixel).put((byte) 0x22).put((byte) 0x33).put((byte) alpha);
        b.put((byte) mask);
        return b.array();
    }

    public static void main(String[] args) {
        URI page = FaviconDiscovery.website(" example.com/login ");
        check(page.equals(URI.create("https://example.com/login")));
        for (String bad : new String[]{"", "file:///etc/passwd", "https://user:pass@example.com", "https://"}) {
            try { FaviconDiscovery.website(bad); throw new AssertionError(bad); }
            catch (IllegalArgumentException expected) { }
        }
        List<URI> icons = FaviconDiscovery.candidates(page,
                "<!-- <link rel=icon href=bad.png> -->"
                + "<LINK HREF='../icon.png?a=1&amp;b=2' REL='shortcut icon'>"
                + "<link rel=apple-touch-icon href=//cdn.example.com/apple.png>"
                + "<link rel=stylesheet href=style.css>"
                + "<link rel=icon href='data:image/png;base64,aaa'>"
                + "<link rel=icon href='https://user:pass@example.com/private'>"
                + "<base href='/assets/site/'>");
        check(icons.equals(Arrays.asList(URI.create("https://example.com/assets/icon.png?a=1&b=2"),
                URI.create("https://cdn.example.com/apple.png"), URI.create("https://example.com/favicon.ico"))));
        check(FaviconDiscovery.candidates(page, "<link rel=mask-icon href=icon.svg>").size() == 1);
        byte[] bytes = ico(32, 0x11, 0, 128);
        IcoDecoder.Frame frame = IcoDecoder.frames(bytes).get(0);
        check(IcoDecoder.pixels(bytes, frame)[0] == 0x80332211);
        bytes = ico(32, 0x11, 0, 0);
        check(IcoDecoder.pixels(bytes, IcoDecoder.frames(bytes).get(0))[0] == 0xff332211);
        bytes = ico(24, 0x11, 128, 0);
        check(IcoDecoder.pixels(bytes, IcoDecoder.frames(bytes).get(0))[0] == 0x00332211);
        for (int i = 0; i < bytes.length; i++) check(IcoDecoder.frames(Arrays.copyOf(bytes, i)).isEmpty());
        bytes[14] = (byte) 255; // oversized frame length
        check(IcoDecoder.frames(bytes).isEmpty());
        bytes = ico(32, 0x11, 0, 128);
        bytes[30] = 127; // corrupt DIB height
        check(IcoDecoder.pixels(bytes, IcoDecoder.frames(bytes).get(0)) == null);
        System.out.println("Favicon discovery and ICO regression tests passed");
    }
}
