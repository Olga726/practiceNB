package api.iteration2;

import api.models.SumValues;
import api.models.UserModel;
import api.specs.ResponseSpecs;
import api.steps.DataBaseSteps;
import api.steps.UserSteps;
import common.annotations.UserSession;
import common.extensions.UserSessionExtension;
import common.storage.SessionStorage;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

@UserSession
public class DepositTest extends BaseTest {



    public static Stream<Arguments> validDepositsData() {
        return Stream.of(
                Arguments.of(SumValues.MINDEPOSIT),
                Arguments.of(SumValues.MAXDEPOSIT)
        );
    }

    @ParameterizedTest
    @MethodSource("validDepositsData")
    @Tag("with_database_with_fix")

    public void userCanDepositTest(SumValues depositSum) {
        UserModel user =SessionStorage.getUser(1);
        long accountId = UserSteps.createAccount(user).getId();
        float initialBalance = UserSteps.getAccBalance(user.getToken(), accountId);
        UserSteps.depositAndAssert(
                softly, initialBalance,
                user.getToken(), accountId, depositSum);

        UserSteps.matchAccountInfoWithDao(user, accountId);
    }


    public static Stream<Arguments> invalidDepositsSum() {
        return Stream.of(
                Arguments.of(SumValues.LESSMIN, ResponseSpecs.badRequestSumLessMin()),
                Arguments.of(SumValues.OVERMAXDEPOSIT, ResponseSpecs.badRequestSumOverMax())
        );
    }

    @ParameterizedTest
    @MethodSource("invalidDepositsSum")
    @Tag("with_database_with_fix")
    public void userCannotDepositInvalidSumTest(SumValues sum, ResponseSpecification spec) {
        UserModel user =SessionStorage.getUser(1);
        long accountId = UserSteps.createAccount(user).getId();
        UserSteps.deposit(
                user.getToken(),
                accountId,
                sum,
                spec
        );

        assertEquals(0.00f, UserSteps.getAccBalance(user.getToken(), accountId), 0.0001f);
        UserSteps.matchAccountInfoWithDao(user, accountId);
    }


    @Test
    @Tag("with_database_with_fix")
    public void userCanNotDepositIntoNotExistingAccTest() {
        UserModel user = UserSteps.createUser();
        long notExistingAcc = (long) (Math.random() * 10000);
        UserSteps.deposit(
                user.getToken(),
                notExistingAcc,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());

        assertEquals(0, UserSteps.getAccounts(user.getToken()).size());
        assertNull(DataBaseSteps.getAccountByCustomerId(user.getId()));

        UserSteps.deleteUsers(user);
    }


    @Test
    @UserSession(value = 2)
    @Tag("with_database_with_fix")
    public void userCanNotDepositIntoAnotherUserAccTest() {
        UserModel user1 = SessionStorage.getUser(1);
        UserModel user2 = SessionStorage.getUser(2);
        long user2AccId = UserSteps.createAccount(user2).getId();

        //пользователь1 пытается положить депозит на счет пользователя2
        UserSteps.deposit(
                user1.getToken(),
                user2AccId,
                SumValues.SOMEDEPOSIT,
                ResponseSpecs.unauthorized());

        assertEquals(0.00f, UserSteps.getAccBalance(user2.getToken(), user2AccId), 0.0001f);
        UserSteps.matchAccountInfoWithDao(user2, user2AccId);

    }
}
