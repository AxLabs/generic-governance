package com.axlabs.governance;

import com.fasterxml.jackson.databind.ObjectMapper;
import io.neow3j.compiler.CompilationUnit;
import io.neow3j.compiler.Compiler;
import io.neow3j.contract.NeoToken;
import io.neow3j.contract.SmartContract;
import io.neow3j.protocol.Neow3j;
import io.neow3j.protocol.core.stackitem.StackItem;
import io.neow3j.test.ContractTest;
import io.neow3j.test.ContractTestExtension;
import io.neow3j.test.DeployConfig;
import io.neow3j.test.DeployConfiguration;
import io.neow3j.test.DeployContext;
import io.neow3j.transaction.AccountSigner;
import io.neow3j.types.ContractParameter;
import io.neow3j.types.Hash160;
import io.neow3j.types.Hash256;
import io.neow3j.utils.Await;
import io.neow3j.wallet.Account;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;

import java.math.BigInteger;
import java.util.List;

import static io.neow3j.types.ContractParameter.any;
import static io.neow3j.types.ContractParameter.array;
import static io.neow3j.types.ContractParameter.bool;
import static io.neow3j.types.ContractParameter.byteArray;
import static io.neow3j.types.ContractParameter.hash160;
import static io.neow3j.types.ContractParameter.string;

@ContractTest(blockTime = 1,
        contracts = {GenericGov.class, GenericGovTreasury.class},
        configFile = "default.neo-express",
        batchFile = "init.batch"
)
public class GenericGovTest {

    private static final String ALICE = "NM7Aky765FG8NhhwtxjXRx7jEL1cnw7PBP";
    private static final String BOB = "NZpsgXn9VQQoLexpuXJsrX8BsoyAhKUyiX";
    private static final String CHARLIE = "NdbtgSku2qLuwsBBzLx3FLtmmMdm32Ktor";

    @RegisterExtension
    private static ContractTestExtension ext = new ContractTestExtension();

    private static Neow3j neow3j;
    private static SmartContract gov;
    private static SmartContract treasury;
    private static Account alice;
    private static Account bob;
    private static Account charlie;

    @DeployConfig(GenericGov.class)
    public static DeployConfiguration config1() {
        DeployConfiguration c = new DeployConfiguration();
        c.setDeployParam(array(
                Hash160.fromAddress(ALICE),
                Hash160.fromAddress(BOB)
        ));
        return c;
    }

    @DeployConfig(GenericGovTreasury.class)
    public static DeployConfiguration config2(DeployContext ctx) {
        DeployConfiguration c = new DeployConfiguration();
        SmartContract gov = ctx.getDeployedContract(GenericGov.class);
        c.setDeployParam(hash160(gov.getScriptHash()));
        return c;
    }

    @BeforeAll
    public static void setUp() throws Exception {
        neow3j = ext.getNeow3j();
        gov = ext.getDeployedContract(GenericGov.class);
        treasury = ext.getDeployedContract(GenericGovTreasury.class);
        alice = ext.getAccount(ALICE);
        bob = ext.getAccount(BOB);
        charlie = ext.getAccount(CHARLIE);
    }

    @Test
    public void basic_proposal_flow() throws Throwable {
        // Define proposal
        ContractParameter proposer = hash160(charlie);
        ContractParameter intent = array(
                NeoToken.SCRIPT_HASH,
                "transfer",
                array(alice.getScriptHash(), charlie.getScriptHash(), 1, any(null)));
        ContractParameter discUrl = string("https://give.charlie.neo/proposals/1");

        // Call createProposal
        Hash256 tx = gov.invokeFunction("createProposal", proposer, array(intent), discUrl)
                .signers(AccountSigner.calledByEntry(charlie))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);
        String proposalHash = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getExecutions().get(0).getStack().get(0).getHexString();
        System.out.println(proposalHash);

        ext.fastForward(5);

        // Vote on proposal
        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(alice))
                .signers(AccountSigner.calledByEntry(alice))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(bob))
                .signers(AccountSigner.calledByEntry(bob))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        List<StackItem> p = gov.invokeFunction("getProposal", byteArray(proposalHash))
                .callInvokeScript().getInvocationResult().getStack().get(0).getList();
        System.out.println(p);

        ext.fastForward(5);

        // Execute proposal
        tx = gov.invokeFunction("execute", array(intent), discUrl)
                .signers(AccountSigner.global(alice))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        System.out.println(charlie.getNep17Balances(neow3j));
    }

    @Test
    public void release_funds() throws Throwable {
        NeoToken neo = new NeoToken(neow3j);

        // Send NEO to the treasury
        Hash256 tx = neo.transfer(alice, treasury.getScriptHash(), new BigInteger("100"))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        // Define proposal (release funds from treasury to charlie)
        ContractParameter proposer = hash160(charlie);
        ContractParameter intent = array(
                treasury.getScriptHash(),
                "releaseFunds",
                array(neo.getScriptHash(), charlie.getScriptHash(), 20));
        ContractParameter discUrl = string("https://charlie.wants.neo/proposals/1");

        // Call createProposal
        tx = gov.invokeFunction("createProposal", proposer, array(intent), discUrl)
                .signers(AccountSigner.calledByEntry(charlie))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);
        String proposalHash = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getExecutions().get(0).getStack().get(0).getHexString();

        ext.fastForward(5);

        // Vote on proposal
        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(alice))
                .signers(AccountSigner.calledByEntry(alice))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(bob))
                .signers(AccountSigner.calledByEntry(bob))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        ext.fastForward(5);

        // Execute proposal
        tx = gov.invokeFunction("execute", array(intent), discUrl)
                .signers(AccountSigner.global(charlie))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        System.out.println(charlie.getNep17Balances(neow3j));
    }

    @Test
    public void update_gov() throws Throwable {
        // Define proposal
        ContractParameter proposer = hash160(charlie);
        CompilationUnit res = new Compiler().compile(GenericGovV2.class.getCanonicalName());
        ObjectMapper mapper = new ObjectMapper();
        String manifestString = mapper.writeValueAsString(res.getManifest());
        ContractParameter intent = array(
                gov.getScriptHash(),
                "update",
                array(res.getNefFile().toArray(), manifestString));
        ContractParameter discUrl = string("https://update.the.gov/proposals/1");

        // Call createProposal
        Hash256 tx = gov.invokeFunction("createProposal", proposer, array(intent), discUrl)
                .signers(AccountSigner.calledByEntry(charlie))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);
        String proposalHash = neow3j.getApplicationLog(tx).send().getApplicationLog()
                .getExecutions().get(0).getStack().get(0).getHexString();

        ext.fastForward(5);

        // Vote on proposal
        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(alice))
                .signers(AccountSigner.calledByEntry(alice))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        tx = gov.invokeFunction("vote", byteArray(proposalHash), bool(true), hash160(bob))
                .signers(AccountSigner.calledByEntry(bob))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);

        ext.fastForward(5);

        // Execute proposal
        tx = gov.invokeFunction("execute", array(intent), discUrl)
                .signers(AccountSigner.global(charlie))
                .sign().send().getSendRawTransaction().getHash();
        Await.waitUntilTransactionIsExecuted(tx, neow3j);
    }

}