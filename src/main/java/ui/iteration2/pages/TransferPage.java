package ui.iteration2.pages;

import com.codeborne.selenide.Condition;
import com.codeborne.selenide.ElementsCollection;
import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import com.codeborne.selenide.conditions.Attribute;
import lombok.Getter;

import java.util.List;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class TransferPage extends BasePage<TransferPage> {
    private SelenideElement newTransferButton = $$("button").findBy(text("\uD83C\uDD95 New Transfer"));
    private SelenideElement transferAgainButton = $$("button").findBy(text("\uD83D\uDD01 Transfer Again"));
    private SelenideElement searchTransactionButton = $$("button").findBy(text("\uD83D\uDD0D Search Transactions"));
    private SelenideElement sendTransferButton = $$("button").findBy(text("\uD83D\uDE80 Send Transfer"));
    private SelenideElement recipientNameInput = $(Selectors.byAttribute("placeholder", "Enter recipient name"));
    private SelenideElement recipientAccountInput = $(Selectors.byAttribute("placeholder", "Enter recipient account number"));
    private SelenideElement searchByUsernamOrNameInput = $(Selectors.byAttribute("placeholder", "Enter name to find transactions"));
    private SelenideElement confirmCheckbox = $(Selectors.byId("confirmCheck"));
    private SelenideElement matchingTransactionsHeader = $(Selectors.byText("Matching Transactions"));
    private ElementsCollection matchingTransactionsItems = $$("li.list-group-item");
    private SelenideElement modalRepeatTransfer = $("[role='dialog']");
    private SelenideElement modalRepeatTransferToAccountId = $("[role='dialog']").$(".modal-body p strong");
    private SelenideElement modalRepeatTransferAccountSelector = $("[role='dialog']").$(".form-control");
    private SelenideElement modalRepeatTransferAmountInput = $("[role='dialog']").$x(".//label[normalize-space()='Amount:']/following-sibling::input");
    private SelenideElement modalRepeatConfirmCheckbox= $("[role='dialog']").$(Selectors.byId("confirmCheck"));
    private SelenideElement modalRepeatCancelButton= $("[role='dialog']").$$("button").findBy(text("Cancel"));
    private SelenideElement modalRepeatSendTransferButton= $("[role='dialog']").$$("button").findBy(text("\uD83D\uDE80 Send Transfer"));

    @Override
    public String url() {
        return "/transfer";
    }

    public TransferPage transferAmount(String transferAccont, String recipientName,
                                       String recipientAccont, String amount) {
        super.accountSelector.$$("option").findBy(Condition.text(transferAccont)).click();
        recipientNameInput.setValue(recipientName);
        recipientAccountInput.setValue(recipientAccont);
        super.amountInput.setValue(amount);
        confirmCheckbox.setSelected(true);
        sendTransferButton.click();
        return this;

    }


    public TransferPage clickSendTransfer() {
        sendTransferButton.click();
        return this;
    }

    public TransferPage transferWithoutCheckbox(String transferAccont, String recipientName,
                                                String recipientAccont, String amount) {
        super.accountSelector.$$("option").findBy(Condition.text(transferAccont)).click();
        recipientNameInput.setValue(recipientName);
        recipientAccountInput.setValue(recipientAccont);
        super.amountInput.setValue(amount);
        sendTransferButton.click();
        return this;

    }

    public TransferPage goToTransferAgain() {
        transferAgainButton.click();
        return this;
    }

    public SelenideElement repeatButtonInMatchingTransactions(String itemText) {
        SelenideElement el = matchingTransactionsItems.stream()
                .filter(i -> i.has(Condition.text(itemText)))
                .findFirst()
                .orElseThrow();

        return el.$x(".//button[contains(., 'Repeat')]");

    }


    public TransferPage repeatTransfer(String account){
        modalRepeatTransferAccountSelector.$$("option")
                .findBy(Condition.text(account)).click();
        modalRepeatConfirmCheckbox.setSelected(true);
        modalRepeatSendTransferButton.click();
        return this;
    }


}
