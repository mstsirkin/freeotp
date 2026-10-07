package org.fedorahosted.freeotp.main;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.fedorahosted.freeotp.main.share.ShareRoute;

/** Per-account destinations; only startup senders compete for a destination. */
public final class AccountAutomation {
    private AccountAutomation() {}
    public static final class State {
        public final ShareRoute destination;
        public final boolean startup;
        public State(ShareRoute destination, boolean startup) {
            this.destination = destination; this.startup = startup;
        }
    }
    public static List<String> configure(Map<String, State> accounts, String edited,
            ShareRoute destination, boolean startup) {
        List<String> disabled = new ArrayList<>();
        accounts.put(edited, new State(destination, startup));
        if (startup && destination != ShareRoute.NONE) {
            for (Map.Entry<String, State> entry : accounts.entrySet()) {
                State state = entry.getValue();
                if (!entry.getKey().equals(edited) && state.startup && state.destination == destination) {
                    entry.setValue(new State(state.destination, false)); disabled.add(entry.getKey());
                }
            }
        }
        return disabled;
    }
}
