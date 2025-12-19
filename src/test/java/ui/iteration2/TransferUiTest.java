package ui.iteration2;

import api.steps.UserSteps;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.*;
import api.models.Account;
import api.models.SumValues;
import api.models.UserModel;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import generators.NameGenerator;
import generators.RandomEntityGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.Alert;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.TransferPage;

import java.util.ArrayList;
import java.util.List;

import static com.browserup.bup.mitmproxy.MitmProxyProcessManager.MitmProxyLoggingLevel.alert;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.within;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class TransferUiTest extends BaseUiTest {
    private UserModel user1;
    private UserModel user2;
    private Account user1acc1;
    private Account user1acc2;
    private Account user2acc1;

    private int user1acc1Id;
    private String user1acc1Number;
    private String user1acc2Number;
    private int user2acc1Id;
    private String user2acc1Number;
    private int user1acc2Id;

    @BeforeEach
    public void init() {
        user1 = SessionStorage.getUser(1);
        user2 = SessionStorage.getUser(2);
        user1acc1 = UserSteps.createAccount(user1);
        user1acc1Id = (int) user1acc1.getId();
        user1acc1Number = user1acc1.getAccountNumber();
        user1acc2 = UserSteps.createAccount(user1);
        user1acc2Id = (int) user1acc2.getId();
        user1acc2Number = user1acc2.getAccountNumber();

        user2acc1 = UserSteps.createAccount(user2);
        user2acc1Id = (int) user2acc1.getId();
        user2acc1Number = user2acc1.getAccountNumber();

        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, 3);
    }


    @UserSession(value = 2, auth = 1)
    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToAnotherUserAccTest(String sum) {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        double finalSenderBalance = initialSenderBalance - Double.parseDouble(sum);
        double finalRecipientBalance = initialRecipientBalance + Double.parseDouble(sum);

        //проверка что на ui баланс изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, finalSenderBalance);

        //проверка API изменений сумм на счетах
        assertEquals(finalSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(finalRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user2acc1Id), 0.001f);

    }

    @UserSession
    @ParameterizedTest
    @CsvSource({
            "10000.0000000000000000001, 10000.00",
            "0.0099999999999999999999, 0.01",
            "10001, 10000.00",
            "'10000.01', 10000.00",
            "5.000e3, 0.01",
            "-1, 0.01"
    })
    public void transferAmountUiValidationTest(String inputValue, String validatedInputValue) {
        new TransferPage().open()
                .setAmount(inputValue)
                .getAmountInput().shouldHave(exactValue(validatedInputValue));
    }


    @UserSession
    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToOwnAccTest(String sum) {
        UserSteps.setCustomerName(user1, NameGenerator.generateName());
        double initialAcc1Balance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialAcc2Balance = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1, user1acc2Number, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        double finalAcc1Balance = initialAcc1Balance - Double.parseDouble(sum);
        double finalAcc2Balance = initialAcc2Balance + Double.parseDouble(sum);

        //проверка что на ui баланс изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, finalAcc1Balance);
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc2Number, finalAcc2Balance);

        //проверка API изменений сумм на счетах
        assertEquals(finalAcc1Balance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(finalAcc2Balance, UserSteps.getAccBalance(user1.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession
    @Test
    public void userCanTransferToSameAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        UserSteps.setCustomerName(user1, NameGenerator.generateName());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1, user1acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        //баланс не поменялся
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSumUser1Acc1);

        assertEquals(initialSumUser1Acc1, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferOverBalanceTest() {
        //предварительный перевод для уменьшения баланса счета отправителя
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED);

        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.INSUFFICIENT_FUNDS);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferLessMinToAnotherUserAccTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, String.valueOf(SumValues.LESSMIN.getValue()))
                .checkAlertAndConfirm(AlertMessages.TRANSFER_MUST_BE_AT_LEAST);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);
    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferOverMaxToAnotherUserAccTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, String.valueOf(SumValues.OVERMAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.TRANSFER_AMOUNT_CANNOT_EXCEED);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferToInvalidRecipientNameTest() {
        String newName = NameGenerator.generateName();
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithParams(user1acc1Number, newName, user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertWithArgsAndConfirm(AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferToInvalidRecipientAccTest() {
        String accNumber = RandomEntityGenerator.generate(String.class);
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithParams(user1acc1Number, UserSteps.getCustomerName(user2),
                        accNumber, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertWithArgsAndConfirm(AlertMessages.NO_USER_FOUND_WITH_THIS_ACCOUNT_NUMBER);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);
    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferToEmptyRecipientAccTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithParams(user1acc1Number, UserSteps.getCustomerName(user2),
                        null, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferWithEmptyFieldsTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .clickSendTransfer()
                .checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferToEmptyRecipientNameTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithParams(user1acc1Number, null,
                        user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);
    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferNullAmountTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithParams(user1acc1Number, UserSteps.getCustomerName(user2),
                        user2acc1Number, null)
                .checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferWithEmptySenderAccTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user1acc2Id);

        TransferPage page = new TransferPage().open().getPage(TransferPage.class);
        page.getRecipientNameInput().setValue(UserSteps.getCustomerName(user2));
        page.getRecipientAccountInput().setValue(user2acc1Number);
        page.getAmountInput().setValue(String.valueOf(SumValues.MAXTRANSFER.getValue()));
        page.getConfirmCheckbox().setSelected(true);
        page.getSendTransferButton().click();
        page.checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession(value = 2, auth = 1)
    @Test
    public void userCanNotTransferWithNotSelectedCheckboxTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new TransferPage().open().getPage(TransferPage.class)
                .transferWithoutCheckbox(user1acc1Number, UserSteps.getCustomerName(user2), user2acc1Number,
                        String.valueOf(SumValues.SOMEDEPOSIT.getValue()))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSumUser1Acc1);

        assertEquals(initialSumUser1Acc1, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialSumUser2Acc1, UserSteps.getAccBalance(user2.getToken(), user2acc1Id), 0.001f);
    }

    @UserSession
    @Test
    public void userCanRepeatTransferToOwnAccountTest() {
        //депозит на 0.02$ на счет user1acc2Id
        UserSteps.deposit(user1.getToken(), user1acc2Id, SumValues.SOMEDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser1Acc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        //переход на Transfer Again
        TransferPage transferPage = new TransferPage().open().goToTransferAgain();
        transferPage.getMatchingTransactionsHeader().shouldBe(visible);

        int transactionsQuantity = transferPage.getMatchingTransactionsItems().size();
        transferPage.repeatButtonInMatchingTransactions("DEPOSIT - $0.02").click();

        transferPage.getModalRepeatTransfer().shouldHave(Condition.text("\uD83D\uDD01 Repeat Transfer"))
                .shouldBe(Condition.visible);
        transferPage.getModalRepeatTransferToAccountId().shouldHave(Condition.exactText(String.valueOf(user1acc2Id)));
        transferPage.getModalRepeatTransferAccountSelector().shouldHave(Condition.text("-- Choose an account --"));
        transferPage.getModalRepeatTransferAmountInput().shouldHave(value("0.02"));
        transferPage.getModalRepeatConfirmCheckbox().shouldNotBe(selected);
        transferPage.getModalRepeatSendTransferButton().shouldBe(Condition.disabled);


        transferPage.repeatTransfer(user1acc1Number)
                .checkAlertWithArgsAndConfirm(AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO,
                        String.valueOf(SumValues.SOMEDEPOSIT.getValue()), user1acc1Id, user1acc2Id)
                .getMatchingTransactionsHeader().shouldBe(visible);

        refresh();
        transferPage.goToTransferAgain();

        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();
        softly.assertThat(transactionsFinalQuantity).isEqualTo(transactionsQuantity + 2);

        softly.assertThat(
                transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_OUT - $0.02"))
                        .exists());
        softly.assertThat(
                transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_IN - $0.02"))
                        .exists());

        //проверка что баланс Acc1 уменьшился, а Acc2 увеличился на 0.02
        assertEquals(initialSumUser1Acc1 - 0.02, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialSumUser1Acc2 + 0.02, UserSteps.getAccBalance(user1.getToken(), user1acc2Id), 0.001f);

    }

    @UserSession
    @Test
    public void userCanRepeatTransferToAnotherUserAccountTest() {
        //перевод 5000 с user1acc1Id на user2acc1Id
        UserSteps.transferSuccessResponse(user1.getToken(), user1acc1Id, user2acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        TransferPage transferPage = new TransferPage().open().goToTransferAgain();

        int transactionsQuantity = transferPage.getMatchingTransactionsItems().size();

        transferPage.repeatButtonInMatchingTransactions("TRANSFER_OUT - $5000.00").click();
        transferPage.getModalRepeatTransferToAccountId().shouldHave(Condition.exactText(String.valueOf(user2acc1Id)));

        String alertText = transferPage.repeatTransfer(user1acc1Number)
                .getAlertTextAndConfirm();

        softly.assertThat(alertText).contains(
                AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO.format(
                        SumValues.MAXDEPOSIT.getValue(),
                        user1acc1Id,
                        user2acc1Id
                ));

        refresh();
        transferPage.goToTransferAgain();
        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();

        softly.assertThat(transactionsFinalQuantity).isEqualTo(transactionsQuantity + 1);

        int newTransactions = transferPage.getMatchingTransactionsItems()
                .filter(Condition.text("TRANSFER_OUT - $5000.00"))
                .size();
        softly.assertThat(newTransactions).isEqualTo(2);

        double actualUser1Acc1Balance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double expectedUser1Acc1Balance = initialSumUser1Acc1 - (double) SumValues.MAXDEPOSIT.getValue();
        softly.assertThat(actualUser1Acc1Balance).isCloseTo(expectedUser1Acc1Balance, within(0.0001));

        double actualUser2Acc1Balance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        double expectedUser2Acc1Balance = initialSumUser2Acc1 + (double) SumValues.MAXDEPOSIT.getValue();
        softly.assertThat(actualUser2Acc1Balance).isCloseTo(expectedUser2Acc1Balance, within(0.0001));

    }
}




