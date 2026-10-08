import org.fedorahosted.freeotp.Token;

/** Run against compiled app classes with Gson and android.jar on the classpath. */
public class TokenIconMetadataTest {
    private static void equal(Object a, Object b) {
        if (!java.util.Objects.equals(a, b)) throw new AssertionError(a + " != " + b);
    }

    public static void main(String[] args) {
        String original = "https://example.com/original.png";
        Token token = Token.deserialize("{\"type\":\"HOTP\",\"counter\":42,\"image\":\"" + original + "\"}");
        equal(original, token.getDisplayImage());
        String custom = "data:image/png;base64,fixture";
        token.setIconOverride(custom);
        token = Token.deserialize(token.serialize());
        equal(original, token.getImage());
        equal(custom, token.getDisplayImage());
        equal(42L, token.getCounter());
        token.setIconOverride("");
        token = Token.deserialize(token.serialize());
        equal("", token.getDisplayImage());
        equal(original, token.getImage());
        token.setIconOverride(null);
        token = Token.deserialize(token.serialize());
        equal(original, token.getDisplayImage());
        equal(null, token.getIconOverride());
        equal(42L, token.getCounter());
        System.out.println("Icon backup metadata, original restoration, and default reset passed");
    }
}
