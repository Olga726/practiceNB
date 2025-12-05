package iteration2;

import models.TransferRequest;

public class TransferFactory {
    private static final float MAXTRANSFER = 10000.0f;
    private static final float MINTRANSFER = 0.01f;

    public static TransferRequest maxTransferRequest(long senderAccId, long receiverAccId){
        return TransferRequest.builder()
                .senderAccountId(senderAccId)
                .amount(MAXTRANSFER)
                .receiverAccountId(receiverAccId)
                .build();
    }

    public static TransferRequest minTransferRequest(long senderAccId, long receiverAccId){
        return TransferRequest.builder()
                .senderAccountId(senderAccId)
                .amount(MINTRANSFER)
                .receiverAccountId(receiverAccId)
                .build();
    }

    public static TransferRequest overMaxTransferRequest(long senderAccId, long receiverAccId){
        return TransferRequest.builder()
                .senderAccountId(senderAccId)
                .amount(MAXTRANSFER + 0.01f)
                .receiverAccountId(receiverAccId)
                .build();
    }

    public static TransferRequest belowMinTransferRequest(long senderAccId, long receiverAccId){
        return TransferRequest.builder()
                .senderAccountId(senderAccId)
                .amount(MINTRANSFER-0.01f)
                .receiverAccountId(receiverAccId)
                .build();
    }

}
