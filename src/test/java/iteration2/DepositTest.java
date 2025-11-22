package iteration2;

import generators.RandomData;
import models.*;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import requests.AdminCreateUserRequester;
import requests.LoginUserRequester;
import requests.UserCreateAccountRequester;
import requests.UserDepositRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DepositTest extends BaseTest {

    private static String userAuthHeader;
    private static String userAuthHeader2;
    private static long acc1Id;
    private static long user2accId;
    private static final float MINDEPOSIT = 0.01f;
    private static final float MAXDEPOSIT = 5000.0f;
    private static final float SOMEDEPOSIT = MINDEPOSIT + 0.01f;


    @BeforeAll
    public static void preSteps() {

        //создание пользователя, получение токена
        userAuthHeader = UserSteps.createUserAndGetToken().getToken();

        //пользователь создает счет
        acc1Id = UserSteps.createAccount(userAuthHeader);

    }

    @Test
    public void userCanDepositMaxAmount() {

        //пользователь делает max депозит 5000 на счет acc1Id
        float initialBalance = UserSteps.getAccBalance(userAuthHeader, acc1Id);

        DepositResponse depositResponse = new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.success())
                .post(DepositFactory.maxDeposit(acc1Id))
                .extract()
                .as(DepositResponse.class);

        float newBalance = depositResponse.getBalance();

        softly.assertThat(acc1Id).isEqualTo(depositResponse.getId());
        softly.assertThat(newBalance).isEqualTo(initialBalance + MAXDEPOSIT);
        softly.assertThat(depositResponse.getAccountNumber()).isNotNull();
        softly.assertThat(depositResponse.getTransactions()).isNotNull();

    }

    @Test
    public void userCanDepositMinAmount() {

        //пользователь делает min депозит 0.01 на счет acc1Id
        float initialBalance = UserSteps.getAccBalance(userAuthHeader, acc1Id);

        DepositResponse depositResponse = new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.success())
                .post(DepositFactory.minDeposit(acc1Id))
                .extract()
                .as(DepositResponse.class);

        float newBalance = depositResponse.getBalance();

        softly.assertThat(acc1Id).isEqualTo(depositResponse.getId());
        softly.assertThat(newBalance).isEqualTo(initialBalance + MINDEPOSIT);
        softly.assertThat(depositResponse.getAccountNumber()).isNotNull();
        softly.assertThat(depositResponse.getTransactions()).isNotNull();

    }

    @Test
    public void userCanNotDepositInvalidSumLessMin() {
        new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.badRequestSumLessMin())
                .post(DepositFactory.belowMin(acc1Id));

    }

    @Test
    public void userCanNotDepositInvalidSumOverMax() {
        new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.badRequestSumOverMax())
                .post(DepositFactory.aboveMax(acc1Id));

    }

    @Test
    public void userCanNotDepositIntoNotExistingAcc() {
        int notExistingAcc = (int) Math.random() * 10000;

        DepositRequest depositRequest = DepositRequest.builder()
                .id(notExistingAcc)
                .balance(SOMEDEPOSIT)
                .build();

        new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.unauthorized())
                .post(depositRequest);

    }

    @Test
    public void userCanNotDepositIntoAnotherUserAcc() {
        //создание пользователя2 и получение токена
        userAuthHeader2 = UserSteps.createUserAndGetToken().getToken();

        //пользователь2 создает счет
        user2accId = UserSteps.createAccount(userAuthHeader2);

        //пользователь1 пытается положить депозит на счет пользователя2
        new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.unauthorized())
                .post(DepositFactory.minDeposit(user2accId));
    }
}
