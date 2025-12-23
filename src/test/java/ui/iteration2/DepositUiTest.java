package ui.iteration2;

import api.steps.UiSteps;
import api.steps.UserSteps;
import api.models.Account;
import api.models.SumValues;
import api.models.UserModel;
import common.annotations.Browsers;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.DepositPage;
import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;

public class DepositUiTest extends BaseUiTest {
    private UserModel user;
    private Account acc;

    @BeforeEach
    public void init(){
        user = SessionStorage.getUser(1);
        acc = UserSteps.createAccount(user);
    }

    @UserSession
    @ParameterizedTest
    @ValueSource(doubles = {0.01, 5000.00})
    public void userCanDepositMaxOrMinTest(double amount) {
        new UiSteps(user).positiveDeposit((int) acc.getId(), acc.getAccountNumber(), String.valueOf(amount));

        double actualBalance =
                UserSteps.getAccBalance(user.getToken(), acc.getId());

        new DepositPage()
                .open()
                .verifyBalanceInSelector(acc.getAccountNumber(), actualBalance);
    }

    @Test
    @UserSession
    public void UserCanNotDepositLessMinTest() {
        new UiSteps(user).negativeDepositAssert((int) acc.getId(), acc.getAccountNumber(),
                String.valueOf(SumValues.LESSMIN.getValue()), AlertMessages.ENTER_VALID_AMOUNT);
    }

    @Test
    @UserSession
    public void UserCanNotDepositOverMaxTest() {
        new UiSteps(user).negativeDepositAssert((int) acc.getId(), acc.getAccountNumber(),
                String.valueOf(SumValues.OVERMAXDEPOSIT.getValue()), AlertMessages.DEPOSIT_LESS);
    }

    @Test
    @UserSession
    public void UserCanNotDepositWithNotSelectedAccountTest() {
        new UiSteps(user).negativeDepositAssert((int) acc.getId(), "Choose an account",
                String.valueOf(SumValues.SOMEDEPOSIT.getValue()), AlertMessages.SELECT_ACCOUNT);
    }

    @Test
    @UserSession
    public void UserCanNotDepositWithNotSelectedAmountTest() {
        new UiSteps(user).negativeDepositAssert((int) acc.getId(), acc.getAccountNumber(),
                null, AlertMessages.ENTER_VALID_AMOUNT);
    }

    @Disabled("нет валидации в поле суммы")
    @UserSession
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

        DepositPage depositPage = new DepositPage().open();
        depositPage.setAmount(input);
        depositPage.getAmountInput().shouldHave(exactValue(validatedInput));

    }
}
