package common.extensions;

import api.steps.UserSteps;
import api.models.UserModel;
import com.codeborne.selenide.WebDriverRunner;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.AfterEachCallback;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.platform.commons.support.AnnotationSupport;
import ui.iteration2.pages.BasePage;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;


public class UserSessionExtension implements BeforeEachCallback, AfterEachCallback {

    private final ThreadLocal<List<UserModel>> createdUsers = new ThreadLocal<>();

    @Override
    public void beforeEach(ExtensionContext context) throws Exception {
        UserSession annotation =
                AnnotationSupport.findAnnotation(
                        context.getTestMethod(), UserSession.class
                ).orElse(
                        AnnotationSupport.findAnnotation(
                                context.getTestClass(), UserSession.class
                        ).orElse(null)
                );

        /*UserSession annotation = context.getRequiredTestMethod().getAnnotation(UserSession.class);
        if (annotation == null) {
            annotation = context.getRequiredTestClass()
                    .getAnnotation(UserSession.class);
        }
        */
        System.out.println("beforeEach: UserSession annotation = " + annotation);
        if (annotation == null) return;

        int userCount = annotation.value();
        int authIndex = annotation.auth();

        if (authIndex > userCount) {
            throw new IllegalArgumentException(
                    "auth index cannot be greater than created users"
            );
        }

        List<UserModel> users = new ArrayList<>();
        for (int i = 0; i < userCount; i++) {
            users.add(UserSteps.createUser());
        }

        SessionStorage.setUser(users);

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

        WebDriverRunner.closeWebDriver();
    }
}
