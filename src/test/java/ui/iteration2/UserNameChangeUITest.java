package ui.iteration2;

import api.iteration2.UserSteps;
import api.iteration2.models.UserModel;

import com.codeborne.selenide.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ui.iteration2.pages.AlertMessages;

import ui.iteration2.pages.DashboardPage;
import ui.iteration2.pages.EditProfilePage;


import static com.codeborne.selenide.Condition.*;
import static com.codeborne.selenide.Selenide.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class UserNameChangeUITest extends BaseUiTest {
    private static UserModel user1;


    @BeforeEach
    public void createUserAndAccount() {
        user1 = UserSteps.createUser();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Johny Donny",
            "NAOMI K",
            "l v",
            "Wolfeschlegelsteinhausenbergerdorff Ninachinmacdholicachinskerray"
    })
    public void userCanChangeNameWithValidDataTest(String name) {
        authAsUser(user1.getUsername(), user1.getPassword());

        //проверка отображения имени и username
        DashboardPage dashboard = new DashboardPage().open();
        dashboard.getWelcomeText().shouldHave(Condition.text("noname"));
        dashboard.getUserName().shouldHave(Condition.text("Noname"));
        dashboard.getUserUserName().shouldHave(Condition.text(user1.getUsername()));

        EditProfilePage editProfilePage =dashboard.openEditProfile().getPage(EditProfilePage.class);
        editProfilePage.editProfile(name)
                .checkAlertAndConfirm(AlertMessages.NAME_UPDATED_SUCCESSFULLY);

        refresh();

        editProfilePage.getUserName().shouldHave(text(name));
        editProfilePage.getEditProfileInput().shouldHave(value(name));

        //Проверка API что имя обновилось в профиле
        assertEquals(name, UserSteps.getCustomerName(user1));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "George 1",
            "Van De Fry",
            "Ivi  Smith",
            "= /*-<>:;'.?!@#$%^^&*()+-",
            "Li",
            "1"
    })
    public void userCanNotChangeNameWithInvalidDataTest(String name) {
        authAsUser(user1.getUsername(), user1.getPassword());

        EditProfilePage editProfilePage = new EditProfilePage().open()
                .editProfile(name)
                .checkAlertAndConfirm(AlertMessages.NAME_INVALID);

        refresh();

        //проверка, что старое имя Noname отображается на ui
        editProfilePage.getUserName().shouldHave(text("Noname"));

        //Проверка API что имя пустое
        assertNull(UserSteps.getCustomerName(user1));

    }

    @Test
    public void userCanNotChangeNonameWithoutEnterInputTest() {
        //проверка если было noname
        authAsUser(user1.getUsername(), user1.getPassword());

        EditProfilePage editProfilePage = new EditProfilePage().open().clickButton()
                .checkAlertAndConfirm(AlertMessages.ENTER_VALID_NAME);
        refresh();

        editProfilePage.getUserName().shouldHave(text("Noname"));
        assertNull(UserSteps.getCustomerName(user1));
    }

    @Test
    public void userCanNotChangeNameWithoutEnterInputTest() {
        String newName = "Poll Fall";
        authAsUser(user1.getUsername(), user1.getPassword());

        EditProfilePage editProfilePage = new EditProfilePage().open().editProfile(newName);
        refresh();
        sleep(2000);
        editProfilePage.clickButton().checkAlertAndConfirm(AlertMessages.NEW_NAME_IS_SAME);
        refresh();

        editProfilePage.getUserName().shouldHave(text(newName));
        assertEquals(newName, UserSteps.getCustomerName(user1));
    }

    @Test
    public void userCanNotChangeNameToOnlySpacesTest() {
        String newName = "   ";
        authAsUser(user1.getUsername(), user1.getPassword());
        EditProfilePage editProfilePage = new EditProfilePage().open().editProfile(newName)
                .checkAlertAndConfirm(AlertMessages.ENTER_VALID_NAME);
        refresh();

        editProfilePage.getUserName().shouldHave(text("Noname"));
        assertNull(UserSteps.getCustomerName(user1));
    }
}
