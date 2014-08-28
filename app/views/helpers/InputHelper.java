package views.helpers;

import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.Radio;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputHelper {
    public static String sourceFieldForNode(ClusterEntity clusterEntity) {
        if (clusterEntity instanceof Radio)
            return "gl2_source_radio_input";
        else
            return "gl2_source_input";
    }
}
