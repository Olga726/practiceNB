package ui.iteration2.pages;

import com.codeborne.selenide.Selectors;
import com.codeborne.selenide.SelenideElement;
import lombok.Getter;

import static com.codeborne.selenide.Condition.text;
import static com.codeborne.selenide.Selenide.$;
import static com.codeborne.selenide.Selenide.$$;

@Getter
public class EditProfilePage extends BasePage<EditProfilePage>{
private SelenideElement editProfileHeader = $(Selectors.byText("✏\uFE0F Edit Profile"));
private SelenideElement editProfileInput = $(Selectors.byAttribute("placeholder", "Enter new name"));
private SelenideElement saveChangesButton = $$("button").findBy(text("\uD83D\uDCBE Save Changes"));

    @Override
    public String url() {
        return "/edit-profile";
    }

    public EditProfilePage editProfile(String newName){
        editProfileInput.setValue(newName);
        saveChangesButton.click();
        return this;
    }

    public EditProfilePage setNewName(String newName){
        editProfileInput.setValue(newName);
        return this;
    }

    public EditProfilePage clickButton (){
        saveChangesButton.click();
        return this;
    }

}
