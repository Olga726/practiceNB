package models;

import generators.GeneratingRule;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateUserRequest extends BaseModel {
    @GeneratingRule(regex = "^[A-Za-z0-9]{3,15}$")
    private String username;
    @GeneratingRule(regex ="^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$^&+=])\\S{8,20}$")
    private String password;
    @GeneratingRule(regex = "^USER$")
    private Role role;

}
