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

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class DepositTest extends BaseTest {

public class DepositTest {
    private static String username = "cat2025-1";
    private static String username2 = "Notcat2025-1";
    private static String password = "sTRongPassword33$";
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
        float initialBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        String body = String.format("""
                {
                  "id": %d,
                  "balance": 5000.0
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        float newBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        assertEquals(newBalance, initialBalance + 5000.0f, 0.0001f);

    }

    @Test
    public void userCanDepositMinAmount() {

        //пользователь делает min депозит 0.01 на счет acc1Id

        float initialBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        String body = String.format("""
                {
                  "id": %d,
                  "balance": 0.01
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        float newBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        assertEquals(newBalance, initialBalance + 0.01f, 0.0001f);

    }

    @Test
    public void userCanNotDepositInvalidSumLessMin() {
        new UserDepositRequester(RequestSpecs.authSpec(userAuthHeader),
                ResponseSpecs.badRequestSumLessMin())
                .post(DepositFactory.belowMin(acc1Id));

    }

    @MethodSource("depositInvalidSumData")
    @ParameterizedTest
    public void userCanNotDepositInvalidSum(float sum) {
        float initialBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        String body = String.format("""
                {
                  "id": %d,
                  "balance": sum
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST);

        float newBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        assertEquals(newBalance, initialBalance);
    }

    @Test
    public void userCanNotDepositIntoNotExistingAcc() {
        float initialBalance = GetBalance.getBalance(userAuthHeader, acc1Id);
        int notExistingAcc = (int) Math.random() * 10000;

        String body = String.format("""
                {
                  "id": %d,
                  "balance": 1.0
                }
                """, notExistingAcc);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        float newBalance = GetBalance.getBalance(userAuthHeader, acc1Id);

        assertEquals(newBalance, initialBalance);
    }

    @Test
    public void userCanNotDepositIntoAnotherUserAcc() {
        //создание пользователя2 и получение токена
        userAuthHeader2 = UserSteps.createUserAndGetToken().getToken();

        //пользователь2 создает счет
        user2accId = UserSteps.createAccount(userAuthHeader2);

        //пользователь1 пытается положить депозит на счет пользователя2
        float initialBalance = GetBalance.getBalance(user2AuthHeader, acc2Id);

        String body3 = String.format("""
                {
                  "id": %d,
                  "balance": 1.0
                }
                """, acc2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        float newBalance = GetBalance.getBalance(user2AuthHeader, acc2Id);
        assertEquals(newBalance, initialBalance);
    }
}
