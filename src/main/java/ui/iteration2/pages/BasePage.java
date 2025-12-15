package ui.iteration2.pages;


import api.specs.RequestSpecs;
import com.codeborne.selenide.*;
import lombok.Getter;
import org.openqa.selenium.Alert;
import ui.iteration2.elements.AccountSelector;
import ui.iteration2.elements.BaseElement;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.function.Function;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Getter
public abstract class BasePage<T extends BasePage> {
    protected SelenideElement userInfo = $(".user-info");
    protected SelenideElement userName = $(".user-name");
    public static final String DEFAULTUSER_NAME = "Noname";
    protected SelenideElement userUserName = $(".user-username");

    protected SelenideElement accountSelector = $(".form-control.account-selector");
    protected SelenideElement amountInput = $(Selectors.byAttribute("placeholder", "Enter amount"));

    public abstract String url();

    public T open() {
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass) {
        return Selenide.page(pageClass);
    }

    public T checkAlertAndConfirm(AlertMessages message) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(message.getMessage());
        alert.accept();
        return (T) this;
    }

    public T setAmount(String amount) {
        amountInput.setValue(amount);
        return (T) this;
    }

    public T checkAlertWithArgsAndConfirm(AlertMessages message, Object... args) {
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(message.format(args));
        alert.accept();
        return (T) this;
    }

    public void verifyBalanceInSelector(String accNumber, double balance) {
        String expectedBalance = BigDecimal.valueOf(balance)
                .setScale(2, RoundingMode.HALF_UP)
                .toPlainString();

        Selenide.Wait().until(driver ->
                getAccountSelectors().stream().anyMatch(acc ->
                        acc.getAccNumber().equals(accNumber) &&
                                acc.getAccBalance().equals(expectedBalance)
                )
        );
    }

    public static void authAsUser(String username, String password) {
        Selenide.open("/");
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.open("/");

    }

    public static void switchUser(String username, String password) {
        // 1️⃣ Очистка всего состояния браузера
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();

        // 2️⃣ Открываем базовую страницу приложения
        Selenide.open("/");

        // 3️⃣ Устанавливаем новый токен пользователя
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // 4️⃣ Обновляем страницу
        Selenide.refresh();

    }

    // ElementCollection -> List<BaseElement>
    protected <T extends BaseElement> List<T> generatePageElements(
            ElementsCollection elementsCollection,
            Function<SelenideElement, T> constructor) {
        return elementsCollection.stream().map(constructor).toList();
    }

    protected List<AccountSelector> getAccountSelectors() {
        accountSelector.shouldBe(visible);

        ElementsCollection options = accountSelector.$$("option")
                .filterBy(Condition.not(text("-- Choose an account --")))
                .shouldHave(sizeGreaterThan(0));

        if (options.isEmpty()) {
            throw new IllegalStateException("No accounts found in selector for user.");
        }
        return generatePageElements(options, AccountSelector::new);
    }
}
