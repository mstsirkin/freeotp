import org.fedorahosted.freeotp.keyboard.KeyboardCodec;
import org.fedorahosted.freeotp.keyboard.KeyboardReports;
import java.util.Arrays;
public class KeyboardReportsTest {
    public static void main(String[] args) {
        byte[] down=KeyboardCodec.report('A');
        byte[] reply=KeyboardReports.get(1,0,0,down,(byte)2);
        if(reply.length!=8 || !Arrays.equals(reply,down)) throw new AssertionError("Input query must preserve held key");
        reply[2]=0;
        if(down[2]!=4) throw new AssertionError("Input query must not alter held key");
        byte[] output=KeyboardReports.get(2,0,8,down,(byte)2);
        if(output.length!=1 || output[0]!=2) throw new AssertionError("LED query must be exactly one byte, even with larger requested buffer");
        if(KeyboardReports.get(1,0,4,down,(byte)0).length!=4) throw new AssertionError("Host buffer limit");
        for(int[] bad:new int[][]{{3,0},{1,1}}) {
            try { KeyboardReports.get(bad[0],bad[1],0,down,(byte)0); throw new AssertionError("Invalid request accepted"); }
            catch(IllegalArgumentException expected) {}
        }
        System.out.println("PASS: host input/LED queries, held-key state, buffer limit, invalid requests");
    }
}
