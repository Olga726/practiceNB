package api.iteration2;

import api.models.Role;
import api.models.UpdateUserNameRequest;
import api.models.UpdateUserNameResponse;
import api.models.UserModel;
import api.steps.DataBaseSteps;
import api.steps.UserSteps;
import common.annotations.UserSession;
import common.storage.SessionStorage;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import api.sceleton.requests.CrudRequester;
import api.sceleton.requests.Endpoint;
import api.sceleton.requests.ValidatedCrudRequester;
import api.specs.RequestSpecs;
import api.specs.ResponseSpecs;

import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.*;

@UserSession
public class UserNameChangeTest extends BaseTest {

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
    @Tag("with_database_with_fix")
    public void userCanUpdateCustomerProfileWithValidDataTest(String name) {
        UpdateUserNameRequest updateUserNameRequest = new UpdateUserNameRequest(name);
        UserModel user = SessionStorage.getUser(1);

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

        String finalUsername = UserSteps.getCustomerName(user);
        String dbName = DataBaseSteps.getUserById(user.getId()).getName();
        assertEquals(finalUsername, dbName);

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
    @Tag("with_database_with_fix")
    public void userCanNotUpdateCustomerProfileWithInvalidDataTest(String name) {
        UpdateUserNameRequest updateUserNameRequest = new UpdateUserNameRequest(name);
        UserModel user = SessionStorage.getUser(1);
        new CrudRequester(RequestSpecs.authSpec(user.getToken()),
                Endpoint.NAME,
                ResponseSpecs.badRequestInvalidUsername())
                .update(updateUserNameRequest);

        assertNull(UserSteps.getCustomerName(user));
        assertNull(DataBaseSteps.getUserById(user.getId()).getName());

    }
}




