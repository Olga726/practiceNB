package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class DepositTest {
    private static String username = "cat2025-1";
    private static String username2 = "Notcat2025-1";
    private static String password = "sTRongPassword33$";
    private static String userAuthHeader;
    private static String user2AuthHeader;
    private static int acc1Id;
    private static int acc2Id;

    @BeforeAll
    public static void setUpRestAsuured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @BeforeAll
    public static void preSteps() {
        //создание пользователя
        String body2 = String.format("""
                {
                   "username": "%s",
                   "password": "%s",
                   "role": "USER"
                    }
                """, username, password);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(body2)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //аутентификация пользователя
        String body1 = String.format("""
                {
                   "username": "%s",
                   "password": "%s"
                    }
                """, username, password);
        userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body1)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //пользователь создает счет
        acc1Id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");

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

    public static Stream<Arguments> depositInvalidSumData() {
        return Stream.of(
                Arguments.of(0.0f),
                Arguments.of(5000.01f)
        );
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
        //создание пользователя2
        String body = String.format("""
                {
                   "username": "%s",
                   "password": "%s",
                   "role": "USER"
                    }
                """, username2, password);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", "Basic YWRtaW46YWRtaW4=")
                .body(body)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //аутентификация пользователя2
        String body2 = String.format("""
                {
                   "username": "%s",
                   "password": "%s"
                    }
                """, username2, password);
        user2AuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body2)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");
        ;

        //пользователь2 создает счет
        acc2Id = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", user2AuthHeader)
                .post("http://localhost:4111/api/v1/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED)
                .extract()
                .jsonPath()
                .getInt("id");


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
