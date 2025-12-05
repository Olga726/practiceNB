package ui.iteration2.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;

import java.nio.channels.Selector;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;

public class DepositPage extends BasePage<DepositPage>{
    private SelenideElement depositButton = $$("button").findBy(text("Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public DepositPage deposit(String accNumber, String amount){
        accountSelector.$$("option").findBy(Condition.text(accNumber)).click();
        amountInput.setValue(amount);
        depositButton.click();
        return this;
    }

    public DepositPage setAmount(String amount){
        amountInput.setValue(amount);
        return this;
    }
}
