package iteration2;

import io.restassured.RestAssured;
import io.restassured.filter.log.RequestLoggingFilter;
import io.restassured.filter.log.ResponseLoggingFilter;
import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.List;
import java.util.stream.Stream;

import static io.restassured.RestAssured.given;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

public class UserNameChangeTest {
    private static String username = "Anna1";
    private static String password = "sTRongPassword33$";
    private static String userAuthHeader;

    public String getName(String token){
        return
                given()
                        .contentType(ContentType.JSON)
                        .accept(ContentType.JSON)
                        .header("Authorization", token)
                        .get("http://localhost:4111/api/v1/customer/profile")
                        .then()
                        .assertThat()
                        .statusCode(HttpStatus.SC_OK)
                        .extract()
                        .jsonPath()
                        .getString("name");
    }

    @BeforeAll
    public static void setUpRestAsuured() {
        RestAssured.filters(
                List.of(new RequestLoggingFilter(),
                        new ResponseLoggingFilter()));
    }

    @BeforeAll
    public static void preSteps() {
        //создание пользователя1
        String body = String.format("""
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
                .body(body)
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
        String initialName = getName(userAuthHeader);

        String body = String.format("""
                {"name": "%s"
                }
                """, name);

        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .body("customer.name", Matchers.equalTo(name));

        String updatedName =getName(userAuthHeader);

        assertNotEquals(initialName, updatedName);
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
        String initialName = getName(userAuthHeader);

        String body = String.format("""
                {"name": "%s"
                }
                """, name);
        given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", userAuthHeader)
                .body(body)
                .put("http://localhost:4111/api/v1/customer/profile")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_BAD_REQUEST)
                .body(Matchers.equalTo("Name must contain two words with letters only"));

        String updatedName =getName(userAuthHeader);

        assertEquals(initialName, updatedName);

    }
}




