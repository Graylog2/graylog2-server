package views.helpers;

import com.google.common.collect.Maps;
import models.InputState;
import models.Node;

import java.util.Map;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class InputStateHelper {
    public static Map<String, Integer> stateCounts(Map<Node, InputState> inputStateMap) {
        Map<String, Integer> results = Maps.newHashMap();
        for (Map.Entry<Node, InputState> entry : inputStateMap.entrySet()) {
            Integer count = results.get(entry.getValue().getState());
            if (count == null) {
                results.put(entry.getValue().getState(), 1);
            } else {
                results.put(entry.getValue().getState(), count + 1);
            }
        }

        return results;
    }

    public static String labelClassForState(String state) {
        switch(state) {
            case "running": return "success";
            case "failed": return "important";
            case "starting": return "info";
            default: return "warning";
        }
    }
}
