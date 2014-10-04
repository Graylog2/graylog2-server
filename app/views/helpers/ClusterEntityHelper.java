package views.helpers;

import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Node;
import org.graylog2.restclient.models.NodeService;
import org.graylog2.restclient.models.Radio;
import play.twirl.api.Html;

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
