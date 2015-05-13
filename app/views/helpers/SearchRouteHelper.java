package views.helpers;

import controllers.routes;
import org.graylog2.restclient.lib.Tools;
import org.graylog2.restclient.lib.timeranges.TimeRange;
import org.graylog2.restclient.models.SearchSort;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.UniversalSearch;
import play.mvc.Call;
import play.mvc.Http;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class SearchRouteHelper {

    public static Call getRoute(UniversalSearch search,
                                Http.Request request,
                                int page) {
        SearchSort order = search.getOrder();
        return getRoute(search, request, page, order.getField(), order.getDirection().toString().toLowerCase());
    }


    public static Call getRoute(UniversalSearch search,
                                Http.Request request,
                                int page,
                                String sortField,
                                String sortOrder) {
        int relative = Tools.intSearchParamOrEmpty(request, "relative");
        String from = Tools.stringSearchParamOrEmpty(request, "from");
        String to = Tools.stringSearchParamOrEmpty(request, "to");
        String keyword = Tools.stringSearchParamOrEmpty(request, "keyword");
        String interval = Tools.stringSearchParamOrEmpty(request, "interval");
        String fields = Tools.stringSearchParamOrEmpty(request, "fields");
        int width = request.getQueryString("width") == null ? -1 : Integer.valueOf(request.getQueryString("width"));

        String filter = search.getFilter();
        String query = search.getQuery();
        TimeRange timeRange = search.getTimeRange();

        // TODO we desperately need to pass the streamid and then build the filter here, instead of passing the filter and then trying to reassemble the streamid.
        if (filter != null && filter.startsWith("streams:")) {
            return routes.StreamSearchController.index(
                    filter.split(":")[1],
                    query,
                    timeRange.getType().toString().toLowerCase(),
                    relative,
                    from,
                    to,
                    keyword,
                    interval,
                    page,
                    "",
                    sortField,
                    sortOrder,
                    fields,
                    width
            );
        } else {
            return routes.SearchController.index(
                    query,
                    timeRange.getType().toString().toLowerCase(),
                    relative,
                    from,
                    to,
                    keyword,
                    interval,
                    page,
                    "",
                    sortField,
                    sortOrder,
                    fields,
                    width
            );
        }
    }

    public static Call getCsvRoute(Http.Request request, Stream stream, UniversalSearch search) {
        int relative = Tools.intSearchParamOrEmpty(request, "relative");
        String from = Tools.stringSearchParamOrEmpty(request, "from");
        String to = Tools.stringSearchParamOrEmpty(request, "to");
        String keyword = Tools.stringSearchParamOrEmpty(request, "keyword");
        String fields = Tools.stringSearchParamOrEmpty(request, "fields");

        String query = search.getQuery();
        TimeRange timeRange = search.getTimeRange();

        if (stream == null) {
            return routes.SearchControllerV2.exportAsCsv(
                    "",
                    query,
                    timeRange.getType().toString().toLowerCase(),
                    relative,
                    from,
                    to,
                    keyword,
                    fields
            );
        } else {
            return routes.StreamSearchController.exportAsCsv(
                    stream.getId(),
                    query,
                    timeRange.getType().toString().toLowerCase(),
                    relative,
                    from,
                    to,
                    keyword,
                    fields
            );
        }
    }
}
