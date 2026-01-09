package common.storage;
import api.models.UserModel;

import java.util.ArrayList;
import java.util.List;


public class SessionStorage {

    private static final ThreadLocal<SessionStorage> INSTANCE = ThreadLocal.withInitial(SessionStorage::new);
        private final List<UserModel> users = new ArrayList<>();

        private SessionStorage() {}

        public static void setUser(List<UserModel> users) {
            if (users == null || users.isEmpty()) {
                throw new IllegalArgumentException("Users list must not be null or empty");
            }
            INSTANCE.get().users.clear();
            INSTANCE.get().users.addAll(users);
        }

        public static UserModel getUser(int i) {
            List<UserModel> users = INSTANCE.get().users;
            if (users.isEmpty()) {
                throw new IllegalStateException("No users stored in SessionStorage");
            }
            if (i < 1 || i > users.size()) {
                throw new IndexOutOfBoundsException(
                        "User index must be between 1 and " + users.size()
                );
            }
            return users.get(i - 1);
        }

        public static void clear() {
            INSTANCE.get().users.clear();
        }
    }


