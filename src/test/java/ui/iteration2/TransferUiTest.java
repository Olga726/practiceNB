package ui.iteration2;

import api.steps.UiSteps;
import api.steps.UserSteps;
import api.specs.ResponseSpecs;
import api.models.Account;
import api.models.SumValues;
import api.models.UserModel;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import generators.NameGenerator;
import generators.RandomEntityGenerator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.TransferPage;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static com.codeborne.selenide.Condition.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

@UserSession(value = 2, auth = 1, ui = true)
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

    @Disabled("нет валидации в поле суммы")
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

    @Test
    public void userCanTransferToSameAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        UserSteps.setCustomerName(user1, NameGenerator.generateName());

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user1),
                user1,
                user1acc1Number,
                user1acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.SUCCESSFULLY_TRANSFERED,
                initialSumUser1Acc1,
                initialSumUser1Acc1);

    }

    @Test
    public void userCanNotTransferOverBalanceTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        //предварительный перевод для уменьшения баланса счета отправителя
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2, user2acc1Number, String.valueOf(SumValues.MAXTRANSFER.getValue()))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED);

        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                user2acc1Number,
                user2acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.INSUFFICIENT_FUNDS,
                initialSenderBalance,
                initialRecipientBalance);

    }


    @Test
    public void userCanNotTransferLessMinToAnotherUserAccTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        UserSteps.setCustomerName(user2, NameGenerator.generateName());

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                user2acc1Number,
                user2acc1Id,
                String.valueOf(SumValues.LESSMIN.getValue()),
                AlertMessages.TRANSFER_MUST_BE_AT_LEAST,
                initialSenderBalance,
                initialRecipientBalance);
    }

    @Test
    public void userCanNotTransferOverMaxToAnotherUserAccTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        UserSteps.setCustomerName(user2, NameGenerator.generateName());

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                user2acc1Number,
                user2acc1Id,
                String.valueOf(SumValues.OVERMAXTRANSFER.getValue()),
                AlertMessages.TRANSFER_AMOUNT_CANNOT_EXCEED,
                initialSenderBalance,
                initialRecipientBalance);
    }
    @Disabled("баг, успешный перевод, если имя получателя не корректное")
    @Test
    public void userCanNotTransferToInvalidRecipientNameTest() {
        String newName = NameGenerator.generateName();
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                newName,
                user2,
                user2acc1Number,
                user2acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH,
                initialSenderBalance,
                initialRecipientBalance);

    }

    @Test
    public void userCanNotTransferToInvalidRecipientAccTest() {
        String accNumber = RandomEntityGenerator.generate(String.class);
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                accNumber,
                user2acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.NO_USER_FOUND_WITH_THIS_ACCOUNT_NUMBER,
                initialSenderBalance,
                initialRecipientBalance);
    }

    @Test
    public void userCanNotTransferToEmptyRecipientAccTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                null,
                user2acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                initialSenderBalance,
                initialRecipientBalance);
    }

    @Test
    public void userCanNotTransferWithEmptyFieldsTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new TransferPage().open().getPage(TransferPage.class)
                .clickSendTransfer()
                .checkAlertWithArgsAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM);

        //проверка что на ui баланс не изменился
        new TransferPage().open()
                .verifyBalanceInSelector(user1acc1Number, initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(user2.getToken(), user2acc1Id), 0.001f);

    }
    @Disabled("баг - успешный перевод при пустом имени получателя")
    @Test
    public void userCanNotTransferToEmptyRecipientNameTest() {
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                null,
                user2,
                user2acc1Number,
                user2acc1Id,
                String.valueOf(SumValues.MAXTRANSFER.getValue()),
                AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                initialSenderBalance,
                initialRecipientBalance);
    }

    @Test
    public void userCanNotTransferNullAmountTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        new UiSteps(user1).negativeTransferWithParams(
                user1acc1Number,
                user1acc1Number,
                user1acc1Id,
                UserSteps.getCustomerName(user2),
                user2,
                user2acc1Number,
                user2acc1Id,
                null,
                AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
                initialSenderBalance,
                initialRecipientBalance);
    }

    @Test
    public void userCanNotTransferWithEmptySenderAccTest() {
        UserSteps.setCustomerName(user2, NameGenerator.generateName());
        double initialSenderBalance = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialRecipientBalance = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

      new UiSteps(user1).negativeTransferWithParams(
              null,
              user1acc1Number,
              user1acc1Id,
              UserSteps.getCustomerName(user2),
              user2,
              user2acc1Number,
              user2acc1Id,
              String.valueOf(SumValues.MAXTRANSFER.getValue()),
              AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM,
              initialSenderBalance,
              initialRecipientBalance);
    }

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

    @Test
    public void userCanRepeatTransferToOwnAccountTest() {
        //депозит на 0.02$ на счет user1acc2Id
        UserSteps.deposit(user1.getToken(), user1acc2Id, SumValues.SOMEDEPOSIT, ResponseSpecs.success());

        BigDecimal initialSumUser1Acc1 = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal initialSumUser1Acc2 = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc2Id))
                .setScale(2, RoundingMode.HALF_UP);

        new UiSteps(user1).positiveRepeatingTransfer(
                "DEPOSIT - $" + SumValues.SOMEDEPOSIT.getValue(),
                user1,
                user1acc2Id,
                user1acc1Id,
                SumValues.SOMEDEPOSIT,
                user1acc1Number,
                initialSumUser1Acc1,
                initialSumUser1Acc2,
                2
        );
    }

    @Disabled("баг, повтор трансфера на чужой счет работает как депозит на свой")
    @Test
    public void userCanRepeatTransferToAnotherUserAccountTest() {
        //перевод 5000 с user1acc1Id на user2acc1Id
        UserSteps.transferSuccessResponse(user1.getToken(), user1acc1Id, user2acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());

        BigDecimal initialSumUser1Acc1 = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        BigDecimal initialSumUser2Acc1 = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        new UiSteps(user1).positiveRepeatingTransfer(
                "TRANSFER_OUT - $" + SumValues.MAXDEPOSIT.getValue(),
                user2,
                user2acc1Id,
                user1acc1Id,
                SumValues.MAXDEPOSIT,
                user1acc1Number,
                initialSumUser1Acc1,
                initialSumUser2Acc1,
                1
        );
    }
}




