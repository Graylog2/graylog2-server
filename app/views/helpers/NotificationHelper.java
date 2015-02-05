package views.helpers;

public class NotificationHelper {
    public static String linkToKnowledgeBase(String article, String body) {
        StringBuilder url = new StringBuilder("https://www.graylog.org/documentation/");
        url.append(article);

        return "<a href='" + url + "' target='_blank'>" + body + "</a>";
    }
}
