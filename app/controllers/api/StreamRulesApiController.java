package controllers.api;

import com.google.common.collect.Lists;
import controllers.AuthenticatedController;
import org.graylog2.restclient.models.StreamRule;
import play.libs.Json;
import play.mvc.Result;

import java.util.List;

public class StreamRulesApiController extends AuthenticatedController {
    public class Type {
        public final int id;
        public final String shortDesc;
        public final String longDesc;

        public Type(int id, String shortDesc, String longDesc) {
            this.id = id;
            this.shortDesc = shortDesc;
            this.longDesc = longDesc;
        }
    }
    public Result types(String streamId) {
        final List<Type> types = Lists.newArrayList();
        for (StreamRule.Type type : StreamRule.Type.values()) {
            types.add(new Type(type.getId(), type.getShortDesc(), type.getLongDesc()));
        }
        return ok(Json.toJson(types));
    }
}
