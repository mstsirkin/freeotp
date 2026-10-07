import org.fedorahosted.freeotp.main.share.ShareRoute;
public class ShareRouteTest {
    public static void main(String[] args) {
        check(false,false,false,ShareRoute.NONE);
        check(true,false,false,ShareRoute.CLIPBOARD);
        check(false,true,false,ShareRoute.JELLING);
        check(false,false,true,ShareRoute.KEYBOARD);
        check(true,true,false,ShareRoute.CHOOSER);
        check(true,false,true,ShareRoute.CHOOSER);
        check(false,true,true,ShareRoute.CHOOSER);
        check(true,true,true,ShareRoute.CHOOSER);
        System.out.println("PASS: all eight sharing configurations, direct routes, disabled methods and chooser");
    }
    private static void check(boolean c,boolean j,boolean k,ShareRoute expected) {
        if(ShareRoute.choose(c,j,k)!=expected) throw new AssertionError("Wrong route for "+c+","+j+","+k);
    }
}
