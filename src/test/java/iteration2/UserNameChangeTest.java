package iteration2;

import models.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import requests.UpdateUserNameRequester;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.stream.Stream;


public class UserNameChangeTest extends BaseTest{
    private static UserModel user;


    @BeforeAll
    public static void preSteps() {
        //создание пользователя
        user = UserSteps.createUserAndGetToken();

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

        UpdateUserNameResponse updateUserNameResponse = new UpdateUserNameRequester(
                RequestSpecs.authSpec(user.getToken()),
                ResponseSpecs.success())
                .post(updateUserNameRequest)
                .extract()
                .as(UpdateUserNameResponse.class);

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

        new UpdateUserNameRequester(RequestSpecs.authSpec(user.getToken()),
                ResponseSpecs.badRequestInvalidUsername())
                .post(updateUserNameRequest);

    }
}




