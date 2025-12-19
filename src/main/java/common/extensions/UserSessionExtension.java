package common.extensions;

import api.steps.UserSteps;
import api.models.UserModel;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.iteration2.pages.BasePage;

import java.util.LinkedList;
import java.util.List;


public class UserSessionExtension implements BeforeEachCallback, AfterEachCallback {

    private final ThreadLocal<List<UserModel>> createdUsers = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        UserSession annotation = context.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation == null) return;

        int userCount = annotation.value();
        SessionStorage.clear();

        List<UserModel> users = new LinkedList<>();
        for (int i = 0; i < userCount; i++) {
            UserModel user = UserSteps.createUser();
            users.add(user);
        }

        if (!users.isEmpty()) {
            SessionStorage.setUser(users.get(0));
        }

        int authIndex = annotation.auth();
        UserModel userToAuth = users.get(authIndex - 1);
        BasePage.authAsUser(userToAuth.getUsername(), userToAuth.getPassword());

        createdUsers.set(users); // сохраняем для удаления
    }

    @Override
    public void afterEach(ExtensionContext context) throws Exception {
        List<UserModel> users = createdUsers.get();
        if (users != null) {
            for (UserModel user : users) {
                try {
                    UserSteps.deleteUsers(user); // безопасное удаление
                } catch (Exception ignored) {}
            }
        }
        SessionStorage.clear();
        createdUsers.remove();
    }
}
