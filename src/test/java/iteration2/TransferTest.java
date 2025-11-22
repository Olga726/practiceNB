package iteration2;


import models.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;


public class TransferTest extends BaseTest {

    private static UserModel user1;
    private static UserModel user2;
    private static long acc1Id;
    private static long acc1_2Id;
    private static long acc2Id;
    private static final float MAXTRANSFER = 10000.0f;
    private static final float MINTRANSFER = 0.01f;

    @BeforeAll
    public static void preSteps() {
        //создание пользователя1
        user1 = UserSteps.createUserAndGetToken();

        //пользователь1 создает счет1
        acc1Id = UserSteps.createAccount(user1.getToken());

        //пользователь1 создает счет2
        acc1_2Id = UserSteps.createAccount(user1.getToken());

        //создание пользователя2
        user2 = UserSteps.createUserAndGetToken();

        //пользователь2 создает счет
        acc2Id = UserSteps.createAccount(user2.getToken());

    }

    @Test
    public void userCanTransferMaxSumToTheirOwnAcc() {
        //пользователь1 делает max депозит 5000 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc1Id));

        //пользователь1 повторно делает max депозит 5000 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc1Id));

        //пользователь1 делает перевод 10000 на свой счет acc1_2Id

        TransferResponse transferResponse = new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(TransferFactory.maxTransferRequest(acc1Id, acc1_2Id))
                .extract()
                .as(TransferResponse.class);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MAXTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1_2Id).isEqualTo(transferResponse.getReceiverAccountId());

    }

    @Test
    public void userCanTransferMinSumToTheirOwnAcc() {
        //пользователь1 делает min депозит 0.01 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.minDeposit(acc1Id));

        //пользователь1 делает перевод 0.01 на свой счет acc1_2Id
        TransferResponse transferResponse = new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(TransferFactory.minTransferRequest(acc1Id, acc1_2Id))
                .extract()
                .as(TransferResponse.class);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MINTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1_2Id).isEqualTo(transferResponse.getReceiverAccountId());
    }

    @Test
    public void userCanNotTransferSumOverBalanceToTheirOwnAcc() {
        //пользователь1 делает перевод 10000 на свой счет acc1_2Id
        new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.badRequestNotEnoughAmount())
                .post(TransferFactory.maxTransferRequest(acc1Id, acc1_2Id));

    }

    @Test
    public void userCanNotTransferSumOverBalanceToAnotherUserAcc() {
        //пользователь2 делает перевод 0.01 на счет пользователя1
        new UserTransferRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.badRequestNotEnoughAmount())
                .post(TransferFactory.minTransferRequest(acc2Id, acc1Id));

    }

    @Test
    public void userCanTransferMinSumToAnotherUserAcc() {
        //пользователь2 делает min депозит 0.01 на счет acc2Id
        new UserDepositRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.minDeposit(acc2Id));

        //пользователь2 делает перевод 0.01 на счет пользователя1
        TransferResponse transferResponse = new UserTransferRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.success())
                .post(TransferFactory.minTransferRequest(acc2Id, acc1Id))
                .extract()
                .as(TransferResponse.class);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MINTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc2Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getReceiverAccountId());

    }

    @Test
    public void userCanTransferMaxSumToAnotherUserAcc() {
        //пользователь2 делает max депозит 5000.0 на счет acc2Id
        new UserDepositRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc2Id));

        //пользователь2 повторно делает max депозит 5000.0 на счет acc2Id
        new UserDepositRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc2Id));

        //пользователь2 делает перевод 10000 на счет пользователя1
        TransferResponse transferResponse = new UserTransferRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.success())
                .post(TransferFactory.maxTransferRequest(acc2Id, acc1Id))
                .extract()
                .as(TransferResponse.class);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MAXTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc2Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getReceiverAccountId());

    }

    @Test
    public void userCanNotTransferFromAnotherUserAcc() {
        //пользователь2 делает перевод 10000 себе со счета пользователя1
        new UserTransferRequester(RequestSpecs.authSpec(user2.getToken()),
                ResponseSpecs.unauthorized())
                .post(TransferFactory.maxTransferRequest(acc1Id, acc2Id));

    }

    @Test
    public void userCanTransferToTheSameAccount() {
        //пользователь1 делает min депозит 0.01 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.minDeposit(acc1Id));

        //пользователь1 делает перевод 0.01 на свой счет acc1Id
        TransferResponse transferResponse = new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(TransferFactory.minTransferRequest(acc1Id, acc1Id))
                .extract()
                .as(TransferResponse.class);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MINTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getReceiverAccountId());
    }

    @Test
    public void userCanNotTransferOverMaxTransfer(){
        //пользователь1 делает max депозит 5000 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc1Id));

        //пользователь1 повторно делает max депозит 5000 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc1Id));

        //пользователь1 делает перевод свыше max
        new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.badRequestTransferOverMax())
                .post(TransferFactory.overMaxTransferRequest(acc1Id, acc1_2Id));
    }

    @Test
    public void userCanNotTransferLessMinTransfer(){
        //пользователь1 делает min депозит 0.01 на счет acc1Id
        new UserDepositRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.success())
                .post(DepositFactory.minDeposit(acc1Id));

        //пользователь1 делает перевод меньше min
        new UserTransferRequester(RequestSpecs.authSpec(user1.getToken()),
                ResponseSpecs.badRequestTransferLessMin())
                .post(TransferFactory.belowMinTransferRequest(acc1Id, acc1_2Id));
    }

}
