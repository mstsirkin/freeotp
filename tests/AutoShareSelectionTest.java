import org.fedorahosted.freeotp.main.AutoShareSelection;

public final class AutoShareSelectionTest {
    public static void main(String[] args) {
        String selected = AutoShareSelection.toggle(null, "account-a");
        require(AutoShareSelection.shouldShare(true, selected, "account-a"), "selected account shares");
        require(!AutoShareSelection.shouldShare(true, selected, "account-b"), "other account stays manual");
        selected = AutoShareSelection.toggle(selected, "account-b");
        require(!AutoShareSelection.shouldShare(true, selected, "account-a"), "previous account is deselected");
        require(AutoShareSelection.shouldShare(true, selected, "account-b"), "new account shares");
        require(!AutoShareSelection.shouldShare(false, selected, "account-b"), "global switch stops sharing");
        require("account-b".equals(AutoShareSelection.remove(selected, "account-a")), "unrelated deletion preserves selection");
        require(AutoShareSelection.remove(selected, "account-b") == null, "selected deletion clears selection");
        selected = AutoShareSelection.toggle(selected, "account-b");
        require(selected == null && !AutoShareSelection.shouldShare(true, selected, "account-b"), "second toggle disables sharing");
        System.out.println("PASS: exclusive selection, switching, deselection, global disable and deletion");
    }
    private static void require(boolean value, String message) {
        if (!value) throw new AssertionError(message);
    }
}
