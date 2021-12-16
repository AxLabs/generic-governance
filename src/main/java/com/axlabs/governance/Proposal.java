package com.axlabs.governance;

import io.neow3j.devpack.ByteString;
import io.neow3j.devpack.Hash160;

public class Proposal {

    public ByteString proposalHash;
    public Hash160 proposer;

    public int yesVotes;
    public int noVotes;

    public Proposal(ByteString proposalHash, Hash160 proposer) {
        this.proposalHash = proposalHash;
        this.proposer = proposer;
        yesVotes = 0;
        noVotes = 0;
    }
}
