package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.UserModel;
import api.iteration2.specs.RequestSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import org.apache.hc.core5.http.HttpStatus;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.openqa.selenium.Alert;

import java.util.Map;

import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserNameChangeUITest {
    private static UserModel user1;

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
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Johny Donny",
            "NAOMI K",
            "l v",
            "Wolfeschlegelsteinhausenbergerdorff Ninachinmacdholicachinskerray"
    })
    public void userCanChangeNameWithValidDataTest(String name) {
        Selenide.open("");

        //шаг 1. пользователь логинится и переходит на вкладку редактирования профиля
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));
        $(".user-username").shouldBe(visible).shouldHave(text("@" + user1.getUsername()));

        $(".user-name").shouldBe(visible).click();

        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(visible);
        $(Selectors.byPlaceholder("Enter new name")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).shouldBe(visible);

        //шаг 2. пользователь вводит новое имя
        $(Selectors.byPlaceholder("Enter new name")).setValue(name);

        //шаг 3. пользователь нажимает "Save Changes"
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).click();

        //шаг 4. проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Name updated successfully!");
        alert.accept();
        sleep(500);

        //шаг 5. обновление страницы
        refresh();

        //шаг 6. проверка, что новое имя отображается на ui
        $(".user-name").shouldBe(visible).shouldHave(text(name));
        $(Selectors.byPlaceholder("Enter new name")).shouldHave(value(name));

        //шаг 7. Проверка API что имя обновилось в профиле
        String customerName = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("name");


        assertEquals(name, customerName);

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "George 1",
            "Van De Fry",
            "Ivi  Smith",
            "= /*-<>:;'.?!@#$%^^&*()+-",
            "Li",
            "1"
    })
    public void userCanNotChangeNameWithValidDataTest(String name) {
        Selenide.open("");

        //шаг 1. пользователь логинится и переходит на вкладку редактирования профиля
        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));
        $(".user-username").shouldBe(visible).shouldHave(text("@" + user1.getUsername()));

        $(".user-name").shouldBe(visible).click();

        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(visible);
        $(Selectors.byPlaceholder("Enter new name")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).shouldBe(visible);

        //шаг 2. пользователь вводит новое имя
        $(Selectors.byPlaceholder("Enter new name")).setValue(name);

        //шаг 3. пользователь нажимает "Save Changes"
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).click();

        //шаг 4. проверка алерта
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Name must contain two words with letters only");
        alert.accept();

        //шаг 5. обновление страницы
        refresh();

        //шаг 6. проверка, что старое имя Noname отображается на ui
        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));

        //шаг 7. Проверка API что имя пустое
        String customerName = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("name");

        assertNull(customerName);

    }

    @Test
    public void userCanNotChangeNameWithoutEnterInputTest() {
        Selenide.open("");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));
        $(".user-username").shouldBe(visible).shouldHave(text("@" + user1.getUsername()));

        $(".user-name").shouldBe(visible).click();

        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(visible);
        $(Selectors.byPlaceholder("Enter new name")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).shouldBe(visible);

        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please enter a valid name.");
        alert.accept();

        refresh();

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));

        String customerName = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("name");

        assertNull(customerName);
    }

    @Test
    public void userCanNotChangeNameToOnlySpacesTest() {
        Selenide.open("");

        $(Selectors.byAttribute("placeholder", "Username")).sendKeys(user1.getUsername());
        $(Selectors.byAttribute("placeholder", "Password")).sendKeys(user1.getPassword());
        $("button").click();
        $(Selectors.byText("User Dashboard")).shouldBe(visible);

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));
        $(".user-username").shouldBe(visible).shouldHave(text("@" + user1.getUsername()));

        $(".user-name").shouldBe(visible).click();

        $(Selectors.byText("✏\uFE0F Edit Profile")).shouldBe(visible);
        $(Selectors.byPlaceholder("Enter new name")).shouldBe(visible);
        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).shouldBe(visible);

        $(Selectors.byPlaceholder("Enter new name")).setValue("   ");

        $$("button").findBy(text("\uD83D\uDCBE Save Changes")).click();

        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains("Please enter a valid name.");
        alert.accept();

        refresh();

        $(".user-name").shouldBe(visible).shouldHave(text("Noname"));

        String customerName = given()
                .spec(RequestSpecs.authSpec(user1.getToken()))
                .get("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .path("name");

        assertNull(customerName);
    }
}
