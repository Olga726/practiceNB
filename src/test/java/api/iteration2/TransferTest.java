package api.iteration2;


import api.models.DeleteMessage;
import api.models.SumValues;
import api.models.UserModel;
import api.sceleton.requests.Endpoint;
import api.sceleton.requests.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;
import api.steps.UserSteps;
import io.restassured.specification.ResponseSpecification;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class TransferTest {
    private static String username = "Den2";
    private static String username2 = "Ben2";
    private static String password = "sTRongPassword33$";
    private static String userAuthHeader;
    private static String user2AuthHeader;
    private static int acc1Id;
    private static int acc1_2Id;
    private static int acc2Id;

    private static UserModel user1;
    private static UserModel user2;
    private static long acc1Id;
    private static long acc1_2Id;
    private static long acc2Id;
    private static final float MAXTRANSFER = 10000.0f;
    private static final float MINTRANSFER = 0.01f;


        //создание пользователя2
        String body2 = String.format("""
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
                .body(body2)
                .post("http://localhost:4111/api/v1/admin/users")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_CREATED);

        //аутентификация пользователя1
        String body3 = String.format("""
                {
                   "username": "%s",
                   "password": "%s"
                    }
                """, username, password);
        userAuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body3)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //аутентификация пользователя2
        String body4 = String.format("""
                {
                   "username": "%s",
                   "password": "%s"
                    }
                """, username2, password);
        user2AuthHeader = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .body(body4)
                .post("http://localhost:4111/api/v1/auth/login")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract().header("Authorization");

        //пользователь1 создает счет
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

    private UserModel user1;
    private UserModel user2;
    private long user1acc1Id;
    private long user1acc2Id;
    private long user2acc1Id;

    @BeforeEach
    public void preSteps() {
        //создание пользователя1 и счетов
        user1 = UserSteps.createUser();
        user1acc1Id = UserSteps.createAccount(user1).getId();
        user1acc2Id = UserSteps.createAccount(user1).getId();

        //создание пользователя2 и счета
        user2 = UserSteps.createUser();
        user2acc1Id = UserSteps.createAccount(user2).getId();

    }

    @AfterEach
    public void deleteUsers(){
        long[] ids = {user1.getId(), user2.getId()};

        //пользователь1 повторно делает max депозит 5000 на счет acc1Id
        String body2 = String.format("""
                {
                  "id": %d,
                  "balance": 5000.0
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body2)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //пользователь1 делает перевод 10000 на свой счет
        float initialBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 10000.00
                }
                """, acc1Id, acc1_2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("senderAccountId", Matchers.equalTo(acc1Id))
                .body("amount", Matchers.equalTo(10000.0f))
                .body("receiverAccountId", Matchers.equalTo(acc1_2Id))
                .body("message", Matchers.equalTo("Transfer successful"))
        ;

        float newBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);
        assertEquals(newBalanceAcc1, initialBalanceAcc1-10000.0f, 0.0001f);
        assertEquals(newBalanceAcc2, initialBalanceAcc2+10000.0f, 0.0001f);

        }
    }

    @Test
    public void userCanTransferMinSumToTheirOwnAcc() {
        //пользователь1 делает min депозит 0.01 на счет acc1Id
        String body1 = String.format("""
                {
                  "id": %d,
                  "balance": 0.01
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body1)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(200);

        //пользователь1 делает перевод 0.01 на свой счет
        float initialBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 0.01
                }
                """, acc1Id, acc1_2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("senderAccountId", Matchers.equalTo(acc1Id))
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("amount", Matchers.equalTo(0.01f))
                .body("receiverAccountId", Matchers.equalTo(acc1_2Id));

        float newBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);
        assertEquals(newBalanceAcc1, initialBalanceAcc1-0.01f, 0.0001f);
        assertEquals(newBalanceAcc2, initialBalanceAcc2+0.01f, 0.0001f);

        float transferAmount = transferResponse.getAmount();

        softly.assertThat(MINTRANSFER).isEqualTo(transferAmount);
        softly.assertThat("Transfer successful").isEqualTo(transferResponse.getMessage());
        softly.assertThat(acc1Id).isEqualTo(transferResponse.getSenderAccountId());
        softly.assertThat(acc1_2Id).isEqualTo(transferResponse.getReceiverAccountId());
    }

    @Test
    public void userCanNotTransferSumOverBalanceToTheirOwnAcc() {
        //пользователь1 делает перевод 1000 на свой счет

        float initialBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 1000.0
                }
                """, acc1Id, acc1_2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));

        float newBalanceAcc1 = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newBalanceAcc2 = GetBalance.getBalance(userAuthHeader, acc1_2Id);
        assertEquals(newBalanceAcc1, initialBalanceAcc1);
        assertEquals(newBalanceAcc2, initialBalanceAcc2);

    //минимальный и максимальный перевод на чужой счет
    @ParameterizedTest
    @CsvSource({
            "MINDEPOSIT, MINTRANSFER",
            "MAXDEPOSIT, MAXTRANSFER"
    })
    public void userCanTransferToAnotherUserAccount(SumValues depositSum, SumValues transferSum){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 2);     //2 раза депозит
        UserSteps.transferAndAssert(softly, user1.getToken(), user1acc1Id, user2acc1Id, transferSum);
    }

    @Test
    public void userCanNotTransferSumOverBalanceToAnotherUserAcc() {
        //пользователь1 делает min депозит 0.01 на счет acc1Id


        String body1 = String.format("""
                {
                  "id": %d,
                  "balance": 0.01
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body1)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //пользователь1 делает перевод 0.02 на счет пользователя2
        float initialUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 0.02
                }
                """, acc1Id, acc2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"));

        float newUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);
        assertEquals(newUser1Balance, initialUser1Balance);
        assertEquals(newUser2Balance, initialUser2Balance);

    @Test
    public void userCanNotTransferFromAnotherUserAcc() {
        //пользователь2 делает перевод 10000 себе со счета пользователя1
        UserSteps.transferErrorResponse(
                user2.getToken(),
                user1acc1Id, user2acc1Id,
                SumValues.MAXTRANSFER,
                ResponseSpecs.unauthorized());
    }

    @Test
    public void userCanTransferMinSumToAnotherUserAcc() {
        //пользователь1 делает min депозит 0.01 на счет acc1Id
        String body1 = String.format("""
                {
                  "id": %d,
                  "balance": 0.01
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body1)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //пользователь1 делает перевод 0.01 на счет пользователя2
        float initialUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 0.01
                }
                """, acc1Id, acc2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("amount", Matchers.equalTo(0.01f))
                .body("senderAccountId", Matchers.equalTo(acc1Id))
                .body("message", Matchers.equalTo("Transfer successful"))
                .body("receiverAccountId", Matchers.equalTo(acc2Id));

        float newUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);
        assertEquals(newUser1Balance, initialUser1Balance-0.01f);
        assertEquals(newUser2Balance, initialUser2Balance+0.01f);

    }

    @Test
    public void userCanTransferMaxSumToAnotherUserAcc() {
//пользователь1 делает max депозит 5000.0 на счет acc1Id
        String body1 = String.format("""
                {
                  "id": %d,
                  "balance": 5000.0
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body1)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //пользователь1 повторно делает max депозит 5000.0 на счет acc1Id
        String body2 = String.format("""
                {
                  "id": %d,
                  "balance": 5000.0
                }
                """, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body2)
                .post("http://localhost:4111/api/v1/accounts/deposit")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK);

        //пользователь1 делает перевод 10000 на счет пользователя2
        float initialUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 10000.0
                }
                """, acc1Id, acc2Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("amount", Matchers.equalTo(10000.0f))
                .body("receiverAccountId", Matchers.equalTo(acc2Id))
                .body("senderAccountId", Matchers.equalTo(acc1Id))
                .body("message", Matchers.equalTo("Transfer successful"));

        float newUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);
        assertEquals(newUser1Balance, initialUser1Balance-10000.0f, 0.0001f);
        assertEquals(newUser2Balance, initialUser2Balance+10000.0f, 0.0001f);

    @ParameterizedTest
    @MethodSource("transferInvalidData")
    public void userCanNotTransferOverMaxOrLessMin(SumValues depositSum, SumValues transferSum, ResponseSpecification spec){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 3);

    @Test
    public void userCanNotTransferFromAnotherUserAcc() {
        //пользователь1 делает перевод 10000 себе со счета пользователя2

        float initialUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float initialUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);

        String body3 = String.format("""
                {
                  "senderAccountId": %d,
                  "receiverAccountId": %d,
                  "amount": 10000.0
                }
                """, acc2Id, acc1Id);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body3)
                .post("http://localhost:4111/api/v1/accounts/transfer")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_FORBIDDEN)
                .body(Matchers.equalTo("Unauthorized access to account"));

        float newUser1Balance = GetBalance.getBalance(userAuthHeader, acc1Id);
        float newUser2Balance = GetBalance.getBalance(user2AuthHeader, acc2Id);
        assertEquals(newUser1Balance, initialUser1Balance);
        assertEquals(newUser2Balance, initialUser2Balance);
    }


    @ParameterizedTest
    @MethodSource("overBalanceData")
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
    }
    public static Stream<Arguments> overBalanceData() {
        return Stream.of(
                Arguments.of("SELF"), // на свой счёт
                Arguments.of("OTHER")    // на чужой счёт
        );
    }


}
