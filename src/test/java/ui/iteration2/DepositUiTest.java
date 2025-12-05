package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.Account;
import api.iteration2.models.SumValues;
import api.iteration2.models.UserModel;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.DashboardPage;
import ui.iteration2.pages.DepositPage;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;


import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositUiTest extends BaseUiTest {
    private static UserModel user1;
    private static int user1acc1Id;
    private static String user1acc1Number;


    @BeforeAll
    public static void createUserAndAccount() {
        user1 = UserSteps.createUser();
        Account user1acc1 = UserSteps.createAccount(user1);
        user1acc1Id = (int) user1acc1.getId();
        user1acc1Number = user1acc1.getAccountNumber();
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 5000.00})
    public void userCanDepositMaxOrMinTest(double amount) {

        double initialSum = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        authAsUser(user1.getUsername(), user1.getPassword());
        new DepositPage().open()
                .deposit(user1acc1Number, String.format(Locale.US, "%.2f", amount))
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_DEPOSITED)
                .getPage(DashboardPage.class);

        //API проверка, что баланс счета изменился на сумму депозита
        BigDecimal balance = BigDecimal.valueOf(UserSteps.getAccBalance(user1.getToken(), user1acc1Id))
                .setScale(2, RoundingMode.HALF_UP);
        assertEquals(initialSum + amount, balance.doubleValue(), 0.0001f);

    }

    @Test
    public void UserCanNotDepositLessMinTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        double initialSum = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        new DepositPage().open().deposit(user1acc1Number, String.valueOf(SumValues.LESSMIN))
                .checkAlertAndConfirm(AlertMessages.ENTER_VALID_AMOUNT)
                .getPage(DepositPage.class);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.0001);

    }

    @Test
    public void UserCanNotDepositOverMaxTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        double initialSum = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        new DepositPage().open().deposit(user1acc1Number, String.valueOf(5000.01))
                .checkAlertAndConfirm(AlertMessages.DEPOSIT_LESS)
                .getPage(DepositPage.class);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.0001);

    }

    @Test
    public void UserCanNotDepositWithNotSelectedAccountTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        double initialSum = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        new DepositPage().open().deposit("Choose an account", String.valueOf(SumValues.SOMEDEPOSIT))
                .checkAlertAndConfirm(AlertMessages.SELECT_ACCOUNT)
                .getPage(DepositPage.class);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.0001);
    }

    @Test
    public void UserCanNotDepositWithNotSelectedAmountTest() {
        authAsUser(user1.getUsername(), user1.getPassword());

        double initialSum = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        new DepositPage().open().deposit(user1acc1Number, "")
                .checkAlertAndConfirm(AlertMessages.ENTER_VALID_AMOUNT)
                .getPage(DepositPage.class);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), user1acc1Id), 0.0001);
    }

    @ParameterizedTest
    @CsvSource({
            "5000.0000000000000000001, 5000.00",
            "0.0099999999999999999999, 0.01",
            "5001, 5000.00",
            "'5000.01', 5000.00",
            "5.000e3, 0.01",
            "-1, 0.01"
    })
    public void validationAmountInputTest(String input, String validatedInput) {
        authAsUser(user1.getUsername(), user1.getPassword());

        new DepositPage().open().setAmount(input);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldHave(exactValue(validatedInput));


    }

}
