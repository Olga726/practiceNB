package api.sceleton.requests;

import api.models.*;

import lombok.AllArgsConstructor;
import lombok.Getter;


@AllArgsConstructor
@Getter
public enum Endpoint {
    ADMIN_USER(
            "/admin/users",
            CreateUserRequest.class,
            CreateUserResponse.class
    ),
    ADMIN_GET_ALLUSERS(
            "/admin/users",
            BaseModel.class,
            Customer.class
    ),

    LOGIN(
            "/auth/login",
            AuthUserRequest.class,
            LoginResponse.class
    ),
    NAME(
            "/customer/profile",
            UpdateUserNameRequest.class,
            UpdateUserNameResponse.class
    ),
    DEPOSIT(
            "/accounts/deposit",
            DepositRequest.class,
            DepositResponse.class
    ),
    ACCOUNTINFO(
            "/customer/accounts",
            BaseModel.class,
            Account.class
    ),
    TRANSFER(
            "/accounts/transfer",
            TransferRequest.class,
            TransferResponse.class
    ),
    ACCOUNTS(
            "/accounts",
            BaseModel.class,
            Account.class
    ),
    ADMIN_USER_DELETE(
            "/admin/users/{id}",
            BaseModel.class,
            String.class
    );
    
    private final String url;
    private final Class<? extends BaseModel> requestModel;
    private final Class<?> responseModel;
    }