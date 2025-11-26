package models;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class Customer extends BaseModel{
    private long id;
    private String username;
    private String password;
    private String name;
    private Role role;
    private List<Account> accounts;
}
