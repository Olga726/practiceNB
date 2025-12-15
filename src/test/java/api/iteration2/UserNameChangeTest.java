package api.iteration2;

import api.models.Role;
import api.models.UpdateUserNameRequest;
import api.models.UpdateUserNameResponse;
import api.models.UserModel;
import api.steps.UserSteps;
import common.annotations.UserSession;
import common.extensions.UserSessionExtension;
import common.storage.SessionStorage;
import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import api.sceleton.requests.CrudRequester;
import api.sceleton.requests.Endpoint;
import api.sceleton.requests.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.List;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertFalse;


public class UserNameChangeTest extends BaseTest {
    private static UserModel user;
    @BeforeAll
    public static void setUpRestAsuured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @BeforeAll
    public static void preSteps() {
        //создание пользователя
        user = UserSteps.createUser();
    }

    @AfterAll
    public static void deleteUser() {
        UserSteps.deleteUsers(user);

    }


    public static Stream<Arguments> nameValidData() {
        return Stream.of(
                Arguments.of("Johny Donny"),
                Arguments.of("NAOMI K"),
                Arguments.of("l v"),
                Arguments.of("Wolfeschlegelsteinhausenbergerdorff Ninachinmacdholicachinskerray")
        );
    }
    @MethodSource("nameValidData")
    @ParameterizedTest
    public void userCanUpdateCustomerProfileWithValidData(String name) {
        UpdateUserNameRequest updateUserNameRequest = new UpdateUserNameRequest(name);

        UpdateUserNameResponse updateUserNameResponse = new ValidatedCrudRequester<UpdateUserNameResponse>(
                RequestSpecs.authSpec(user.getToken()),
                Endpoint.NAME,
                ResponseSpecs.success())
                .update(updateUserNameRequest);

        softly.assertThat("Profile updated successfully").isEqualTo(updateUserNameResponse.getMessage());
        softly.assertThat(user.getId()).isEqualTo(updateUserNameResponse.getCustomer().getId());
        softly.assertThat(name).isEqualTo(updateUserNameResponse.getCustomer().getName());
        softly.assertThat(user.getUsername()).isEqualTo(updateUserNameResponse.getCustomer().getUsername());
        softly.assertThat(user.getPassword()).isNotEqualTo(updateUserNameResponse.getCustomer().getPassword());
        softly.assertThat(Role.USER).isEqualTo(updateUserNameResponse.getCustomer().getRole());

    }

    public static Stream<Arguments> nameInvalidData() {
        return Stream.of(
                Arguments.of("Вася Обломов"),
                Arguments.of("Li  Lia"),
                Arguments.of("Li,Lia"),
                Arguments.of("Li2 Ku"),
                Arguments.of(" Li KuChi "),
                Arguments.of("Li"),
                Arguments.of("Li Ku Chi"),
                Arguments.of(". <"),
                Arguments.of("   ")
        );
    }

    @MethodSource("nameInvalidData")
    @ParameterizedTest
    public void userCanNotUpdateCustomerProfileWithInvalidData(String name) {
        UpdateUserNameRequest updateUserNameRequest = new UpdateUserNameRequest(name);

        new CrudRequester(RequestSpecs.authSpec(user.getToken()),
                Endpoint.NAME,
                ResponseSpecs.badRequestInvalidUsername())
                .update(updateUserNameRequest);

    }
}




