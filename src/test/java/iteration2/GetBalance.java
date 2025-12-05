package iteration2;

import io.restassured.http.ContentType;
import org.apache.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static io.restassured.RestAssured.given;

public class GetBalance {
    public static float getBalance(String token, long accId){
        List<Map<String, Object>> accountsList = given()
                .contentType(ContentType.JSON)
                .accept(ContentType.JSON)
                .header("Authorization", token)
                .get("http://localhost:4111/api/v1/customer/accounts")
                .then()
                .assertThat()
                .statusCode(HttpStatus.SC_OK)
                .extract()
                .body()
                .jsonPath()
                .getList("");

        Map<String, Object> account = accountsList.stream()
                .filter(a -> ((Number)a.get("id")).longValue() == accId)
                .findFirst()
                .orElseThrow(()-> new RuntimeException("Account not found"));

        return ((Number)account.get("balance")).floatValue();

    }
}
