package ui.iteration2.pages;

import lombok.Getter;

@Getter
public enum AlertMessages {
    SUCCESSFULLY_DEPOSITED("Successfully deposited"),
    ENTER_VALID_AMOUNT("Please enter a valid amount."),
    DEPOSIT_LESS("Please deposit less or equal to 5000$."),
    SELECT_ACCOUNT("Please select an account."),
    NAME_UPDATED_SUCCESSFULLY("Name updated successfully!"),
    NAME_INVALID("Name must contain two words with letters only"),
    ENTER_VALID_NAME("Please enter a valid name."),
    NEW_NAME_IS_SAME("⚠\uFE0F New name is the same as the current one."),
    SUCCESSFULLY_TRANSFERED("Successfully transferred"),
    TRANSFER_MUST_BE_AT_LEAST("Error: Transfer amount must be at least 0.01"),
    TRANSFER_AMOUNT_CANNOT_EXCEED("Error: Transfer amount cannot exceed 10000"),
    INSUFFICIENT_FUNDS("Error: Invalid transfer: insufficient funds or invalid accounts"),
    RECIPIENT_NAME_DOES_NOT_MATCH("The recipient name does not match the registered name."),
    NO_USER_FOUNT_WITH_THIS_ACCOUNT_NUMBER("No user found with this account number."),
    PLEASE_FILL_ALL_FIELDS_AND_CONFIRM("Please fill all fields and confirm."),
    TRANSFER_SUCCESSFUL_FROM_ACCOUNT_TO("successful from Account");

    private final String message;

    AlertMessages(String message) {
        this.message = message;
    }
}
