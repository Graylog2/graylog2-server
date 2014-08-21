package views.helpers;

import controllers.routes;
import org.graylog2.restclient.models.Startpage;
import play.mvc.Call;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class StartpageRouteHelper {
    public static Call getCall(Startpage startPage) {
        switch (startPage.getType()) {
            case STREAM:
                return routes.StreamSearchController.index(startPage.getId(), "*", "relative", 3600, "", "", "", "", 0, "", "", "", "", -1); // TODO fields and width
            case DASHBOARD:
                return routes.DashboardsController.show(startPage.getId());
            default:
                return null;
        }
    }
}
