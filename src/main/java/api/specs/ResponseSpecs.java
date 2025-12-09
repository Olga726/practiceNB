package api.specs;

import io.restassured.builder.ResponseSpecBuilder;
import io.restassured.specification.ResponseSpecification;
import org.apache.http.HttpStatus;
import org.hamcrest.Matchers;

public class ResponseSpecs {
    private ResponseSpecs() {
    }

    private static ResponseSpecBuilder defaultResponseBuilder() {
        return new ResponseSpecBuilder();
    }

    public static ResponseSpecification entityWasCreated() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_CREATED)
                .build();
    }

    public static ResponseSpecification success() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_OK)
                .build();
    }

    public static ResponseSpecification badRequestSumOverMax() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Deposit amount cannot exceed 5000"))
                .build();
    }

    public static ResponseSpecification badRequestSumLessMin() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Deposit amount must be at least 0.01"))
                .build();
    }

    public static ResponseSpecification badRequestNotEnoughAmount() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Invalid transfer: insufficient funds or invalid accounts"))
                .build();
    }

    public static ResponseSpecification badRequestInvalidUsername() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Name must contain two words with letters only"))
                .build();
    }


    public static ResponseSpecification unauthorized() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_FORBIDDEN)
                .expectBody(Matchers.equalTo("Unauthorized access to account"))
                .build();
    }

    public static ResponseSpecification badRequestTransferLessMin() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Transfer amount must be at least 0.01"))
                .build();
    }

    public static ResponseSpecification badRequestTransferOverMax() {
        return defaultResponseBuilder()
                .expectStatusCode(HttpStatus.SC_BAD_REQUEST)
                .expectBody(Matchers.equalTo("Transfer amount cannot exceed 10000"))
                .build();
    }

}


