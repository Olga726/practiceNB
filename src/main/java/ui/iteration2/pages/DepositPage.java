package ui.iteration2.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import ui.iteration2.elements.AccountSelector;

import java.nio.channels.Selector;
import java.util.List;
import java.util.stream.Collectors;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.*;

public class DepositPage extends BasePage<DepositPage>{
    private SelenideElement depositButton = $$("button").findBy(text("Deposit"));

    @Override
    public String url() {
        return "/deposit";
    }

    public List<AccountSelector> getAllAccounts() {
        ElementsCollection elementsCollection = $(".form-control.account-selector").$$("option");

            return elementsCollection.stream()
                    .filter(option -> !option.getAttribute("value").isEmpty())
                    .map(AccountSelector::new)
                    .collect(Collectors.toList());


    }


    public DepositPage deposit(String accNumber, String amount){
        accountSelector.$$("option").findBy(Condition.text(accNumber)).click();
        amountInput.setValue(amount);
        depositButton.click();
        return this;
    }
/*
    public DepositPage setAmount(String amount){
        amountInput.setValue(amount);
        amountInput.pressTab();
        return this;
    }

 */
}
