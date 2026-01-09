package api.models;

import lombok.Data;

@Data
public class TransactionResponse extends BaseModel {
    private long id;
    private float amount;
    private String type;
    private String timestamp;
    private int relatedAccountId;
}
