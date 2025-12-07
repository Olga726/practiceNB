package ui.iteration2.pages;

import com.codeborne.selenide.*;
import lombok.Getter;
import org.openqa.selenium.Alert;

import java.util.Locale;

import static com.codeborne.selenide.CollectionCondition.sizeGreaterThan;
import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.*;
import static org.assertj.core.api.Assertions.assertThat;

@Getter
public abstract class BasePage<T extends BasePage> {
    protected SelenideElement userInfo = $(".user-info");
    protected SelenideElement userName = $(".user-name");
    protected SelenideElement userUserName = $(".user-username");

    protected SelenideElement accountSelector = $(".form-control.account-selector");
    protected SelenideElement amountInput =  $(Selectors.byAttribute("placeholder", "Enter amount"));

    public abstract String url();

    public T open(){
        return Selenide.open(url(), (Class<T>) this.getClass());
    }

    public <T extends BasePage> T getPage(Class<T> pageClass){
        return Selenide.page(pageClass);
    }

    public T checkAlertAndConfirm(AlertMessages message){
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(message.getMessage());
        alert.accept();
        return (T) this;
    }

    public T setAmount(String amount){
        amountInput.setValue(amount);
        return (T) this;
    }

    public T checkAlertWithArgsAndConfirm(AlertMessages message, Object... args){
        Alert alert = switchTo().alert();
        assertThat(alert.getText()).contains(message.format(args));
        alert.accept();
        return (T) this;
    }

    public void verifyBalanceInSelector(String accNumber, double balance) {
        String formatted = String.format(Locale.US, "%.2f", balance);
        String expected = accNumber + " (Balance: $" + formatted + ")";

        accountSelector.$$("option").shouldHave(sizeGreaterThan(0));

        accountSelector.$$("option")
                .findBy(text(expected))
                .should(Condition.exist);
    }

}
