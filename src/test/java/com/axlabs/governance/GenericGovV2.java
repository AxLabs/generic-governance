package com.axlabs.governance;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.LedgerContract;

import static io.neow3j.devpack.Runtime.checkWitness;

@Permission(contract = "*", methods = "*")
@DisplayName("Generic Governance")
@ManifestExtra(key = "author", value = "AxLabs")
public class GenericGovV2 {

    static StorageContext ctx = Storage.getStorageContext();
    static final StorageMap proposals = ctx.createMap(0);
    static final StorageMap members = ctx.createMap(1);

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            int idx = LedgerContract.currentIndex();
            for (Hash160 m : (Hash160[]) data) {
                members.put(m.toByteString(), idx);
            }
        }
    }

    public static ByteString createProposal(Hash160 proposer, Intent[] intents,
            String discUrl) throws Exception {
        throw new Exception("Not allowed anymore");
    }

    public static void vote(ByteString proposalHash, boolean vote, Hash160 voter) throws Exception {
        throw new Exception("Not allowed anymore");
    }

    public static Object[] execute(Intent[] intents, String discUrl) throws Exception {
        throw new Exception("Not allowed anymore");
    }

    public static void update(ByteString nef, String manifest) {
        assert Runtime.getCallingScriptHash() == Runtime.getExecutingScriptHash() :
                "Not authorised";
        ContractManagement.update(nef, manifest);
    }

}