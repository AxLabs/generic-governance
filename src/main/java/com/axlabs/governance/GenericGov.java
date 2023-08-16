package com.axlabs.governance;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Contract;
import io.neow3j.devpack.Hash160;
import io.neow3j.devpack.Runtime;
import io.neow3j.devpack.Storage;
import io.neow3j.devpack.StorageContext;
import io.neow3j.devpack.StorageMap;
import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.annotations.Safe;
import io.neow3j.devpack.constants.CallFlags;
import io.neow3j.devpack.contracts.ContractManagement;
import io.neow3j.devpack.contracts.CryptoLib;
import io.neow3j.devpack.contracts.LedgerContract;
import io.neow3j.devpack.contracts.StdLib;

import static io.neow3j.devpack.Runtime.checkWitness;

@Permission(contract = "*", methods = "*")
@DisplayName("Generic Governance")
@ManifestExtra(key = "author", value = "AxLabs")
public class GenericGov {

    static StorageContext ctx = Storage.getStorageContext();
    static final StorageMap proposals = new StorageMap(ctx, "proposals");
    static final StorageMap members = new StorageMap(ctx, "members");

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (!update) {
            int idx = new LedgerContract().currentIndex();
            for (Hash160 m : (Hash160[]) data) {
                members.put(m.toByteString(), idx);
            }
        }
    }

    public static ByteString createProposal(Hash160 proposer, Intent[] intents, String discUrl) {
        assert Runtime.checkWitness(proposer);
        ByteString proposalHash = hashProposal(intents, discUrl);
        proposals.put(proposalHash, new StdLib().serialize(new Proposal(proposalHash, proposer)));
        return proposalHash;
    }

    public static ByteString hashProposal(Intent[] intents, String discUrl) {
        return new CryptoLib().sha256(new StdLib().serialize(intents).concat(discUrl));
    }

    public static void vote(ByteString proposalHash, boolean vote, Hash160 voter) {
        assert members.get(voter.toByteString()) != null && checkWitness(voter);
        Proposal proposal = (Proposal) new StdLib().deserialize(proposals.get(proposalHash));
        if (vote) {
            proposal.yesVotes++;
        } else {
            proposal.noVotes++;
        }
        proposals.put(proposalHash, new StdLib().serialize(proposal));
    }

    @Safe
    public static Proposal getProposal(ByteString proposalHash) {
        return (Proposal) new StdLib().deserialize(proposals.get(proposalHash));
    }

    public static Object[] execute(Intent[] intents, String discUrl) {
        ByteString proposalHash = hashProposal(intents, discUrl);
        Proposal proposal = (Proposal) new StdLib().deserialize(proposals.get(proposalHash));
        int voteCount = proposal.yesVotes + proposal.noVotes;
        assert proposal.yesVotes * 100 / voteCount >= 50;

        Object[] returnVals = new Object[intents.length];
        for (int i = 0; i < intents.length; i++) {
            Intent t = intents[i];
            returnVals[i] = Contract.call(t.targetContract, t.targetMethod, CallFlags.All, t.methodParams);
        }
        return returnVals;
    }

    public static void update(ByteString nef, String manifest) {
        assert Runtime.getCallingScriptHash() == Runtime.getExecutingScriptHash();
        new ContractManagement().update(nef, manifest);
    }

}
