/**
 * This file is part of Graylog.
 *
 * Graylog is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Graylog is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Graylog.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.graylog2.alerts.types;

import com.google.common.collect.Lists;
import com.google.inject.assistedinject.Assisted;
import com.google.inject.assistedinject.AssistedInject;
import org.graylog2.Configuration;
import org.graylog2.alerts.AbstractAlertCondition;
import org.graylog2.indexer.InvalidRangeFormatException;
import org.graylog2.indexer.results.ResultMessage;
import org.graylog2.indexer.results.SearchResult;
import org.graylog2.indexer.searches.Searches;
import org.graylog2.indexer.searches.Sorting;
import org.graylog2.indexer.searches.timeranges.InvalidRangeParametersException;
import org.graylog2.indexer.searches.timeranges.RelativeRange;
import org.graylog2.plugin.Message;
import org.graylog2.plugin.MessageSummary;
import org.graylog2.plugin.Tools;
import org.graylog2.plugin.streams.Stream;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nullable;
import java.util.List;
import java.util.Map;

public class FieldStringValueAlertCondition extends AbstractAlertCondition {
    private static final Logger LOG = LoggerFactory.getLogger(FieldStringValueAlertCondition.class);

    private final Searches searches;
    private final Configuration configuration;
    private final String field;
    private final String value;

    public interface Factory {
        FieldStringValueAlertCondition createAlertCondition(Stream stream, String id, DateTime createdAt, @Assisted("userid") String creatorUserId, Map<String, Object> parameters);
    }

    @AssistedInject
    public FieldStringValueAlertCondition(Searches searches, Configuration configuration, @Assisted Stream stream, @Nullable @Assisted String id, @Assisted DateTime createdAt, @Assisted("userid") String creatorUserId, @Assisted Map<String, Object> parameters) {
        super(stream, id, Type.FIELD_STRING_VALUE, createdAt, creatorUserId, parameters);
        this.searches = searches;
        this.configuration = configuration;
        this.field = (String) parameters.get("field");
        this.value = (String) parameters.get("value");
    }

    @Override
    protected CheckResult runCheck() {
        String filter = "streams:" + stream.getId();
        String query = new StringBuilder()
                .append(field).append(":\"").append(value).append("\"")
                .toString();
        Integer backlogSize = getBacklog();

        if(backlogSize == null || backlogSize == 0) {
            LOG.debug("No backlog size configured. Setting to default 100.");
            backlogSize = 100;
        }

        try {
            SearchResult result = searches.search(
                    query,
                    filter,
                    new RelativeRange(configuration.getAlertCheckInterval() * 60),
                    backlogSize,
                    0,
                    new Sorting("timestamp", Sorting.Direction.DESC)
            );

            final List<MessageSummary> summaries = Lists.newArrayList();
            if (getBacklog() > 0) {
                for (ResultMessage resultMessage : result.getResults()) {
                    final Message msg = new Message(resultMessage.getMessage());
                    summaries.add(new MessageSummary(resultMessage.getIndex(), msg));
                }
            }

            final long count = result.getTotalResults();

            final String resultDescription = "Stream received messages matching <" + query + "> "
                    + "(Current grace time: " + grace + " minutes)";

            if (count > 0) {
                LOG.debug("Alert check <{}> found [{}] messages.", id, count);
                return new CheckResult(true, this, resultDescription, Tools.iso8601(), summaries);
            } else {
                LOG.debug("Alert check <{}> returned no results.", id);
                return new NegativeCheckResult(this);
            }
        } catch (InvalidRangeParametersException e) {
            // cannot happen lol
            LOG.error("Invalid timerange.", e);
            return null;
        } catch (InvalidRangeFormatException e) {
            // lol same here
            LOG.error("Invalid timerange format.", e);
            return null;
        }
    }

    @Override
    public String getDescription() {
        return "field: " + field + ", value: " + value;
    }

}
