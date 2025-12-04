package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.*;
import api.iteration2.specs.RequestSpecs;
import api.iteration2.specs.ResponseSpecs;
import com.codeborne.selenide.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
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
import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferUiTest {
    private UserModel user1;
    private UserModel user2;
    private int user1acc1Id;
    private String user1acc1Number;
    private String user1acc2Number;
    private String user1name;
    private String user2Name;
    private int user2acc1Id;
    private String user2acc1Number;
    private int user1acc2Id;
    private int user2acc2Id;

    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = "http://localhost:4444/wd/hub";
        Configuration.baseUrl = "http://192.168.1.51";
        Configuration.browser = "chrome";
        Configuration.browserSize = "1928x1080";


        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));

    }

    @BeforeEach
    public void createUserAndAccount() {
        user1 = UserSteps.createUser();
        Account user1acc1 = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .post("http://localhost:4111/api/v1/accounts")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .as(Account.class);
        user1acc1Id = (int) user1acc1.getId();
        user1acc1Number = user1acc1.getAccountNumber();
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        UserSteps.deposit(user1.getToken(), user1acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());
        Account user1acc2 = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .post("http://localhost:4111/api/v1/accounts")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .as(Account.class);
        user1acc2Id = (int) user1acc2.getId();
        user1acc2Number = user1acc2.getAccountNumber();
        user1name = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .body("""
                        {
                        "name": "Sam Smith"
                        }
                        """
                )
                .put("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("customer.name");


        user2 = UserSteps.createUser();
        user2Name = given()
                .spec(RequestSpecs.authSpec(user2.getToken()))
                .body("""
                        {
                        "name": "Sam Smith"
                        }
                        """
                )
                .put("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("customer.name");

        Account user2acc1 = given()
                .spec(RequestSpecs.authSpec(user2.getToken()))
                .post("http://localhost:4111/api/v1/accounts")
                .then().assertThat()
                .statusCode(201)
                .extract()
                .as(Account.class);
        user2acc1Id = (int) user2acc1.getId();
        user2acc1Number = user2acc1.getAccountNumber();

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToAnotherUserAccTest(double sum) {
        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Id
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        String formatted = String.format(Locale.US, "%.2f", sum);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(formatted);
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", finalSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", finalSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);

        //проверка изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1 - sum, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1 + sum, roundedUser2Acc1Balance, 0.0001f);

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
        Selenide.open("");

        //шаг 1. пользователь логинится и переходит на вкладку Трансфер
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер вводит в поле amount невалидные значения
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(inputValue);

        //отображаются валидные значения
        $x("//input[contains(@placeholder,'Enter amount')]").shouldHave(exactValue(validatedInputValue));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "0.01",
            "10000.00"
    })
    public void userCanTransferToOwnAccTest(double sum) {
        Selenide.open("");

        double initialSumAcc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumAcc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод себе на другой счет
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user1name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user1acc2Number);
        String formatted = String.format(Locale.US, "%.2f", sum);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(formatted);
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui поменялись
        double finalSumAcc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumAcc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);
        refresh();

        String formattedSumAcc1 = String.format(Locale.US, "%.2f", finalSumAcc1);
        String formattedSumAcc2 = String.format(Locale.US, "%.2f", finalSumAcc2);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumAcc1 + ")"))
                .should(Condition.exist);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc2Number + " (Balance: $" + formattedSumAcc2 + ")"))
                .should(Condition.exist);


        //проверка изменений сумм на счетах
        BigDecimal balanceAcc1 = BigDecimal.valueOf(finalSumAcc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedAcc1Balance = balanceAcc1.doubleValue();

        BigDecimal balanceAcc2 = BigDecimal.valueOf(finalSumAcc2)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedAcc2Balance = balanceAcc2.doubleValue();

        assertEquals(initialSumAcc1 - sum, roundedAcc1Balance, 0.0001f);
        assertEquals(initialSumAcc2 + sum, roundedAcc2Balance, 0.0001f);
    }

    @Test
    public void userCanNotTransferLessMinToAnotherUserAccTest() {
        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(0.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(" Error: Transfer amount must be at least 0.01");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсуствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferOverMaxToAnotherUserAccTest() {
        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.01));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(" Error: Transfer amount cannot exceed 10000");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсуствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanTransferToSameAccTest() {
        Selenide.open("");

        double initialSumAcc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод себе на тот же счет
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user1name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user1acc1Number);
        String formatted = String.format(Locale.US, "%.2f", 10000.00);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(formatted);
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы баланс на ui не поменялся
        double finalSumAcc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        String formattedSumAcc1 = String.format(Locale.US, "%.2f", finalSumAcc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumAcc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений суммы на счете
        BigDecimal balanceAcc1 = BigDecimal.valueOf(finalSumAcc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedAcc1Balance = balanceAcc1.doubleValue();

        assertEquals(initialSumAcc1, roundedAcc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferOverBalanceTest() {
        Selenide.open("");

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number (2 раза т.к. начальный баланс был 15000)
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Successfully transferred");
        alert.accept();

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert1 = switchTo().alert();
        assertThat(alert1.getText()).contains("Error: Invalid transfer: insufficient funds or invalid accounts");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToInvalidRecipientNameTest() {
        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue("Taylor Swift");
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("The recipient name does not match the registered name.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToInvalidRecipientAccTest() {
        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Id
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue("ACC0000");
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("No user found with this account number.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToWrongRecipientAccTest() {

        //имя user2
        user2Name = given()
                .spec(RequestSpecs.authSpec(user2.getToken()))
                .body("""
                        {
                        "name": "John Snow"
                        }
                        """
                )
                .put("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("customer.name");

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user1acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user1acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("The recipient name does not match the registered name.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        //проверка ui user2 - там баланс не изменился

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToWrongRecipientNameTest() {

        //имя user2
        user2Name = given()
                .spec(RequestSpecs.authSpec(user2.getToken()))
                .body("""
                        {
                        "name": "John Snow"
                        }
                        """
                )
                .put("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("customer.name");

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Id
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user1name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("The recipient name does not match the registered name.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        //проверка ui user2 - там баланс не изменился
        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferWithEmptyFieldsTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает перевод

        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        //проверка отсутствия изменений сумм на счете
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToEmptyRecipientNameTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        //проверка ui баланса user2
        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferNullTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient name')]").setValue(user2Name);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);

        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }


    @Test
    public void userCanNotTransferWithEmptySenderAccTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number
        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferToEmptyRecipientAccTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод без указания счета получателя
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));
        $(Selectors.byId("confirmCheck")).setSelected(true);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanNotTransferWithNotSelectedCheckboxTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //проверка элементов страницы
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $x("//input[contains(@placeholder,'Enter recipient name')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter recipient account number')]").shouldBe(visible);
        $x("//input[contains(@placeholder,'Enter amount')]").shouldBe(visible);
        $(Selectors.byLabel("Confirm details are correct")).shouldBe(visible);
        $(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //юзер делает max перевод на user2acc1Number без подянтого чекбокса
        $(".form-control.account-selector")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();

        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text(user1acc1Number));

        $x("//input[contains(@placeholder,'Enter recipient account number')]").setValue(user2acc1Number);
        $x("//input[contains(@placeholder,'Enter amount')]").setValue(String.valueOf(10000.00));

        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please fill all fields and confirm.");
        alert.accept();

        // после перевода остались на той же вкладке
        $$("button").findBy(text("\uD83D\uDE80 Send Transfer")).shouldBe(visible);

        //после обновления страницы балансы на ui не поменялись
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);
        refresh();

        String formattedSumUser1Acc1 = String.format(Locale.US, "%.2f", initialSumUser1Acc1);
        String formattedSumUser2Acc1 = String.format(Locale.US, "%.2f", initialSumUser2Acc1);

        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user1acc1Number + " (Balance: $" + formattedSumUser1Acc1 + ")"))
                .should(Condition.exist);

        $$("button").findBy(text("\uD83D\uDEAA Logout")).click();
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user2.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user2.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(Selectors.byText("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();
        $(".form-control.account-selector option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        $(".form-control.account-selector").$$("option")
                .findBy(Condition.text(user2acc1Number + " (Balance: $" + formattedSumUser2Acc1 + ")"))
                .should(Condition.exist);


        //проверка отсутствия изменений сумм на счетах
        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser2Acc1Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1, roundedUser2Acc1Balance, 0.0001f);

    }

    @Test
    public void userCanRepeatTransferToOwnAccountTest() {
        //депозит на 0.02$
        UserSteps.deposit(user1.getToken(), user1acc2Id, SumValues.SOMEDEPOSIT, ResponseSpecs.success());

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser1Acc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        //шаг 1. пользователь логинится и переходит на вкладку Make a Transfer
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        //переходит на Make a Transfer
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //переходит на Transfer Again
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(visible);

        //нажимает на Repeat с депозитом
        $$("li.list-group-item")
                .filterBy(Condition.text("DEPOSIT - $0.02"))
                .first()
                .$x(".//button[contains(., 'Repeat')]")
                .click();


        //проверка элементов модального окна
        SelenideElement modalRepeatTransfer = $("[role='dialog']")
                .shouldHave(Condition.text("\uD83D\uDD01 Repeat Transfer"))
                .shouldBe(Condition.visible);
        modalRepeatTransfer.$(".modal-body p strong").shouldHave(Condition.exactText(String.valueOf(user1acc2Id)));
        modalRepeatTransfer.$(".form-control option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        modalRepeatTransfer.$x(".//label[normalize-space()='Amount:']/following-sibling::input")
                .shouldHave(value("0.02"));
        modalRepeatTransfer.$(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        modalRepeatTransfer.$$("button").findBy(text("Cancel")).shouldBe(visible);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer"))
                .shouldBe(visible).shouldBe(Condition.disabled);

        //заполнение формы - юзер переводит ту же сумму что была в депозите на тот же счет user1acc2Id
        // с другого своего счета user1acc1Number:
        modalRepeatTransfer.$(".form-control")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();
        $(Selectors.byId("confirmCheck")).setSelected(true);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer"))
                .shouldBe(visible).shouldBe(enabled);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Transfer of $" + "0.02" + " successful from Account "
                + user1acc1Id + " to " + user1acc2Id + "!");
        alert.accept();
        $(Selectors.byText("Matching Transactions")).shouldBe(visible);
        refresh();
        //после обновления стр почему-то оказываемся на New Transfer
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).click();

        //проверка что в Matching Transactions появились TRANSFER_IN и TRANSFER_OUT
        $$("li.list-group-item")
                .findBy(Condition.text("TRANSFER_IN - $0.02"))
                .shouldBe(visible);

        $$("li.list-group-item")
                .findBy(Condition.text("TRANSFER_OUT - $0.02"))
                .shouldBe(visible);

        //проверка что баланс Acc1 уменьшился, а Acc2 увеличился на 0.02
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser1Acc2 = UserSteps.getAccBalance(user1.getToken(), user1acc2Id);

        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser1Acc2 = BigDecimal.valueOf(finalSumUser1Acc2)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc2Balance = balanceUser1Acc2.doubleValue();

        assertEquals(initialSumUser1Acc1 - 0.02, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser1Acc2 + 0.02, roundedUser1Acc2Balance, 0.0001f);

    }

    @Test
    public void userCanRepeatTransferToAnotherUserAccountTest() {

        Selenide.open("");

        double initialSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double initialSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        //шаг 1. пользователь логинится
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        //переходит на Make a Transfer
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD04 Make a Transfer")).click();

        //делает перевод 5000 на user2acc1Id
        UserSteps.transferSuccessResponse(
                user1.getToken(), user1acc1Id, user2acc1Id, SumValues.MAXDEPOSIT, ResponseSpecs.success());

        //переходит на Transfer Again
        refresh();
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).click();
        $(Selectors.byText("Matching Transactions")).shouldBe(visible);
        //нажимает на Repeat с депозитом (на стр 3 одинаковых депозита)
        $$("li.list-group-item")
                .filterBy(Condition.text("TRANSFER_OUT - $5000.00"))
                .first()
                .$x(".//button[contains(., 'Repeat')]")
                .click();

        //проверка элементов модального окна: в т.ч. в Confirm transfer to Account ID user2acc1Id
        SelenideElement modalRepeatTransfer = $("[role='dialog']")
                .shouldHave(Condition.text("\uD83D\uDD01 Repeat Transfer"))
                .shouldBe(Condition.visible);
        modalRepeatTransfer.$(".modal-body p strong").shouldHave(Condition.exactText(String.valueOf(user2acc1Id)));
        modalRepeatTransfer.$(".form-control option:checked")
                .shouldHave(Condition.text("-- Choose an account --"));
        modalRepeatTransfer.$x(".//label[normalize-space()='Amount:']/following-sibling::input")
                .shouldHave(value("5000"));
        modalRepeatTransfer.$(Selectors.byId("confirmCheck")).shouldNotBe(selected);
        modalRepeatTransfer.$$("button").findBy(text("Cancel")).shouldBe(visible);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer"))
                .shouldBe(visible).shouldBe(Condition.disabled);

        int transactionsCount1 = $$("ul.list-group li").size();

        //заполнение формы - user1 переводит ту же сумму что была на счет user2acc1Id
        modalRepeatTransfer.$(".form-control")
                .$$("option")
                .findBy(Condition.text(user1acc1Number)).click();
        $(Selectors.byId("confirmCheck")).setSelected(true);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer"))
                .shouldBe(visible).shouldBe(enabled);
        modalRepeatTransfer.$$("button").findBy(text("\uD83D\uDE80 Send Transfer")).click();

        //проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Transfer of $" + "5000.00" + " successful from Account "
                + user1acc1Id + " to " + user2acc1Id + "!");
        alert.accept();
        $(Selectors.byText("Matching Transactions")).shouldBe(visible);
        refresh();

        //после обновления стр почему-то оказываемся на New Transfer
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDD01 Transfer Again")).click();

        //проверка что в Matching Transactions добавился 1 элемент
        int transactionsCount2 = $$("ul.list-group li").size();
        assertEquals(transactionsCount1 + 1, transactionsCount2);

        $$("ul.list-group li").last().shouldHave(Condition.text("TRANSFER_OUT - $5000.00"));

        //проверка что баланс Acc1 user1 уменьшился, а Acc1 user2 увеличился на 5000
        double finalSumUser1Acc1 = UserSteps.getAccBalance(user1.getToken(), user1acc1Id);
        double finalSumUser2Acc1 = UserSteps.getAccBalance(user2.getToken(), user2acc1Id);

        BigDecimal balanceUser1Acc1 = BigDecimal.valueOf(finalSumUser1Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc1Balance = balanceUser1Acc1.doubleValue();

        BigDecimal balanceUser2Acc1 = BigDecimal.valueOf(finalSumUser2Acc1)
                .setScale(2, RoundingMode.HALF_UP);
        double roundedUser1Acc2Balance = balanceUser2Acc1.doubleValue();

        assertEquals(initialSumUser1Acc1 - 5000.00, roundedUser1Acc1Balance, 0.0001f);
        assertEquals(initialSumUser2Acc1 + 5000.00, roundedUser1Acc2Balance, 0.0001f);

    }

}




