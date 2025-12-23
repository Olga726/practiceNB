package api.iteration2;

import api.models.SumValues;
import api.models.UserModel;
import api.specs.ResponseSpecs;
import api.steps.UserSteps;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.stream.Stream;


public class DepositTest extends BaseTest {

    @ParameterizedTest
    @MethodSource("validDepositsData")
    public void userCanDeposit(SumValues depositSum) {
        UserModel user = UserSteps.createUser();
        long accountId = UserSteps.createAccount(user).getId();
        float initialBalance = UserSteps.getAccBalance(user.getToken(), accountId);
        UserSteps.depositAndAssert(
                softly, initialBalance,
                user.getToken(), accountId, depositSum);
        UserSteps.deleteUsers(user);
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
        UserModel user = UserSteps.createUser();
        long accountId = UserSteps.createAccount(user).getId();
        UserSteps.deposit(
                user.getToken(),
                accountId,
                sum,
                spec
        );
        UserSteps.deleteUsers(user);
    }

    public static Stream<Arguments> invalidDepositsSum() {
        return Stream.of(
                Arguments.of(SumValues.LESSMIN, ResponseSpecs.badRequestSumLessMin()),
                Arguments.of(SumValues.OVERMAXDEPOSIT, ResponseSpecs.badRequestSumOverMax())
        );
    }

    @Test
    public void userCanNotDepositIntoNotExistingAcc() {
        UserModel user = UserSteps.createUser();
        long notExistingAcc = (long) (Math.random() * 10000);
        UserSteps.deposit(
                user.getToken(),
                notExistingAcc,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());

    }

    @Test
    public void userCanNotDepositIntoAnotherUserAcc() {
        UserModel user1 = UserSteps.createUser();

        UserModel user2 = UserSteps.createUser();
        long user2Acc = UserSteps.createAccount(user2).getId();

        //пользователь1 пытается положить депозит на счет пользователя2
        UserSteps.deposit(
                user1.getToken(),
                user2Acc,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());

        UserSteps.deleteUsers(user1, user2);

    }
}
