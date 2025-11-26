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
public class Account extends BaseModel{
    private long id;
    private String accountNumber;
    private float balance;
    private List<Transaction> transactions;



}
