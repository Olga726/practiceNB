package api.steps;

import api.models.SumValues;
import api.models.UserModel;
import com.codeborne.selenide.Condition;

import lombok.Getter;
import org.assertj.core.api.SoftAssertions;
import ui.iteration2.pages.*;

import java.math.BigDecimal;
import java.math.RoundingMode;


import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Condition.visible;
import static com.codeborne.selenide.Selenide.refresh;
import static org.assertj.core.api.Assertions.within;
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

    public void negativeTransferWithParams(
            String senderAccNumber,
            String senderAccNumberForUiCheck,
            int senderAccId,
            String recipientName,
            UserModel recipient,
            String recipientAccNumber,
            int recipientAccId,
            String sum,
            AlertMessages alertMessage,
            double initialSenderBalance,
            double initialRecipientBalance
    ){

        SoftAssertions softly = new SoftAssertions();
        TransferPage transferPage= new TransferPage().open().getPage(TransferPage.class);

        String alertText = transferPage.transferWithParams(senderAccNumber, recipientName,
                        recipientAccNumber, sum)
                .getAlertTextAndConfirm();

        softly.assertThat(alertText).contains(
                alertMessage.getMessage());

        //проверка что на ui баланс не изменился
        double actualBalance = new TransferPage().open().getBalance(senderAccNumberForUiCheck);
        softly.assertThat(actualBalance)
                .isEqualTo(initialSenderBalance);

        //проверка API отсуствия изменений сумм на счетах
        softly.assertThat(initialSenderBalance).isCloseTo(UserSteps.getAccBalance(sender.getToken(), senderAccId), within(0.0001));
        softly.assertThat(initialRecipientBalance).isCloseTo(UserSteps.getAccBalance(recipient.getToken(), recipientAccId), within(0.0001));
        softly.assertAll();

    }

    public void positiveRepeatingTransfer(
            String transactionNameForRepeat,
            UserModel recipient,
            int recipientAccId,
            int senderAccId,
            SumValues sum,
            String senderAccNumber,
            BigDecimal initialSenderAccBalance,
            BigDecimal initialRecipientAccBalance,
            int transactionsCountIncrease

    ){
        SoftAssertions softly = new SoftAssertions();
        //переход на Transfer Again
        TransferPage transferPage = new TransferPage().open().goToTransferAgain();
        transferPage.getMatchingTransactionsHeader().shouldBe(visible);

        int transactionsQuantity = transferPage.getMatchingTransactionsItems().size();
        transferPage.repeatButtonInMatchingTransactions(transactionNameForRepeat).click();

        transferPage.getModalRepeatTransfer().shouldHave(Condition.text("\uD83D\uDD01 Repeat Transfer"))
                .shouldBe(Condition.visible);
        transferPage.getModalRepeatTransferToAccountId().shouldHave(Condition.exactText(String.valueOf(recipientAccId)));
        transferPage.getModalRepeatTransferAccountSelector().shouldHave(Condition.text("-- Choose an account --"));
        String expectedAmount = formatAmount(sum.getValue());
        transferPage.getModalRepeatTransferAmountInput().shouldHave(value(expectedAmount));
        transferPage.getModalRepeatConfirmCheckbox().shouldNotBe(selected);
        transferPage.getModalRepeatSendTransferButton().shouldBe(Condition.disabled);

        String alertText = transferPage.repeatTransfer(senderAccNumber)
                .getAlertTextAndConfirm();

        softly.assertThat(alertText).contains(
                AlertMessages.TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO.format(
                        expectedAmount,
                        senderAccId,
                        recipientAccId
                ));

        refresh();
        transferPage.goToTransferAgain();

        int transactionsFinalQuantity = transferPage.getMatchingTransactionsItems().size();
        softly.assertThat(transactionsFinalQuantity).isEqualTo(transactionsQuantity + transactionsCountIncrease);

        softly.assertThat(
                transferPage.getMatchingTransactionsItems().findBy(Condition.text("TRANSFER_OUT - $" + expectedAmount))
                        .exists());

        //проверка что баланс счета отправителя уменьшился, а баланс счета получателя увеличился
        BigDecimal expectedSenderBalance = initialSenderAccBalance
                .subtract(BigDecimal.valueOf(sum.getValue()))
                .setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualSenderBalance = BigDecimal.valueOf(
                UserSteps.getAccBalance(sender.getToken(), senderAccId)
        ).setScale(2, RoundingMode.HALF_UP);


        BigDecimal expectedRecipientBalance =
                initialRecipientAccBalance
                        .add(BigDecimal.valueOf(sum.getValue()))
                        .setScale(2, RoundingMode.HALF_UP);
        BigDecimal actualRecipientBalance =
                BigDecimal.valueOf(
                        UserSteps.getAccBalance(recipient.getToken(), recipientAccId)
                ).setScale(2, RoundingMode.HALF_UP);

        softly.assertThat(expectedSenderBalance).isEqualByComparingTo(actualSenderBalance);
        softly.assertThat(expectedRecipientBalance).isEqualByComparingTo(actualRecipientBalance);
        softly.assertAll();

    }

    private String formatAmount(double value) {
        BigDecimal bd = BigDecimal.valueOf(value).stripTrailingZeros();

        if (bd.scale() <= 0) {
            // целое число
            return bd.toPlainString();
        }

        // дробное — всегда 2 знака
        return bd.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

}
