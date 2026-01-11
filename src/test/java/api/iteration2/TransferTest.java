package api.iteration2;

import api.models.SumValues;
import api.models.UserModel;
import api.specs.ResponseSpecs;
import api.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import io.restassured.specification.ResponseSpecification;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


@UserSession(value = 2)
public class TransferTest extends BaseTest {

    private UserModel user1;
    private UserModel user2;
    private long user1acc1Id;
    private long user1acc2Id;
    private long user2acc1Id;

    @BeforeEach
    public void preSteps() {
        user1 = SessionStorage.getUser(1);
        user2 = SessionStorage.getUser(2);
        user1acc1Id = UserSteps.createAccount(user1).getId();
        user1acc2Id = UserSteps.createAccount(user1).getId();
        user2acc1Id = UserSteps.createAccount(user2).getId();

    }

    //минимальный и максимальный перевод на свой счет
    @ParameterizedTest
    @CsvSource({
            "MINDEPOSIT, MINTRANSFER",
            "MAXDEPOSIT, MAXTRANSFER"
    })
    @Tag("with_database_with_fix")
    public void userCanTransferToOwnAccount(SumValues depositSum, SumValues transferSum){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 2);  //2 раза депозит
        UserSteps.transferAndAssert(softly, user1.getToken(), user1acc1Id, user1acc2Id, transferSum);

        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc2Id);
        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);

    }

    //минимальный и максимальный перевод на чужой счет
    @ParameterizedTest
    @CsvSource({
            "MINDEPOSIT, MINTRANSFER",
            "MAXDEPOSIT, MAXTRANSFER"
    })
    @Tag("with_database_with_fix")
    public void userCanTransferToAnotherUserAccount(SumValues depositSum, SumValues transferSum){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 2);     //2 раза депозит
        UserSteps.transferAndAssert(softly, user1.getToken(), user1acc1Id, user2acc1Id, transferSum);

        UserSteps.assertBalanceEqualsDB(user2.getToken(), user2acc1Id);
        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);
    }

    @Test
    @Tag("with_database_with_fix")
    public void userCanNotTransferFromAnotherUserAcc() {
        //пользователь2 делает перевод 10000 себе со счета пользователя1
        UserSteps.transferErrorResponse(
                user2.getToken(),
                user1acc1Id, user2acc1Id,
                SumValues.MAXTRANSFER,
                ResponseSpecs.unauthorized());

        UserSteps.assertBalanceEqualsDB(user2.getToken(), user2acc1Id);
        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);
    }

    @Test
    @Tag("with_database_with_fix")
    public void userCanTransferToTheSameAccount() {
        UserSteps.depositAndTransferSuccess(
                user1.getToken(),
                user1acc1Id, user1acc1Id,
                SumValues.MINTRANSFER);

        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);
    }

    @ParameterizedTest
    @MethodSource("transferInvalidData")
    @Tag("with_database_with_fix")
    public void userCanNotTransferOverMaxOrLessMin(SumValues depositSum, SumValues transferSum, ResponseSpecification spec){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 3);

        UserSteps.transferErrorResponse(
                user1.getToken(),
                user1acc1Id, user1acc2Id,
                transferSum,
                spec);

        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);
        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc2Id);
    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(SumValues.MAXDEPOSIT, SumValues.OVERMAXTRANSFER, ResponseSpecs.badRequestTransferOverMax()),
                Arguments.of(SumValues.MINDEPOSIT, SumValues.LESSMIN, ResponseSpecs.badRequestTransferLessMin())
        );
    }

    @ParameterizedTest
    @MethodSource("overBalanceData")
    @Tag("with_database_with_fix")
    public void userCanNotTransferOverBalance(String type) {
        String token = user1.getToken();
        long fromAcc = user1acc1Id;
        long toAcc;

        if(type.equals("SELF")){
            toAcc = user1acc2Id;
        } else {
            toAcc = user2acc1Id;
        }
        UserSteps.transferErrorResponse(
                token, fromAcc, toAcc,
                SumValues.MAXTRANSFER,
                ResponseSpecs.badRequestNotEnoughAmount());

        UserSteps.assertBalanceEqualsDB(user1.getToken(), user1acc1Id);
        UserSteps.assertBalanceEqualsDB(user2.getToken(), user2acc1Id);
    }
    public static Stream<Arguments> overBalanceData() {
        return Stream.of(
                Arguments.of("SELF"), // на свой счёт
                Arguments.of("OTHER")    // на чужой счёт
        );
    }

}
