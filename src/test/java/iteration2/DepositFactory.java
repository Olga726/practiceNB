package iteration2;

import models.DepositRequest;

public class DepositFactory {
    private static final float MAXDEPOSIT = 5000.0f;
    private static final float MINDEPOSIT = 0.01f;

    public static DepositRequest maxDeposit(long accId){
        return DepositRequest.builder()
                .id(accId)
                .balance(MAXDEPOSIT)
                .build();
    }

    public static DepositRequest minDeposit(long accId){
        return DepositRequest.builder()
                .id(accId)
                .balance(MINDEPOSIT)
                .build();
    }

    public static DepositRequest belowMin(long accId){
        return DepositRequest.builder()
                .id(accId)
                .balance(MINDEPOSIT-0.01f)
                .build();
    }

    public static DepositRequest aboveMax(long accId){
        return DepositRequest.builder()
                .id(accId)
                .balance(MAXDEPOSIT+0.01f)
                .build();
    }



}
