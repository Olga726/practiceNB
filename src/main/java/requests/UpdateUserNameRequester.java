package requests;

import io.restassured.response.ValidatableResponse;
import io.restassured.specification.RequestSpecification;
import io.restassured.specification.ResponseSpecification;
import models.UpdateUserNameRequest;

import static io.restassured.RestAssured.given;

public class UpdateUserNameRequester extends Request<UpdateUserNameRequest> {
    public UpdateUserNameRequester(RequestSpecification requestSpecification, ResponseSpecification responseSpecification) {
        super(requestSpecification, responseSpecification);
    }

    @Override
    public ValidatableResponse post(UpdateUserNameRequest model) {
        return
                given()
                        .spec(requestSpecification)
                        .body(model)
                        .put("/api/v1/customer/profile")
                        .then()
                        .assertThat()
                        .spec(responseSpecification);
    }
}
