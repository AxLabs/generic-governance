package com.axlabs.governance;

import io.neow3j.devpack.annotations.DisplayName;
import io.neow3j.devpack.annotations.ManifestExtra;
import io.neow3j.devpack.annotations.OnDeployment;
import io.neow3j.devpack.annotations.Permission;
import io.neow3j.devpack.events.Event1Arg;

@Permission(contract = "*", methods = "*")
@DisplayName("Simple Governance")
@ManifestExtra(key = "author", value = "AxLabs")
public class SimpleGovUpdated {

    static Event1Arg<String> updated;

    @OnDeployment
    public static void deploy(Object data, boolean update) {
        if (update) {
            updated.fire("Updated the contract.");
        }
    }

}