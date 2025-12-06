package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.*;
import api.iteration2.specs.ResponseSpecs;
import com.codeborne.selenide.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.DashboardPage;

import ui.iteration2.pages.TransferPage;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;

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
        user1Name = UserSteps.setCustomerName(user1, "Garry Second");
        user2 = UserSteps.createUser();
        user2Name = UserSteps.setCustomerName(user2, "Garry Second");
        Account user2acc1 = UserSteps.createAccount(user2);
        user2acc1Id = (int) user2acc1.getId();
        user2acc1Number = user2acc1.getAccountNumber();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToAnotherUserAccTest(String sum) {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
          new DashboardPage().open().openTransferPage().getPage(TransferPage.class)
                 .transferAmount(user1acc1Number, user2Name, user2acc1Number, sum)
                 .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                 .getPage(TransferPage.class);

        //после обновления страницы балансы на ui поменялись
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", UserSteps.getAccBalance(user1.getToken(), user1acc1Id));
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", UserSteps.getAccBalance(user2.getToken(), user2acc1Id));

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка изменений сумм на счетах
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user2Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumUser1Acc1 - Double.parseDouble(sum), user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(initialSumUser2Acc1 + Double.parseDouble(sum), user2Acc1balance.doubleValue(), 0.0001f);
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
        double initialSumAcc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumAcc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1Name, user1acc2Number, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED).getPage(TransferPage.class);

        //после обновления страницы балансы на ui поменялись
        String formattedSumAcc1 = String.format(Locale.US, "%.2f", UserSteps.getAccBalance(user1.getToken(), user1acc1Id));
        String formattedSumAcc2 = String.format(Locale.US, "%.2f", UserSteps.getAccBalance(user1.getToken(), user1acc2Id));

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumAcc1 + ")"))
                .should(Condition.exist);
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc2Number + " (Balance: $" + formattedSumAcc2 + ")"))
                .should(Condition.exist);

        //проверка изменений сумм на счетах
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user1Acc2balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc2Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumAcc1 - Double.parseDouble(sum), user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(initialSumAcc2 + Double.parseDouble(sum), user1Acc2balance.doubleValue(), 0.0001f);
    }

    @Test
    public void userCanNotTransferLessMinToAnotherUserAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, String.valueOf(0.00))
                .checkAlertAndConfirm(AlertMessages.TRANSFER_MUST_BE_AT_LEAST).getPage(TransferPage.class);

        //после обновления страницы балансы на ui не поменялись
        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсуствия изменений сумм на счетах
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user2Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumUser1Acc1, user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(initialSumUser2Acc1, user2Acc1balance.doubleValue(), 0.0001f);
    }

    @Test
    public void userCanNotTransferOverMaxToAnotherUserAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, String.valueOf(10000.01))
                .checkAlertAndConfirm(AlertMessages.TRANSFER_AMOUNT_CANNOT_EXCEED)
                .getPage(TransferPage.class);

        //после обновления страницы балансы на ui не поменялись
        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсуствия изменений сумм на счетах
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user2Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumUser1Acc1, user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(initialSumUser2Acc1, user2Acc1balance.doubleValue(), 0.0001f);
    }

    @Test
    public void userCanTransferToSameAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1Name, user1acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        //после обновления страницы баланс на ui не поменялся
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений суммы на счете
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(initialSumUser1Acc1, user1Acc1balance.doubleValue(), 0.0001f);
    }

    @Test
    public void userCanNotTransferOverBalanceTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.INSUFFICIENT_FUNDS)
                .getPage(TransferPage.class);


        //после обновления страницы балансы на ui не поменялись (по сравнению с балансами после первого перевода)
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        String formattedFinalSumUser1Acc1 = String.format(Locale.US, "%.2f", finalSumUser1Acc1);
        String formattedFinalSumUser2Acc1 = String.format(Locale.US, "%.2f", finalSumUser2Acc1);

        SelenideElement accUser1Selector = new TransferPage().open().getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedFinalSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open().getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedFinalSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счетах
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user2Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(finalSumUser1Acc1, user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(finalSumUser2Acc1, user2Acc1balance.doubleValue(), 0.0001f);
    }

    @Test
    public void userCanNotTransferToInvalidRecipientNameTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, "Taylor Swift", user2acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH)
                .getPage(TransferPage.class);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счетах
        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferToInvalidRecipientAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, "ACC000000", String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.NO_USER_FOUNT_WITH_THIS_ACCOUNT_NUMBER)
                .getPage(TransferPage.class);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счетах
        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);

    }

    @Test
    public void userCanNotTransferToWrongRecipientAccTest() {
        user2Name = UserSteps.setCustomerName(user2, "Anna Smith");
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user1acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счетах
        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferToWrongRecipientNameTest() {
        user2Name = UserSteps.setCustomerName(user2, "Anna Smith");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        //перевод на свое имя на счет user2acc1Number
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user1Name, user2acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.RECIPIENT_NAME_DOES_NOT_MATCH)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счетах
        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferWithEmptyFieldsTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .clickSendTransfer()
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                        .getPage(TransferPage.class);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferToEmptyRecipientNameTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, null, user2acc1Number, String.valueOf(10000.00))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferNullAmountTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, user2acc1Number, null)
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);

    }


    @Test
    public void userCanNotTransferWithEmptySenderAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount("Choose an account", user2Name, user2acc1Number, String.valueOf(SumValues.SOMEDEPOSIT))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferToEmptyRecipientAccTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());

        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(user1acc1Number, user2Name, null, String.valueOf(SumValues.SOMEDEPOSIT))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanNotTransferWithNotSelectedCheckboxTest() {
        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        authAsUser(user1.getUsername(), user1.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferWithoutCheckbox(user1acc1Number, user2Name, user2acc1Number, String.valueOf(SumValues.SOMEDEPOSIT))
                .checkAlertAndConfirm(AlertMessages.PLEASE_FILL_ALL_FIELDS_AND_CONFIRM)
                .getPage(TransferPage.class);

        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        SelenideElement accUser1Selector = new TransferPage().open()
                .getAccountSelector();
        accUser1Selector.$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        authAsUser(user2.getUsername(), user2.getPassword());
        SelenideElement accUser2Selector = new TransferPage().open()
                .getAccountSelector();
        accUser2Selector.$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        assertEquals(initialSumUser1Acc1, finalSumUser1Acc1, 0.0001f);
        assertEquals(initialSumUser2Acc1, finalSumUser2Acc1, 0.0001f);
    }

    @Test
    public void userCanRepeatTransferToOwnAccountTest() {
        //депозит на 0.02$ на счет user1acc2Id
        UserSteps.deposit(user1.getToken(), user1acc2Id, SumValues.SOMEDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser1Acc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        //переход на Transfer Again
        TransferPage transferPage= new TransferPage().open().goToTransferAgain();
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
                .checkAlertWithArgsAndConfirm(AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO,"0.02", user1acc1Id, user1acc2Id)
                .getMatchingTransactionsHeader().shouldBe(visible);

        refresh();
        transferPage.goToTransferAgain();
        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();
        assertEquals(transactionsQuantity + 2, transactionsFinalQuantity);

        transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_OUT - $0.02")).shouldBe(visible);
        transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_IN - $0.02")).shouldBe(visible);

        //проверка что баланс Acc1 уменьшился, а Acc2 увеличился на 0.02
        BigDecimal user1Acc1balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal user1Acc2balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc2Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumUser1Acc1 - 0.02, user1Acc1balance.doubleValue(), 0.0001f);
        assertEquals(initialSumUser1Acc2 + 0.02, user1Acc2balance.doubleValue(), 0.0001f);

    }

    @Test
    public void userCanRepeatTransferToAnotherUserAccountTest() {
        //перевод 5000 с user1acc1Id на user2acc1Id
        UserSteps.transferSuccessResponse(user1.getToken(), user1acc1Id, user2acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        TransferPage transferPage= new TransferPage().open().goToTransferAgain();

        int transactionsQuantity = transferPage.getMatchingTransactionsItems().size();

        transferPage.repeatButtonInMatchingTransactions("TRANSFER_OUT - $5000.00").click();
        transferPage.getModalRepeatTransferToAccountId().shouldHave(Condition.exactText(String.valueOf(user2acc1Id)));

        System.out.println("Трансфер и алерт:");
        transferPage.repeatTransfer(user1acc1Number)
                .checkAlertWithArgsAndConfirm(AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO,"5000", user1acc1Id, user2acc1Id)
                .getMatchingTransactionsHeader().shouldBe(visible);

        refresh();
        transferPage.goToTransferAgain();
        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();
        System.out.println("Проверка, что количество транзакций увеличилось на 1:");
        assertEquals(transactionsQuantity + 1, transactionsFinalQuantity);

        int newTransactions = transferPage.getMatchingTransactionsItems()
                .filter(Condition.text("TRANSFER_OUT - $5000.00"))
                .size();
        System.out.println("Проверка, что теперь 2 'TRANSFER_OUT - $5000.00':");
        assertEquals(2, newTransactions);

        System.out.println("Проверка, что баланс user1acc1Id уменьшился на 5000, баланс user2acc1Id увеличился на 5000");

        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(UserSteps.getAccBalance(user2.getToken(), user2acc1Id))
                .setScale(2, RoundingMode.HALF_UP);

        assertEquals(initialSumUser1Acc1 - 5000.00, balanceUser1Acc1.doubleValue(), 0.0001f);
        assertEquals(initialSumUser2Acc1 + 5000.00, balanceUser2Acc1.doubleValue(), 0.0001f);
    }

}




