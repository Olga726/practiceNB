package common.storage;
import api.models.UserModel;


public class SessionStorage {
        private static final SessionStorage INSTANCE = new SessionStorage();
        private UserModel user;

        private SessionStorage() {}

        public static void setUser(UserModel u) {
            INSTANCE.user = u;
        }

        public static UserModel getUser(int i) {
            if (INSTANCE.user == null) {
                throw new IllegalStateException("User not set in SessionStorage");
            }
            return INSTANCE.user;
        }

        public static void clear() {
            INSTANCE.user = null;
        }
    }


