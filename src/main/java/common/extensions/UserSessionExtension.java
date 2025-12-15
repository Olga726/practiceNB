package common.extensions;

import api.steps.UserSteps;
import api.models.UserModel;
import common.annotations.UserSession;
import common.storage.SessionStorage;
import org.junit.jupiter.api.extension.BeforeEachCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import ui.iteration2.pages.BasePage;

import java.util.LinkedList;
import java.util.List;


public class UserSessionExtension implements BeforeEachCallback {

        @Override
        public void beforeEach(ExtensionContext context) throws Exception {
            // Проверяем, что у теста есть аннотация @UserSession
            UserSession annotation = context.getRequiredTestMethod().getAnnotation(UserSession.class);
            if (annotation == null) return;

            int userCount = annotation.value();
            SessionStorage.clear(); // очищаем прошлую сессию пользователя

            List<UserModel> users = new LinkedList<>();

            for (int i = 0; i < userCount; i++) {

                UserModel user = UserSteps.createUser(); // создаём пользователя
                users.add(user);
            }

            // Сохраняем пользователей в SessionStorage (только пользователи)
            if (!users.isEmpty()) {
                // Берём первого пользователя или по индексу auth
                SessionStorage.setUser(users.get(0));
            }

            // Авторизация пользователя, если указано
            int authIndex = annotation.auth(); // номер пользователя для авторизации
            UserModel userToAuth = users.get(authIndex - 1); // индекс с 1

            BasePage.authAsUser(userToAuth.getUsername(), userToAuth.getPassword());
        }
    }
