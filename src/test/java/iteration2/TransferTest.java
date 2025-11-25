package iteration2;


import io.restassured.specification.ResponseSpecification;
import models.*;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.MethodSource;
import requesters.sceleton.requests.Endpoint;
import requesters.sceleton.requests.ValidatedCrudRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class TransferTest extends BaseTest {

    private UserModel user1;
    private UserModel user2;
    private long user1acc1Id;
    private long user1acc2Id;
    private long user2acc1Id;

    @BeforeEach
    public void preSteps() {
        //создание пользователя1 и счетов
        user1 = UserSteps.createUser();
        user1acc1Id = UserSteps.createAccount(user1.getToken());
        user1acc2Id = UserSteps.createAccount(user1.getToken());

        //создание пользователя2 и счета
        user2 = UserSteps.createUser();
        user2acc1Id = UserSteps.createAccount(user2.getToken());

    }

    @AfterEach
    public void deleteUsers(){
        long[] ids = {user1.getId(), user2.getId()};

        for(long id : ids) {
            String deleteMessage = new ValidatedCrudRequester<DeleteMessage>(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_USER_DELETE,
                    ResponseSpecs.success())
                    .delete(id);

            String json = new ValidatedCrudRequester<>(
                    RequestSpecs.adminSpec(),
                    Endpoint.ADMIN_GET_ALLUSERS,
                    ResponseSpecs.success())
                    .getAll();

            assertThat(deleteMessage).isEqualTo("User with ID "+id +" deleted successfully.");
            assertFalse(json.contains("\"id\": " + id));

        }
    }

    //минимальный и максимальный перевод на свой счет
    @ParameterizedTest
    @CsvSource({
            "MINDEPOSIT, MINTRANSFER",
            "MAXDEPOSIT, MAXTRANSFER"
    })
    public void userCanTransferToOwnAccount(SumValues depositSum, SumValues transferSum){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 2);  //2 раза депозит
        UserSteps.transferAndAssert(softly, user1.getToken(), user1acc1Id, user1acc2Id, transferSum);
    }


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
    public void userCanNotTransferFromAnotherUserAcc() {
        //пользователь2 делает перевод 10000 себе со счета пользователя1
        UserSteps.transferErrorResponse(
                user2.getToken(),
                user1acc1Id, user2acc1Id,
                SumValues.MAXTRANSFER,
                ResponseSpecs.unauthorized());
    }

    @Test
    public void userCanTransferToTheSameAccount() {
        UserSteps.depositAndTransferSuccess(
                user1.getToken(),
                user1acc1Id, user1acc1Id,
                SumValues.MINTRANSFER);
    }


    @ParameterizedTest
    @MethodSource("transferInvalidData")
    public void userCanNotTransferOverMaxOrLessMin(SumValues depositSum, SumValues transferSum, ResponseSpecification spec){
        UserSteps.depositNTimes(user1.getToken(), user1acc1Id, depositSum, 3);

        UserSteps.transferErrorResponse(
                user1.getToken(),
                user1acc1Id, user1acc2Id,
                transferSum,
                spec);

    }

    public static Stream<Arguments> transferInvalidData() {
        return Stream.of(
                Arguments.of(SumValues.MAXDEPOSIT, SumValues.OVERMAXTRANSFER, ResponseSpecs.badRequestTransferOverMax()),
                Arguments.of(SumValues.MINDEPOSIT, SumValues.LESSMIN, ResponseSpecs.badRequestTransferLessMin())
        );
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
