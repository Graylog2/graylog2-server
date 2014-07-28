package views.helpers;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class NotificationHelper {
    public static String linkToKnowledgeBase(String article, String body) {
        StringBuilder url = new StringBuilder("http://support.torch.sh/help/kb/graylog2-server/");
        url.append(article);

        return "<a href='" + url + "' target='_blank'>" + body + "</a>";
    }
}
