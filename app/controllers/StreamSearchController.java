package controllers;

import com.google.inject.Inject;
import org.graylog2.restclient.lib.APIException;
import org.graylog2.restclient.lib.ApiClient;
import org.graylog2.restclient.lib.ServerNodes;
import org.graylog2.restclient.models.SearchSort;
import org.graylog2.restclient.models.Startpage;
import org.graylog2.restclient.models.Stream;
import org.graylog2.restclient.models.StreamService;
import play.mvc.Result;

import java.io.IOException;

public class StreamSearchController extends SearchController {
    @Inject
    private StreamService streamService;
    @Inject
    private ServerNodes serverNodes;

    public Result index(String streamId,
                        String q,
                        String rangeType, int relative,
                        String from, String to,
                        String keyword, String interval,
                        int page,
                        String savedSearchId,
                        String sortField, String sortOrder,
                        String fields,
                        int displayWidth) {
        SearchSort sort = buildSearchSort(sortField, sortOrder);

        Stream stream;
        try {
            stream = streamService.get(streamId);
        } catch (IOException e) {
            return status(504, views.html.errors.error.render(ApiClient.ERROR_MSG_IO, e, request()));
        } catch (APIException e) {
            if (e.getHttpCode() == NOT_FOUND || e.getHttpCode() == FORBIDDEN) {
                String msg = "The requested stream was deleted and no longer exists.";
                final Startpage startpage = currentUser().getStartpage();
                if (startpage != null) {
                    if (new Startpage(Startpage.Type.STREAM, streamId).equals(startpage)) {
                        msg += " Please reset your startpage.";
                    }
                }
                flash("error", msg);
                return redirect(routes.StreamsController.index());
            }

            String message = "Unable to fetch stream. We expected HTTP 200, but got a HTTP " + e.getHttpCode() + ".";
            return status(504, views.html.errors.error.render(message, e, request()));
        }

        String filter = "streams:" + streamId;

        return renderSearch(q, rangeType, relative, from, to, keyword, interval, page, savedSearchId, fields, displayWidth, sort, stream, filter);
    }

    @Override
    public Result exportAsCsv(String q, String streamId, String rangeType, int relative, String from, String to, String keyword, String fields) {
        return super.exportAsCsv(q, "streams:" + streamId, rangeType, relative, from, to, keyword, fields);
    }
}
