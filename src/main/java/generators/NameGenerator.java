package generators;

import com.mifmif.common.regex.Generex;

public class NameGenerator {
    public static String generateName() {
        String regex = "^[A-Za-z]+ [A-Za-z]+$".replaceAll("^\\^", "").replaceAll("\\$$", "");
        Generex generex = new Generex(regex);
        return generex.random();
    }

}
