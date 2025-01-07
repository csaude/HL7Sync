package mz.org.csaude.hl7sync.util;

import org.apache.commons.lang.StringUtils;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class Hl7Util {
    public static String getCurrentTimeStamp() {
        return new SimpleDateFormat("yyyy-MM-dd_HH:mm:ss").format(new Date());
    }

    public static String listToString(List<String> locationsBySite) {
        String locations = StringUtils.join(locationsBySite, "','");
        locations = "'" + locations + "'";
        return locations;
    }

}
