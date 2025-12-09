package api.iteration2;

import api.models.SumValues;
import api.models.UserModel;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import api.specs.ResponseSpecs;
import java.util.stream.Stream;


public class DepositTest extends BaseTest {

    private static UserModel user1;
    private static UserModel user2;
    private static long user1accId;
    private static long user2accId;


    @BeforeAll
    public static void preSteps() {

        //создание пользователя1 и счета
        user1 = UserSteps.createUser();
        user1accId = UserSteps.createAccountAndGetId(user1.getToken());


        //создание пользователя2  и счета
        user2 = UserSteps.createUser();
        user2accId = UserSteps.createAccountAndGetId(user2.getToken());

    }

    @AfterAll
    public static void deleteUsers() {
        UserSteps.deleteUsers(user1, user2);
    }

    @ParameterizedTest
    @MethodSource("validDepositsData")
    public void userCanDeposit(SumValues depositSum) {
        float initialBalance = UserSteps.getAccBalance(user1.getToken(), user1accId);
        UserSteps.depositAndAssert(
                softly, initialBalance,
                user1.getToken(), user1accId, depositSum);
    }
    public static Stream<Arguments> validDepositsData() {
        return Stream.of(
                Arguments.of(SumValues.MINDEPOSIT),
                Arguments.of(SumValues.MAXDEPOSIT)
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDepositsSum")
    public void userCannotDepositInvalidSum(SumValues sum, ResponseSpecification spec) {
        UserSteps.deposit(
                user1.getToken(),
                user1accId,
                sum,
                spec
        );
    }

    public static Stream<Arguments> invalidDepositsSum() {
        return Stream.of(
                Arguments.of(SumValues.LESSMIN, ResponseSpecs.badRequestSumLessMin()),
                Arguments.of(SumValues.OVERMAXDEPOSIT, ResponseSpecs.badRequestSumOverMax())
        );
    }

    @Test
    public void userCanNotDepositIntoNotExistingAcc() {
        long notExistingAcc = (long) (Math.random() * 10000);
        UserSteps.deposit(
                user1.getToken(),
                notExistingAcc,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());

    }

    @Test
    public void userCanNotDepositIntoAnotherUserAcc() {

        //пользователь1 пытается положить депозит на счет пользователя2
        UserSteps.deposit(
                user1.getToken(),
                user2accId,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());


    }
}
