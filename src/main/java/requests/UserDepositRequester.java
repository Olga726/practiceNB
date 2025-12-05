package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.DepositRequest;

import static io.restassured.RestAssured.given;

public class UserDepositRequester extends Request<DepositRequest> {
    public UserDepositRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(DepositRequest model) {
        return
             given()
                     .spec(requestSpecification)
                     .body(model)
                     .post("/api/v1/accounts/deposit")
                     .then()
                     .assertThat()
                     .spec(responseSpecification);
    }
}
