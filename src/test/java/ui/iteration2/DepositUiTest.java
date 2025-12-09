package ui.iteration2;

import api.iteration2.UserSteps;
import api.models.Account;
import api.models.SumValues;
import api.models.UserModel;
import org.junit.jupiter.api.AfterAll;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import ui.iteration2.pages.AlertMessages;

import ui.iteration2.pages.DepositPage;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;


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
    @AfterAll
    public static void deleteUsers(){
        UserSteps.deleteUsers(user1);
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 5000.00})
    public void userCanDepositMaxOrMinTest(double amount) {
        UiSteps.positiveDeposit(user1, user1acc1Id, user1acc1Number, String.valueOf(amount));
    }

    @Test
    public void UserCanNotDepositLessMinTest() {
        UiSteps.negativeDepositAssert(user1, user1acc1Id, user1acc1Number,
                String.valueOf(SumValues.LESSMIN.getValue()), AlertMessages.ENTER_VALID_AMOUNT);
    }

    @Test
    public void UserCanNotDepositOverMaxTest() {
        UiSteps.negativeDepositAssert(user1, user1acc1Id, user1acc1Number,
                String.valueOf(SumValues.OVERMAXDEPOSIT.getValue()), AlertMessages.DEPOSIT_LESS);
    }

    @Test
    public void UserCanNotDepositWithNotSelectedAccountTest() {
        UiSteps.negativeDepositAssert(user1, user1acc1Id, "Choose an account",
                String.valueOf(SumValues.SOMEDEPOSIT.getValue()), AlertMessages.SELECT_ACCOUNT);
    }

    @Test
    public void UserCanNotDepositWithNotSelectedAmountTest() {
        UiSteps.negativeDepositAssert(user1, user1acc1Id, user1acc1Number,
                null, AlertMessages.ENTER_VALID_AMOUNT);
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
