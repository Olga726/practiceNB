package ui.iteration2;

import api.iteration2.UserSteps;
import api.specs.ResponseSpecs;
import com.codeborne.selenide.*;
import api.models.Account;
import api.models.SumValues;
import api.models.UserModel;
import generators.NameGenerator;
import generators.RandomEntityGenerator;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.TransferPage;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferUiTest extends BaseUiTest {
    private UserModel user1;
    private UserModel user2;
    private int user1acc1Id;
    private String user1acc1Number;
    private String user1acc2Number;
    private String user1Name;
    private String user2Name;
    private int user2acc1Id;
    private String user2acc1Number;
    private int user1acc2Id;
    private String name1;

    @BeforeEach
    public void createUserAndAccount() {
        user1 = UserSteps.createUser();
        Account user1acc1 = UserSteps.createAccount(user1);
        user1acc1Id = (int) user1acc1.getId();
        user1acc1Number = user1acc1.getAccountNumber();
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        Account user1acc2 = UserSteps.createAccount(user1);
        user1acc2Id = (int) user1acc2.getId();
        user1acc2Number = user1acc2.getAccountNumber();
        name1 = NameGenerator.generateName();
        user1Name = UserSteps.setCustomerName(user1, name1);
        user2 = UserSteps.createUser();
        user2Name = UserSteps.setCustomerName(user2, name1);
        Account user2acc1 = UserSteps.createAccount(user2);
        user2acc1Id = (int) user2acc1.getId();
        user2acc1Number = user2acc1.getAccountNumber();
    }

    @AfterEach
    public void deleteUsers() {
        UserSteps.deleteUsers(user1, user2);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToAnotherUserAccTest(String sum) {
        UiSteps.positiveTransferAssert(user1acc1Number, user2acc1Number, user1, user2, sum,
                user1acc1Id, user2acc1Id, user2Name, user1acc1Number, user2acc1Number);

    }

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
        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open()
                .setAmount(inputValue)
                .getAmountInput().shouldHave(exactValue(validatedInputValue));
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToOwnAccTest(String sum) {
        UiSteps.positiveTransferAssert(user1acc1Number, user1acc2Number, user1, user1, sum,
                user1acc1Id, user1acc2Id, user1Name, user1acc1Number, user1acc2Number);

    }

    @Test
    public void userCanTransferToSameAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1Name, user1acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        //баланс не поменялся
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSumUser1Acc1);

        assertEquals(initialSumUser1Acc1, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
    }

    @Test
    public void userCanNotTransferOverBalanceTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED);

        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.INSUFFICIENT_FUNDS,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferLessMinToAnotherUserAccTest() {
        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.LESSMIN.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.TRANSFER_MUST_BE_AT_LEAST,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferOverMaxToAnotherUserAccTest() {
        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.OVERMAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.TRANSFER_AMOUNT_CANNOT_EXCEED,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferToInvalidRecipientNameTest() {
        String name2 = NameGenerator.generateName();
        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, name2, AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferToInvalidRecipientAccTest() {
        String accNumber = RandomEntityGenerator.generate(String.class);
        UiSteps.negativeTransferAssert(user1acc1Number, accNumber, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.NO_USER_FOUND_WITH_THIS_ACCOUNT_NUMBER,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferToWrongRecipientAccTest() {
        String name2 = NameGenerator.generateName();
        user2Name = UserSteps.setCustomerName(user2, name2);

        UiSteps.negativeTransferAssert(user1acc1Number, user1acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferToEmptyRecipientAccTest() {
        UiSteps.negativeTransferAssert(user1acc1Number, null, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferWithEmptyFieldsTest() {
        UiSteps.negativeTransferAssert("Choose an account", null, user1, user2,
                null,
                user1acc1Id, user2acc1Id, null, AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferToWrongRecipientNameTest() {
        String name2 = NameGenerator.generateName();
        user2Name = UserSteps.setCustomerName(user2, name2);

        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user1Name, AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferToEmptyRecipientNameTest() {
        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, null, AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                user1acc1Number, user2acc1Number);

    }

    @Test
    public void userCanNotTransferNullAmountTest() {
        UiSteps.negativeTransferAssert(user1acc1Number, user2acc1Number, user1, user2, null,
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferWithEmptySenderAccTest() {
        UiSteps.negativeTransferAssert("Choose an account", user2acc1Number, user1, user2,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                user1acc1Id, user2acc1Id, user2Name, AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                user1acc1Number, user2acc1Number);
    }

    @Test
    public void userCanNotTransferWithNotSelectedCheckboxTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferWithoutCheckbox(user1acc1Number, user2Name, user2acc1Number,
                        String.valueOf(SumValues.SOMEDEPOSIT.getValue()))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSumUser1Acc1);

        authAsUser(user2.getUsername(), user2.getPassword());
        new TransferPage().open()
                .verifyBalanceInSelector(user2acc1Number, initialSumUser2Acc1);

        assertEquals(initialSumUser1Acc1, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialSumUser2Acc1, UserSteps.getAccBalance(user2.getToken(), user2acc1Id), 0.001f);
    }

    @Test
    public void userCanRepeatTransferToOwnAccountTest() {
        //депозит на 0.02$ на счет user1acc2Id
        UserSteps.deposit(user1.getToken(), user1acc2Id, SumValues.SOMEDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser1Acc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        authAsUser(user1.getUsername(), user1.getPassword());
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
        assertEquals(transactionsQuantity + 2, transactionsFinalQuantity);

        transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_OUT - $0.02")).shouldBe(visible);
        transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_IN - $0.02")).shouldBe(visible);

        //проверка что баланс Acc1 уменьшился, а Acc2 увеличился на 0.02
        assertEquals(initialSumUser1Acc1 - 0.02, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialSumUser1Acc2 + 0.02, UserSteps.getAccBalance(user1.getToken(), user1acc2Id), 0.001f);

    }

    @Test
    public void userCanRepeatTransferToAnotherUserAccountTest() {
        //перевод 5000 с user1acc1Id на user2acc1Id
        UserSteps.transferSuccessResponse(user1.getToken(), user1acc1Id, user2acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        TransferPage transferPage = new TransferPage().open().goToTransferAgain();

        int transactionsQuantity = transferPage.getMatchingTransactionsItems().size();

        transferPage.repeatButtonInMatchingTransactions("TRANSFER_OUT - $5000.00").click();
        transferPage.getModalRepeatTransferToAccountId().shouldHave(Condition.exactText(String.valueOf(user2acc1Id)));

        System.out.println("Трансфер и алерт:");
        transferPage.repeatTransfer(user1acc1Number)
                .checkAlertWithArgsAndConfirm(AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO, String.valueOf(SumValues.MAXDEPOSIT.getValue()), user1acc1Id, user2acc1Id)
                .getMatchingTransactionsHeader().shouldBe(visible);

        refresh();
        transferPage.goToTransferAgain();
        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();
        assertEquals(transactionsQuantity + 1, transactionsFinalQuantity);

        int newTransactions = transferPage.getMatchingTransactionsItems()
                .filter(Condition.text("TRANSFER_OUT - $5000.00"))
                .size();
        assertEquals(2, newTransactions);

        assertEquals(initialSumUser1Acc1 - SumValues.MAXDEPOSIT.getValue(), UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.0001f);
        assertEquals(initialSumUser2Acc1 + SumValues.MAXDEPOSIT.getValue(), UserSteps.getAccBalance(user2.getToken(), user2acc1Id), 0.0001f);
    }

}




