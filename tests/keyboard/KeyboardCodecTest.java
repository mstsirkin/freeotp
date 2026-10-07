import org.fedorahosted.freeotp.keyboard.KeyboardCodec;
public class KeyboardCodecTest {
 public static void main(String[] args) {
  for(char c=32;c<127;c++) { byte[] r=KeyboardCodec.report(c); if(r.length!=8 || r[2]==0) throw new AssertionError("Missing ASCII "+c); }
  check('a',0,4); check('Z',2,29); check('0',0,39); check('!',2,30); check('?',2,56); check('\n',0,40); check('\t',0,43); check('"',2,52);
  if(!KeyboardCodec.normalize("a\r\nb\rc").equals("a\nb\nc")) throw new AssertionError("Newlines");
  for(String bad:new String[]{"שלום","€","😀","a\u0000"}) { try { KeyboardCodec.validate(bad); throw new AssertionError("Unsupported input accepted"); } catch(IllegalArgumentException expected) {} }
  System.out.println("PASS: printable ASCII, modifiers, line endings, rejection of unsupported Unicode/control input");
 }
 static void check(char c,int mod,int usage) { byte[] r=KeyboardCodec.report(c); if(r[0]!=mod || r[2]!=usage) throw new AssertionError("Incorrect usage for "+c); }
}
