package ui.iteration2;

import api.steps.UserSteps;

import api.models.UserModel;

import com.codeborne.selenide.Condition;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import generators.NameGenerator;

import org.junit.jupiter.api.BeforeEach;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import ui.iteration2.pages.AlertMessages;
import ui.iteration2.pages.DashboardPage;
import ui.iteration2.pages.EditProfilePage;
import static com.codeborne.selenide.Condition.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static ui.iteration2.pages.BasePage.DEFAULTUSER_NAME;
import static ui.iteration2.pages.DashboardPage.DEFAULTWELCOMENAME;

@UserSession(ui = true)
public class UserNameChangeUITest extends BaseUiTest {
    private UserModel user;
    private String newName;

    @BeforeEach
    public void init(){
        user = SessionStorage.getUser(1);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "Johny Donny",
            "NAOMI K",
            "l v",
            "Wolfeschlegelsteinhausenbergerdorff Ninachinmacdholicachinskerray"
    })
    public void userCanChangeNameWithValidDataTest(String name) {

        //проверка отображения имени и username
        DashboardPage dashboard = new DashboardPage().open();
        dashboard.getWelcomeText().shouldHave(Condition.text(DEFAULTWELCOMENAME));
        dashboard.getUserName().shouldHave(Condition.text(DEFAULTUSER_NAME));
        dashboard.getUserUserName().shouldHave(Condition.text(user.getUsername()));

        EditProfilePage editProfilePage = dashboard.openEditProfile().getPage(EditProfilePage.class);
        editProfilePage.editProfile(name).checkAlertAndConfirm(AlertMessages.NAME_UPDATED_SUCCESSFULLY);

        DashboardPage dashboardAfterReload = new DashboardPage().open();
        dashboardAfterReload.getUserName().shouldHave(text(name));

        EditProfilePage editProfilePageAfterReload = new EditProfilePage().open();
        editProfilePageAfterReload.getEditProfileInput().shouldHave(value(name));

        //Проверка API что имя обновилось в профиле
        assertEquals(name, UserSteps.getCustomerName(user));

    }

    @ParameterizedTest
    @ValueSource(strings = {
            "<script>alert('XSS')</script>",
            "George 1",
            "Van De Fry",
            "Ivi  Smith",
            "= /*-<>:;'.?!@#$%^^&*()+-",
            "Li",
            "1",
            "   "
    })
    public void userCanNotChangeNameWithInvalidDataTest(String name) {
        EditProfilePage editProfilePage = new EditProfilePage().open()
                .editProfile(name)
                .checkAlertAndConfirmAny(
                        AlertMessages.NAME_INVALID,
                        AlertMessages.ENTER_VALID_NAME)
                .getPage(EditProfilePage.class);

        //проверка, что старое имя Noname отображается на ui
        editProfilePage.getUserName().shouldHave(text(DEFAULTUSER_NAME));

        //Проверка API что имя пустое
        assertNull(UserSteps.getCustomerName(user));
    }
    
    @Test
    public void userCanNotChangeNameWithoutEnterInputTest() {
        newName = NameGenerator.generateName();
        UserSteps.setCustomerName(user, newName);

        EditProfilePage editProfilePage =new EditProfilePage().open();
        editProfilePage.getUserName().shouldHave(text(newName));
        editProfilePage.clickButton().checkAlertAndConfirm(AlertMessages.NEW_NAME_IS_SAME)
                .getPage(EditProfilePage.class);

        new EditProfilePage().open().getUserName().shouldHave(text(newName));
        assertEquals(newName, UserSteps.getCustomerName(user));
    }

}
