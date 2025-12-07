package ui.iteration2;
import api.iteration2.UserSteps;
import api.iteration2.models.UserModel;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.DashboardPage;
import ui.iteration2.pages.DepositPage;
import ui.iteration2.pages.TransferPage;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class UiSteps extends BaseUiTest {
    public static void negativeTransferAssert(
            String fromAcc,
            String toAcc,
            UserModel sender,
            UserModel recipient,
            String sum,
            int senderAccId,
            int recipientAccId,
            String recipientName,
            AlertMessages message,
            String fromAccInAssert,
            String toAccInAssert){
        double initialSenderBalance = UserSteps.getAccBalance(sender.getToken(), senderAccId);
        double initialRecipientBalance = UserSteps.getAccBalance(recipient.getToken(), recipientAccId);

        authAsUser(sender.getUsername(), sender.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(fromAcc, recipientName, toAcc, sum)
                .checkAlertAndConfirm(message)
                .getPage(TransferPage.class);

        //проверка что на ui балансы не изменились
        new TransferPage().open()
                .verifyBalanceInSelector(fromAccInAssert, initialSenderBalance);

        switchUser(recipient.getUsername(), recipient.getPassword());
        new TransferPage().open()
                .verifyBalanceInSelector(toAccInAssert, initialRecipientBalance);

        //проверка API отсуствия изменений сумм на счетах
        assertEquals(initialSenderBalance, UserSteps.getAccBalance(sender.getToken(), senderAccId), 0.001f);
        assertEquals(initialRecipientBalance, UserSteps.getAccBalance(recipient.getToken(), recipientAccId), 0.001f);

    }

    public static void positiveTransferAssert(
            String fromAcc,
            String toAcc,
            UserModel sender,
            UserModel recipient,
            String sum,
            int senderAccId,
            int recipientAccId,
            String recipientName,
            String fromAccInAssert,
            String toAccInAssert){
        double initialSenderBalance = UserSteps.getAccBalance(sender.getToken(), senderAccId);
        double initialRecipientBalance = UserSteps.getAccBalance(recipient.getToken(), recipientAccId);

        authAsUser(sender.getUsername(), sender.getPassword());
        new TransferPage().open().getPage(TransferPage.class)
                .transferAmount(fromAcc, recipientName, toAcc, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_TRANSFERED)
                .getPage(TransferPage.class);

        double finalSenderBalance = initialSenderBalance - Double.parseDouble(sum);
        double finalRecipientBalance = initialRecipientBalance + Double.parseDouble(sum);

        //проверка что на ui балансы изменились
        new TransferPage().open()
                .verifyBalanceInSelector(fromAccInAssert, finalSenderBalance);

        switchUser(recipient.getUsername(), recipient.getPassword());
        new TransferPage().open()
                .verifyBalanceInSelector(toAccInAssert, finalRecipientBalance);

        //проверка API изменений сумм на счетах
        assertEquals(finalSenderBalance, UserSteps.getAccBalance(sender.getToken(), senderAccId), 0.001f);
        assertEquals(finalRecipientBalance, UserSteps.getAccBalance(recipient.getToken(), recipientAccId), 0.001f);

    }

    public static void positiveDeposit(
            UserModel user,
            int accId,
            String accNumber,
            String sum){
        double initialBalance = UserSteps.getAccBalance(user.getToken(), accId);

        authAsUser(user.getUsername(), user.getPassword());
        new DepositPage().open()
                .deposit(accNumber, sum)
                .checkAlertAndConfirm(AlertMessages.SUCCESSFULLY_DEPOSITED)
                .getPage(DashboardPage.class);

        assertEquals(initialBalance+Double.parseDouble(sum), UserSteps.getAccBalance(user.getToken(), accId), 0.001f);

    }

    public static void negativeDepositAssert(
            UserModel user,
            int accId,
            String accNumber,
            String sum,
            AlertMessages message){
        double initialBalance = UserSteps.getAccBalance(user.getToken(), accId);

        authAsUser(user.getUsername(), user.getPassword());
        new DepositPage().open()
                .deposit(accNumber, sum)
                .checkAlertAndConfirm(message)
                .getPage(DepositPage.class);

        assertEquals(initialBalance, UserSteps.getAccBalance(user.getToken(), accId), 0.001f);

    }





}
