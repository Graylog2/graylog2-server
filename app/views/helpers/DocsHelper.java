package views.helpers;

import org.graylog2.restclient.lib.Version;

public class DocsHelper {

    public static String linkToDocs(String page, String title) {
        StringBuilder sb = new StringBuilder("<a href=\"http://docs.graylog.org/en/")
                .append(Version.VERSION.getBranchName())
                .append("/pages/")
                .append(page)
                .append("\" target=\"_blank\">")
                .append(title)
                .append("</a>");

        return sb.toString();
    }

}
