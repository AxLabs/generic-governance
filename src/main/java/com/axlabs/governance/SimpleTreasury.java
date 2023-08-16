package com.axlabs.governance;

import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.OnNEP17Payment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.events.Event3Args;

@Permission(contract = "*", methods = "*")
public class SimpleTreasury {

    static final String OWNER_KEY = "owner";

    static final StorageContext ctx = Storage.getStorageContext();

    @DisplayName("PaymentReceived")
    static Event3Args<Hash160, Integer, Hash160> received;

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            Storage.put(ctx, OWNER_KEY, (Hash160) data);
        }
    }

    @OnNEP17Payment
    public static void onNep17Payment(Hash160 sender, int amount, Object data) {
        received.fire(sender, amount, Runtime.getCallingScriptHash());
    }

    public static boolean releaseFunds(Hash160 token, Hash160 receiver, int amount) {
        assert Runtime.getCallingScriptHash().toByteString() == Storage.get(ctx, OWNER_KEY);
        Object[] params = new Object[] {Runtime.getExecutingScriptHash(), receiver, amount, new Object[] {}};
        return (boolean) Contract.call(token, "transfer", CallFlags.All, params);
    }
}
