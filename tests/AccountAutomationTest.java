import java.util.*;
import org.fedorahosted.freeotp.main.AccountAutomation;
import org.fedorahosted.freeotp.main.share.ShareRoute;
public final class AccountAutomationTest {
    private static void require(boolean condition) { if (!condition) throw new AssertionError(); }
    public static void main(String[] args) {
        Map<String, AccountAutomation.State> accounts = new LinkedHashMap<>();
        for (String id : Arrays.asList("a", "b", "c", "d")) accounts.put(id, new AccountAutomation.State(ShareRoute.NONE, false));
        AccountAutomation.configure(accounts,"a",ShareRoute.CLIPBOARD,true);
        require(AccountAutomation.configure(accounts,"b",ShareRoute.CLIPBOARD,false).isEmpty());
        require(accounts.get("a").startup && !accounts.get("b").startup);
        require(AccountAutomation.configure(accounts,"b",ShareRoute.CLIPBOARD,true).equals(Arrays.asList("a")));
        require(accounts.get("b").startup && !accounts.get("a").startup);
        require(accounts.get("a").destination == ShareRoute.CLIPBOARD);
        AccountAutomation.configure(accounts,"c",ShareRoute.KEYBOARD,true);
        require(accounts.get("b").startup && accounts.get("c").startup);
        require(AccountAutomation.configure(accounts,"c",ShareRoute.CLIPBOARD,true).equals(Arrays.asList("b")));
        require(accounts.get("c").startup && !accounts.get("b").startup);
        AccountAutomation.configure(accounts,"a",ShareRoute.NONE,true);
        AccountAutomation.configure(accounts,"d",ShareRoute.NONE,true);
        require(accounts.get("a").startup && accounts.get("d").startup && accounts.get("c").startup);
        AccountAutomation.configure(accounts,"b",ShareRoute.JELLING,true);
        require(accounts.get("c").startup && accounts.get("b").startup);
        AccountAutomation.configure(accounts,"b",ShareRoute.NONE,false);
        require(!accounts.get("b").startup && accounts.get("b").destination == ShareRoute.NONE);
        System.out.println("PASS: shared manual destinations, startup exclusivity, latest change wins, route changes, preserved destinations and no-destination accounts");
    }
}
