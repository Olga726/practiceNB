package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.SumValues;
import api.iteration2.models.UserModel;
import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.Alert;


import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Locale;
import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selectors.byAttribute;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositUiTest {
    private static UserModel user1;
    private static int accId;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.1.51";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1928x1080";


        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));

    }

    @BeforeAll
    public static void createUserAndAccount() {
        user1 = UserSteps.createUser();
        accId = (int) UserSteps.createAccountAndGetId(user1.getToken());
    }

    @ParameterizedTest
    @ValueSource(doubles = {0.01, 5000.00})
    public void userCanDepositMaxOrMinTest(double amount) {
        Selenide.open("");

        //шаг 1. пользователь логинится и переходит на вкладку Депозит
        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        //шаг 2. пользователь выбирает счет
        $(".form-control.account-selector").selectOption(1);

        //шаг 3. пользователь указывает сумму депозита
        double initialSum = UserSteps.getAccBalance(user1.getToken(), accId);
        String formatted = String.format(Locale.US, "%.2f", amount);
        $(byAttribute("placeholder", "Enter amount")).setValue(formatted);

        //шаг 4. пользователь нажимает кнопку Deposit
        $$("button").findBy(text("Deposit")).click();

        //шаг 5. проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully deposited");
        alert.accept();

        //шаг 6. проверка, что после закрытия алерта пользователь на вкладке User Dashboard
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        //шаг 7. API проверка, что баланс счета изменился на сумму депозита
        double finalSumAcc1 = UserSteps.getAccBalance(user1.getToken(), accId);
        BigDecimal balance = BigDecimal.valueOf(finalSumAcc1)
                .setScale(2, RoundingMode.HALF_UP);
        double rounded = balance.doubleValue();
        assertEquals(initialSum + amount, rounded, 0.0001f);

    }

    @Test
    public void UserCanNotDepositLessMinTest() {
        Selenide.open("");

        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        $(".form-control.account-selector").selectOption(1);

        double initialSum = UserSteps.getAccBalance(user1.getToken(), accId);
        $(byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(SumValues.LESSMIN));

        $$("button").findBy(text("Deposit")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please enter a valid amount.");
        alert.accept();

        $$("button").findBy(text("Deposit")).shouldBe(visible);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), accId), 0.0001);

    }

    @Test
    public void UserCanNotDepositOverMaxTest() {
        Selenide.open("");

        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        $(".form-control.account-selector").selectOption(1);

        double initialSum = UserSteps.getAccBalance(user1.getToken(), accId);
        $(byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(5000.01));

        $$("button").findBy(text("Deposit")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please deposit less or equal to 5000$.");
        alert.accept();

        $$("button").findBy(text("Deposit")).shouldBe(visible);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), accId), 0.0001);

    }

    @Test
    public void UserCanNotDepositWithNotSelectedAccountTest() {
        Selenide.open("");

        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        double initialSum = UserSteps.getAccBalance(user1.getToken(), accId);
        $(byAttribute("placeholder", "Enter amount")).setValue(String.valueOf(1));

        $$("button").findBy(text("Deposit")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please select an account.");
        alert.accept();

        $$("button").findBy(text("Deposit")).shouldBe(visible);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), accId), 0.0001);
    }

    @Test
    public void UserCanNotDepositWithNotSelectedAmountTest() {
        Selenide.open("");

        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        $(".form-control.account-selector").selectOption(1);

        double initialSum = UserSteps.getAccBalance(user1.getToken(), accId);

        $$("button").findBy(text("Deposit")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please enter a valid amount.");
        alert.accept();
        $$("button").findBy(text("Deposit")).shouldBe(visible);

        assertEquals(initialSum, UserSteps.getAccBalance(user1.getToken(), accId), 0.0001);
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
        Selenide.open("");

        $(byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);
        $(Selectors.byText("\uD83D\uDCB0 Deposit Money")).shouldBe(visible);
        $$("button").findBy(text("💰 Deposit Money")).click();

        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(input);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldHave(exactValue(validatedInput));


    }

}
