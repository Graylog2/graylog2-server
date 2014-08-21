package views.helpers;

import controllers.routes;
import org.graylog2.restclient.lib.DateTools;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.dashboards.widgets.DashboardWidget;
import org.joda.time.DateTime;
import play.mvc.Call;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class DashboardWidgetRouteHelper {
    public static Call replayRoute(DashboardWidget widget) {
        if (widget.getStreamId() == null || widget.getStreamId().isEmpty()) {
            return prepareNonStreamBoundReplayRoute(widget.getQuery(), widget.getTimerange());
        } else {
            return prepareStreamBoundReplayRoute(widget.getStreamId(), widget.getQuery(), widget.getTimerange());
        }
    }

    protected static Call prepareNonStreamBoundReplayRoute(String query, TimeRange timerange) {
        return routes.SearchController.index(
                (query == null) ? "" : query,
                timerange.getType().name().toLowerCase(),
                timerange.getQueryParams().containsKey("range") ? Integer.valueOf(timerange.nullSafeParam("range")) : 0,
                timerange.getQueryParams().containsKey("from") ? new DateTime(timerange.nullSafeParam("from")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.getQueryParams().containsKey("to") ? new DateTime(timerange.nullSafeParam("to")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.nullSafeParam("keyword"),
                "minute",
                0,
                "",
                "",
                "",
                "", // TODO fields
                -1
        );
    }

    protected static Call prepareStreamBoundReplayRoute(String streamId, String query, TimeRange timerange) {
        return routes.StreamSearchController.index(
                streamId,
                (query == null) ? "" : query,
                timerange.getType().name().toLowerCase(),
                timerange.getQueryParams().containsKey("range") ? Integer.valueOf(timerange.nullSafeParam("range")) : 0,
                timerange.getQueryParams().containsKey("from") ? new DateTime(timerange.nullSafeParam("from")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.getQueryParams().containsKey("to") ? new DateTime(timerange.nullSafeParam("to")).toString(DateTools.SHORT_DATE_FORMAT) : "",
                timerange.nullSafeParam("keyword"),
                "minute",
                0,
                "",
                "",
                "",
                "", // TODO fields
                -1
        );
    }
}
