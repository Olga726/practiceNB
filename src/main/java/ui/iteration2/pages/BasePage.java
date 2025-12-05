package ui.iteration2.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.Selenide;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;
import org.openqa.selenium.Alert;
import static org.assertj.core.api.Assertions.assertThat;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.switchTo;

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
}
