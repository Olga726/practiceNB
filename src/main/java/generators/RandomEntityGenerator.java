package generators;
import com.mifmif.common.regex.Generex;


import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.util.Random;
import java.util.UUID;

public class RandomEntityGenerator {

    private static final Random random = new Random();
    private static final String DEFAULT_STRING_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
    private static final String PASSWORD_SPECIALS = "@#%^&+=";

    @SuppressWarnings("unchecked")
    public static <T> T generate(Class<T> clazz) {
        try {
        // Специальная проверка для String
            if (clazz.equals(String.class)) {
                return (T) UUID.randomUUID().toString().substring(0, 8);
            }


            T instance = clazz.getDeclaredConstructor().newInstance();

            for (Field field : clazz.getDeclaredFields()) {
                if (Modifier.isStatic(field.getModifiers())) continue; // пропускаем static
                field.setAccessible(true);
                Class<?> type = field.getType();

                // Проверяем аннотацию @GeneratingRule
                if (field.isAnnotationPresent(GeneratingRule.class)) {
                    String regex = field.getAnnotation(GeneratingRule.class).regex();

                    if (type.isEnum()) {
                        Object[] enumValues = type.getEnumConstants();
                        String name = regex.replaceAll("[\\^\\$]", "").trim();
                        boolean found = false;
                        for (Object e : enumValues) {
                            if (((Enum<?>) e).name().equals(name)) {
                                field.set(instance, e);
                                found = true;
                                break;
                            }
                        }
                        if (!found) {
                            throw new RuntimeException("Enum constant not found: " + name + " for field " + field.getName());
                        }
                    } else if (regex.contains("(?=")) {
                        field.set(instance, randomPassword(8, 20));
                    } else {
                        Generex generex = new Generex(regex.replaceAll("^\\^", "").replaceAll("\\$$", ""));
                        field.set(instance, generex.random());
                    }
                    continue;
                }

                // Генерация по типу, если нет @GeneratingRule
                if (type == String.class) {
                    field.set(instance, randomString(10));
                } else if (type == int.class || type == Integer.class) {
                    field.set(instance, random.nextInt(1000));
                } else if (type == long.class || type == Long.class) {
                    field.set(instance, Math.abs(random.nextLong() % 1_000_000));
                } else if (type == float.class || type == Float.class) {
                    field.set(instance, random.nextFloat() * 1000);
                } else if (type == double.class || type == Double.class) {
                    field.set(instance, random.nextDouble() * 1000);
                } else if (type == boolean.class || type == Boolean.class) {
                    field.set(instance, random.nextBoolean());
                } else if (type.isEnum()) {
                    Object[] enumValues = type.getEnumConstants();
                    field.set(instance, enumValues[random.nextInt(enumValues.length)]);
                } else {
                    // Вложенный POJO — рекурсивно вызываем генератор
                    field.set(instance, generate(type));
                }
            }

            return instance;

        } catch (Exception e) {
            throw new RuntimeException("Failed to generate instance for " + clazz.getName(), e);
        }
    }

    // Генерация случайного String
    private static String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(DEFAULT_STRING_CHARS.charAt(random.nextInt(DEFAULT_STRING_CHARS.length())));
        }
        return sb.toString();
    }

    // Генерация валидного пароля
    private static String randomPassword(int minLength, int maxLength) {
        int length = minLength + random.nextInt(maxLength - minLength + 1);
        StringBuilder sb = new StringBuilder();

        // Гарантируем хотя бы один символ каждого типа
        sb.append((char) ('A' + random.nextInt(26))); // заглавная
        sb.append((char) ('a' + random.nextInt(26))); // строчная
        sb.append((char) ('0' + random.nextInt(10))); // цифра
        sb.append(PASSWORD_SPECIALS.charAt(random.nextInt(PASSWORD_SPECIALS.length()))); // спецсимвол

        String allChars = DEFAULT_STRING_CHARS + PASSWORD_SPECIALS;
        for (int i = sb.length(); i < length; i++) {
            sb.append(allChars.charAt(random.nextInt(allChars.length())));
        }

        return shuffleString(sb.toString());
    }

    private static String shuffleString(String input) {
        char[] a = input.toCharArray();
        for (int i = a.length - 1; i > 0; i--) {
            int j = random.nextInt(i + 1);
            char temp = a[i];
            a[i] = a[j];
            a[j] = temp;
        }
        return new String(a);
    }
}
