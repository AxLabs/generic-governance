package com.axlabs.governance;

import io.neow3j.devpack.Hash160;

public class Intent {

    public Hash160 targetContract;
    public String targetMethod;
    public Object[] methodParams;

    public Intent(Hash160 targetContract, String targetMethod, Object[] methodParams) {
        this.targetContract = targetContract;
        this.targetMethod = targetMethod;
        this.methodParams = methodParams;
    }
}
