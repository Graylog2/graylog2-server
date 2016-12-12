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
package org.graylog2.system.stats;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.google.auto.value.AutoValue;
import org.graylog.autovalue.WithBeanGetter;
import org.graylog2.plugin.inputs.Extractor;
import org.graylog2.system.stats.elasticsearch.ElasticsearchStats;
import org.graylog2.system.stats.mongo.MongoStats;

import java.util.Map;

@JsonAutoDetect
@AutoValue
@WithBeanGetter
public abstract class ClusterStats {
    @JsonProperty("elasticsearch")
    public abstract ElasticsearchStats elasticsearchStats();

    @JsonProperty("mongo")
    public abstract MongoStats mongoStats();

    @JsonProperty
    public abstract long streamCount();

    @JsonProperty
    public abstract long streamRuleCount();

    @JsonProperty
    public abstract Map<String, Long> streamRuleCountByStream();

    @JsonProperty
    public abstract long userCount();

    @JsonProperty
    public abstract long outputCount();

    @JsonProperty
    public abstract Map<String, Long> outputCountByType();

    @JsonProperty
    public abstract long dashboardCount();

    @JsonProperty
    public abstract long inputCount();

    @JsonProperty
    public abstract long globalInputCount();

    @JsonProperty
    public abstract Map<String, Long> inputCountByType();

    @JsonProperty
    public abstract long extractorCount();

    @JsonProperty
    public abstract Map<Extractor.Type, Long> extractorCountByType();

    @JsonProperty
    public abstract long contentPackCount();

    @JsonProperty
    public abstract LdapStats ldapStats();

    @JsonProperty
    public abstract AlarmStats alarmStats();

    public static ClusterStats create(ElasticsearchStats elasticsearchStats,
                                      MongoStats mongoStats,
                                      long streamCount,
                                      long streamRuleCount,
                                      Map<String, Long> streamRuleCountByStream,
                                      long userCount,
                                      long outputCount,
                                      Map<String, Long> outputCountByType,
                                      long dashboardCount,
                                      long inputCount,
                                      long globalInputCount,
                                      Map<String, Long> inputCountByType,
                                      long extractorCount,
                                      Map<Extractor.Type, Long> extractorCountByType,
                                      long contentPackCount,
                                      LdapStats ldapStats,
                                      AlarmStats alarmStats) {
        return new AutoValue_ClusterStats(
                elasticsearchStats,
                mongoStats,
                streamCount,
                streamRuleCount,
                streamRuleCountByStream,
                userCount,
                outputCount,
                outputCountByType,
                dashboardCount,
                inputCount,
                globalInputCount,
                inputCountByType,
                extractorCount,
                extractorCountByType,
                contentPackCount,
                ldapStats,
                alarmStats);
    }
}
