package api.iteration2;

import api.iteration2.generators.RandomEntityGenerator;
import api.iteration2.models.*;
import api.iteration2.models.Account;
import api.iteration2.models.AuthUserRequest;
import api.iteration2.models.CreateUserRequest;
import api.iteration2.models.UserModel;
import io.restassured.specification.ResponseSpecification;

import org.assertj.core.api.SoftAssertions;
import requesters.sceleton.requests.CrudRequester;
import requesters.sceleton.requests.Endpoint;
import requesters.sceleton.requests.ValidatedCrudRequester;
import api.iteration2.specs.RequestSpecs;
import api.iteration2.specs.ResponseSpecs;

import java.util.List;

import static io.restassured.RestAssured.given;
import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class UserSteps {

    public static UserModel createUser() {
        CreateUserRequest userRequest = RandomEntityGenerator.generate(CreateUserRequest.class);

        CreateUserResponse createUserResponse = new ValidatedCrudRequester<CreateUserResponse>
                (RequestSpecs.adminSpec(),
                        Endpoint.ADMIN_USER,
                        ResponseSpecs.entityWasCreated())
                .post(userRequest);

        String token = new CrudRequester
                (RequestSpecs.unauthSpec(),
                        Endpoint.LOGIN,
                        ResponseSpecs.success())
                .post(new AuthUserRequest(userRequest.getUsername(), userRequest.getPassword()))
                .extract()
                .header("Authorization");

        return UserModel.builder()
                .username(userRequest.getUsername())
                .password(userRequest.getPassword())
                .id(createUserResponse.getId())
                .token(token)
                .build();
    }


    public static long createAccountAndGetId(String token) {
        return new CrudRequester(
                RequestSpecs.authSpec(token),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    public static Account createAccount(String token) {
        return new CrudRequester(
                RequestSpecs.authSpec(token),
                Endpoint.ACCOUNTS,
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .as(Account.class);

    }

    public static float getAccBalance(String token, long accId) {
        List<Account> accountList =
                new CrudRequester(
                        RequestSpecs.authSpec(token),
                        Endpoint.ACCOUNTINFO,
                        ResponseSpecs.success())
                        .get(accId)
                        .extract()
                        .body()
                        .jsonPath()
                        .getList("", Account.class);

        return accountList.stream()
                .filter(a -> a.getId() == accId)
                .findFirst()
                .orElseThrow(() -> new RuntimeException("Account not found"))
                .getBalance();

    }

    public static DepositRequest makeDeposit(long accId, SumValues sum) {
        return DepositRequest.builder()
                .id(accId)
                .balance(sum.getValue())
                .build();
    }

    public static DepositResponse depositResponse(
            String token, long accId,
            SumValues sum, ResponseSpecification spec) {
        return new ValidatedCrudRequester<DepositResponse>(
                RequestSpecs.authSpec(token),
                Endpoint.DEPOSIT,
                spec)
                .post(makeDeposit(accId, sum));
    }

    public static void deposit(
            String token, long accId,
            SumValues sum, ResponseSpecification spec) {
        new CrudRequester(
                RequestSpecs.authSpec(token),
                Endpoint.DEPOSIT,
                spec)
                .post(makeDeposit(accId, sum));
    }

    public static void depositNTimes(
            String token, long accId,
            SumValues sum, int times) {
        for (int i = 0; i < times; i++) {
            deposit(token, accId, sum, ResponseSpecs.success());
        }
    }

    public static TransferResponse depositAndTransferSuccess(
            String token, long fromAcc, long toAcc, SumValues sum) {
        deposit(token, fromAcc, sum, ResponseSpecs.success());
        return transferSuccessResponse(token, fromAcc, toAcc, sum, ResponseSpecs.success());

    }

    public static void transferAndAssert(
            SoftAssertions softly, String token,
            long fromAcc, long toAcc, SumValues sum) {
        TransferResponse resp = transferSuccessResponse(token, fromAcc, toAcc, sum, ResponseSpecs.success());
        assertTransferResponse(softly, resp, fromAcc, toAcc, sum.getValue());
    }

    public static TransferRequest transferRequest(
            long fromAccId, long toAccId, SumValues sum) {
        return TransferRequest.builder()
                .senderAccountId(fromAccId)
                .amount(sum.getValue())
                .receiverAccountId(toAccId)
                .build();

    }

    public static TransferResponse transferSuccessResponse(
            String token,
            long fromAcc,
            long toAcc,
            SumValues sum,
            ResponseSpecification spec) {
        return new ValidatedCrudRequester<TransferResponse>(
                RequestSpecs.authSpec(token),
                Endpoint.TRANSFER,
                spec)
                .post(transferRequest(fromAcc, toAcc, sum));
    }

    public static void transferErrorResponse(
            String token,
            long fromAcc,
            long toAcc,
            SumValues sum,
            ResponseSpecification spec) {

        new CrudRequester(
                RequestSpecs.authSpec(token),
                Endpoint.TRANSFER,
                spec)
                .post(transferRequest(fromAcc, toAcc, sum));
    }

    public static void assertTransferResponse(
            SoftAssertions softly,
            TransferResponse resp,
            long expectedSender,
            long expectedReceiver,
            float expectedAmount) {
        softly.assertThat(resp.getAmount()).isEqualTo(expectedAmount);
        softly.assertThat(resp.getMessage()).isEqualTo("Transfer successful");
        softly.assertThat(resp.getSenderAccountId()).isEqualTo(expectedSender);
        softly.assertThat(resp.getReceiverAccountId()).isEqualTo(expectedReceiver);
    }

    public static void assertDepositResponse(
            SoftAssertions softly,
            DepositResponse resp,
            SumValues sum,
            long account,
            float initialBalance) {
        softly.assertThat(account).isEqualTo(resp.getId());
        softly.assertThat(resp.getBalance()).isEqualTo(initialBalance + sum.getValue());
        softly.assertThat(resp.getTransactions()).isNotNull();

    }

    public static void depositAndAssert(SoftAssertions softly, float initialBalance, String token, long account, SumValues sum) {
        DepositResponse resp = depositResponse(token, account, sum, ResponseSpecs.success());
        assertDepositResponse(softly, resp, sum, account, initialBalance);
    }

    public static void deleteUsers(UserModel... users) {
        for (UserModel user : users) {
            String deleteMessage = new ValidatedCrudRequester<DeleteMessage>(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_USER_DELETE,
                    ResponseSpecs.success())
                    .delete(user.getId());

            String json = new ValidatedCrudRequester<>(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_GET_ALLUSERS,
                    ResponseSpecs.success())
                    .getAll();

            assertThat(deleteMessage).isEqualTo("User with ID " + user.getId() + " deleted successfully.");
            assertFalse(json.contains("\"id\": " + user.getId()));
        }
    }

    public static String setAndGetName(String name, String token){
        return given()
                .spec(RequestSpecs.authSpec(token))
                .body(String.format("""
                        {
                        "name": "%s"
                        }
                        """, name
                ))
                .put("http://localhost:4111/api/v1/customer/profile")
                .then().assertThat()
                .statusCode(200)
                .extract()
                .path("customer.name");

    }
}






