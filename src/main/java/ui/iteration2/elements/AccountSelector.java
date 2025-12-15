package ui.iteration2.elements;

import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
public class AccountSelector extends BaseElement{
    private final String accNumber;
    private final String accBalance;

    private static final Pattern PATTERN =
            Pattern.compile("(\\w+) \\(Balance: \\$(\\d+\\.\\d{2})\\)");

    public AccountSelector(SelenideElement option) {
        super(option);

        String text = option.getText().replace("\n", "").trim();
        Matcher matcher = PATTERN.matcher(text);

        if (!matcher.find()) {
            throw new IllegalStateException("Cannot parse account selector: " + text);
        }

        this.accNumber = matcher.group(1);
        this.accBalance = matcher.group(2);
    }
}

