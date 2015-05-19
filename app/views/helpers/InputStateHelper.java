package views.helpers;

import com.google.common.collect.Maps;
import org.graylog2.restclient.models.ClusterEntity;
import org.graylog2.restclient.models.InputState;
import org.graylog2.restclient.models.InputState.InputStateType;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputStateHelper {
    public static Map<InputStateType, Integer> stateCounts(Map<ClusterEntity, InputState> inputStateMap) {
        Map<InputStateType, Integer> results = Maps.newHashMap();
        for (Map.Entry<ClusterEntity, InputState> entry : inputStateMap.entrySet()) {
            Integer count = results.get(entry.getValue().getState());
            if (count == null) {
                results.put(entry.getValue().getState(), 1);
            } else {
                results.put(entry.getValue().getState(), count + 1);
            }
        }

        return results;
    }

    public static String labelClassForState(InputStateType state) {
        switch(state) {
            case RUNNING: return "success";
            case FAILED: return "danger";
            case STARTING: return "info";
            default: return "warning";
        }
    }
}
