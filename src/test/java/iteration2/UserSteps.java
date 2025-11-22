package iteration2;

import generators.RandomData;
import models.*;
import requests.*;
import specs.RequestSpecs;
import specs.ResponseSpecs;

import java.util.List;

public class UserSteps {

    public static UserModel createUserAndGetToken() {
        String username = RandomData.getUserName();
        String password = RandomData.getPassword();


        CreateUserResponse createUserResponse = new AdminCreateUserRequester(
                RequestSpecs.adminSpec(),
                ResponseSpecs.entityWasCreated())
                .post(new CreateUserRequest(username, password, Role.USER))
                .extract()
                .as(CreateUserResponse.class);

        String token = new LoginUserRequester(RequestSpecs.unauthSpec(),
                ResponseSpecs.success())
                .post(new AuthUserRequest(username, password))
                .extract()
                .header("Authorization");

        return UserModel.builder()
                .username(username)
                .password(password)
                .id(createUserResponse.getId())
                .token(token)
                .build();
    }

    public static long createAccount(String token) {
        return new UserCreateAccountRequester(RequestSpecs.authSpec(token),
                ResponseSpecs.entityWasCreated())
                .post(null)
                .extract()
                .jsonPath()
                .getInt("id");
    }

    public static float getAccBalance(String token, long accId){
        List<Account> accountList =
                new UserGetAccountInfoRequester(
                        RequestSpecs.authSpec(token),
                        ResponseSpecs.success())
                        .get()
                        .extract()
                        .body()
                        .jsonPath()
                        .getList("", Account.class);

        return accountList.stream()
                .filter(a -> a.getId() ==accId)
                .findFirst()
                .orElseThrow(()->new RuntimeException("Account nit found"))
                .getBalance();


    }

}
