package ui.iteration2;

import api.iteration2.BaseTest;
import api.iteration2.configs.Config;
import api.iteration2.specs.RequestSpecs;
import com.codeborne.selenide.Configuration;
import com.codeborne.selenide.Selenide;
import org.junit.jupiter.api.BeforeAll;

import java.util.Map;

import static com.codeborne.selenide.Selenide.executeJavaScript;

public class BaseUiTest extends BaseTest {
    @BeforeAll
    public static void setupSelenoid() {
        Configuration.remote = Config.getProperty("uiRemote");
        Configuration.baseUrl = Config.getProperty("uiBaseUrl");
        Configuration.browser = Config.getProperty("browser");
        Configuration.browserSize = Config.getProperty("browserSize");

        Configuration.browserCapabilities.setCapability("selenoid:options",
                Map.of("enableVNC", true, "enableLog", true));

    }


    public static void authAsUser(String username, String password){
        Selenide.open("/");
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);
        Selenide.refresh();

    }

    public static void switchUser(String username, String password) {
        // 1️⃣ Очистка всего состояния браузера
        Selenide.clearBrowserCookies();
        Selenide.clearBrowserLocalStorage();

        // 2️⃣ Открываем базовую страницу приложения
        Selenide.open("/");

        // 3️⃣ Устанавливаем новый токен пользователя
        String userAuthHeader = RequestSpecs.getUserAuthHeader(username, password);
        executeJavaScript("localStorage.setItem('authToken', arguments[0]);", userAuthHeader);

        // 4️⃣ Обновляем страницу
        Selenide.refresh();

    }
}
