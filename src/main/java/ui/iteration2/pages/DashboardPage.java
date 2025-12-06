package ui.iteration2.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class DashboardPage extends BasePage<DashboardPage>{
    private SelenideElement headerUserDashboard = $(Selectors.byText("User Dashboard"));
    private SelenideElement welcomeText = $(".welcome-text");
    private SelenideElement depositMoneyButton = $$("button").findBy(text("\uD83D\uDCB0 Deposit Money"));
    private SelenideElement makeTransferButton = $$("button").findBy(text("\uD83D\uDD04 Make a Transfer"));
    private SelenideElement createAccountButton = $$("button").findBy(text("➕ Create New Account"));


    @Override
    public String url() {
        return "/dashboard";
    }

    public DashboardPage openEditProfile(){
        super.userInfo.click();
        return this;
    }

    public DashboardPage openTransferPage(){
        makeTransferButton.click();
        return this;
    }



}
