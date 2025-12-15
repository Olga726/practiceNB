package api.steps;

import api.models.UserModel;
import lombok.Getter;
import ui.iteration2.pages.*;

import java.math.BigDecimal;
import java.math.RoundingMode;

import static org.junit.jupiter.api.Assertions.assertEquals;

@Getter
public class UiSteps {
    private UserModel sender;

    public UiSteps(UserModel sender) {
        this.sender = sender;
    }

    public void positiveDeposit(
            int accId,
            String accNumber,
            String sum) {
        double initialBalance = UserSteps.getAccBalance(sender.getToken(), accId);

        new DepositPage().open()
                .deposit(accNumber, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_DEPOSITED)
                .getPage(DashboardPage.class);

        double finalBalance = UserSteps.getAccBalance(sender.getToken(), accId);
        finalBalance = new BigDecimal(finalBalance)
                .setScale(2, RoundingMode.HALF_UP)
                .doubleValue();

        assertEquals(initialBalance + Double.parseDouble(sum), finalBalance, 0.001f);

    }

    public void negativeDepositAssert(
            int accId,
            String accNumber,
            String sum,
            AlertMessages message) {
        double initialBalance = UserSteps.getAccBalance(sender.getToken(), accId);

        new DepositPage().open()
                .deposit(accNumber, sum)
                .checkAlertAndConfirm(message)
                .getPage(DepositPage.class);

        assertEquals(initialBalance, UserSteps.getAccBalance(sender.getToken(), accId), 0.001f);

    }


}
