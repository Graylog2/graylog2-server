package views.helpers;

import models.ClusterEntity;
import models.Node;
import models.NodeService;
import models.Radio;
import play.api.templates.Html;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class ClusterEntityHelper {

    public static Html linkToEntity(ClusterEntity entity) {
        if (entity instanceof Node)
            return views.html.partials.node_title_link.render((Node) entity);

        if (entity instanceof Radio)
            return views.html.partials.radio_title_link.render((Radio) entity);

        return null;
    }
}
