/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
package org.graylog.datanode.metrics;

import org.graylog.shaded.opensearch2.org.apache.http.HttpEntity;
import org.graylog.shaded.opensearch2.org.opensearch.client.Response;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestClient;
import org.graylog.shaded.opensearch2.org.opensearch.client.RestHighLevelClient;
import org.graylog2.shared.bindings.providers.ObjectMapperProvider;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class ClusterStatMetricsCollectorTest {

    ClusterStatMetricsCollector collector;
    @Mock
    RestHighLevelClient client;

    @BeforeEach
    public void setUp() throws IOException {
        Response response = mock(Response.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(clusterStatResponse.getBytes(Charset.defaultCharset())));
        RestClient lowLevelClient = mock(RestClient.class);
        when(client.getLowLevelClient()).thenReturn(lowLevelClient);
        when(lowLevelClient.performRequest(any())).thenReturn(response);
        this.collector = new ClusterStatMetricsCollector(client, new ObjectMapperProvider().get());
    }

    @Test
    public void getClusterMetrics() {
        final Map<String, Object> previousMetrics = Map.of("search_ops", 5);
        Map<String, Object> clusterMetrics = collector.getClusterMetrics(previousMetrics);
        assertThat(clusterMetrics.get("doc_count")).isEqualTo(6206956);
        assertThat(clusterMetrics.get("search_ops")).isEqualTo(13);
        assertThat(clusterMetrics.get("search_ops_rate")).isEqualTo(8L);
        String[] allMetrics = Arrays.stream(ClusterStatMetrics.values()).map(ClusterStatMetrics::getFieldName).toArray(String[]::new);
        assertThat(clusterMetrics).containsKeys(allMetrics);
    }

    private final static String clusterStatResponse = """
            {
                "_shards": {
                    "total": 38,
                    "successful": 38,
                    "failed": 0
                },
                "_all": {
                    "primaries": {
                        "docs": {
                            "count": 6206956,
                            "deleted": 3
                        },
                        "store": {
                            "size_in_bytes": 1781904103,
                            "reserved_in_bytes": 0
                        },
                        "indexing": {
                            "index_total": 6188311,
                            "index_time_in_millis": 505860,
                            "index_current": 0,
                            "index_failed": 10,
                            "delete_total": 0,
                            "delete_time_in_millis": 0,
                            "delete_current": 0,
                            "noop_update_total": 0,
                            "is_throttled": false,
                            "throttle_time_in_millis": 0,
                            "doc_status": {}
                        },
                        "get": {
                            "total": 175,
                            "time_in_millis": 103,
                            "exists_total": 173,
                            "exists_time_in_millis": 103,
                            "missing_total": 2,
                            "missing_time_in_millis": 0,
                            "current": 0
                        },
                        "search": {
                            "open_contexts": 0,
                            "query_total": 11,
                            "query_time_in_millis": 40,
                            "query_current": 0,
                            "fetch_total": 11,
                            "fetch_time_in_millis": 0,
                            "fetch_current": 0,
                            "scroll_total": 0,
                            "scroll_time_in_millis": 0,
                            "scroll_current": 0,
                            "point_in_time_total": 0,
                            "point_in_time_time_in_millis": 0,
                            "point_in_time_current": 0,
                            "suggest_total": 0,
                            "suggest_time_in_millis": 0,
                            "suggest_current": 0
                        },
                        "merges": {
                            "current": 0,
                            "current_docs": 0,
                            "current_size_in_bytes": 0,
                            "total": 831,
                            "total_time_in_millis": 246474,
                            "total_docs": 20378299,
                            "total_size_in_bytes": 5110146545,
                            "total_stopped_time_in_millis": 0,
                            "total_throttled_time_in_millis": 39458,
                            "total_auto_throttle_in_bytes": 679234600,
                            "unreferenced_file_cleanups_performed": 0
                        },
                        "refresh": {
                            "total": 52698,
                            "total_time_in_millis": 3326040,
                            "external_total": 52625,
                            "external_total_time_in_millis": 3346017,
                            "listeners": 0
                        },
                        "flush": {
                            "total": 68,
                            "periodic": 66,
                            "total_time_in_millis": 30243
                        },
                        "warmer": {
                            "current": 0,
                            "total": 7729,
                            "total_time_in_millis": 165
                        },
                        "query_cache": {
                            "memory_size_in_bytes": 0,
                            "total_count": 0,
                            "hit_count": 0,
                            "miss_count": 0,
                            "cache_size": 0,
                            "cache_count": 0,
                            "evictions": 0
                        },
                        "fielddata": {
                            "memory_size_in_bytes": 0,
                            "evictions": 0
                        },
                        "completion": {
                            "size_in_bytes": 0
                        },
                        "segments": {
                            "count": 93,
                            "memory_in_bytes": 0,
                            "terms_memory_in_bytes": 0,
                            "stored_fields_memory_in_bytes": 0,
                            "term_vectors_memory_in_bytes": 0,
                            "norms_memory_in_bytes": 0,
                            "points_memory_in_bytes": 0,
                            "doc_values_memory_in_bytes": 0,
                            "index_writer_memory_in_bytes": 1692740,
                            "version_map_memory_in_bytes": 144840,
                            "fixed_bit_set_memory_in_bytes": 0,
                            "max_unsafe_auto_id_timestamp": -1,
                            "remote_store": {
                                "upload": {
                                    "total_upload_size": {
                                        "started_bytes": 0,
                                        "succeeded_bytes": 0,
                                        "failed_bytes": 0
                                    },
                                    "refresh_size_lag": {
                                        "total_bytes": 0,
                                        "max_bytes": 0
                                    },
                                    "max_refresh_time_lag_in_millis": 0,
                                    "total_time_spent_in_millis": 0
                                },
                                "download": {
                                    "total_download_size": {
                                        "started_bytes": 0,
                                        "succeeded_bytes": 0,
                                        "failed_bytes": 0
                                    },
                                    "total_time_spent_in_millis": 0
                                }
                            },
                            "segment_replication": {
                                "max_bytes_behind": "0b",
                                "total_bytes_behind": "0b",
                                "max_replication_lag": "0s"
                            },
                            "file_sizes": {}
                        },
                        "translog": {
                            "operations": 1006222,
                            "size_in_bytes": 833969509,
                            "uncommitted_operations": 1006222,
                            "uncommitted_size_in_bytes": 833969509,
                            "earliest_last_modified_age": 24209,
                            "remote_store": {
                                "upload": {
                                    "total_uploads": {
                                        "started": 0,
                                        "failed": 0,
                                        "succeeded": 0
                                    },
                                    "total_upload_size": {
                                        "started_bytes": 0,
                                        "failed_bytes": 0,
                                        "succeeded_bytes": 0
                                    }
                                }
                            }
                        },
                        "request_cache": {
                            "memory_size_in_bytes": 0,
                            "evictions": 0,
                            "hit_count": 2,
                            "miss_count": 9
                        },
                        "recovery": {
                            "current_as_source": 0,
                            "current_as_target": 0,
                            "throttle_time_in_millis": 0
                        }
                    },
                    "total": {
                        "docs": {
                            "count": 6206974,
                            "deleted": 3
                        },
                        "store": {
                            "size_in_bytes": 1782014741,
                            "reserved_in_bytes": 0
                        },
                        "indexing": {
                            "index_total": 6189056,
                            "index_time_in_millis": 507364,
                            "index_current": 0,
                            "index_failed": 10,
                            "delete_total": 0,
                            "delete_time_in_millis": 0,
                            "delete_current": 0,
                            "noop_update_total": 0,
                            "is_throttled": false,
                            "throttle_time_in_millis": 0,
                            "doc_status": {}
                        },
                        "get": {
                            "total": 206,
                            "time_in_millis": 205,
                            "exists_total": 204,
                            "exists_time_in_millis": 205,
                            "missing_total": 2,
                            "missing_time_in_millis": 0,
                            "current": 0
                        },
                        "search": {
                            "open_contexts": 0,
                            "query_total": 13,
                            "query_time_in_millis": 96,
                            "query_current": 0,
                            "fetch_total": 13,
                            "fetch_time_in_millis": 0,
                            "fetch_current": 0,
                            "scroll_total": 0,
                            "scroll_time_in_millis": 0,
                            "scroll_current": 0,
                            "point_in_time_total": 0,
                            "point_in_time_time_in_millis": 0,
                            "point_in_time_current": 0,
                            "suggest_total": 0,
                            "suggest_time_in_millis": 0,
                            "suggest_current": 0
                        },
                        "merges": {
                            "current": 0,
                            "current_docs": 0,
                            "current_size_in_bytes": 0,
                            "total": 852,
                            "total_time_in_millis": 249527,
                            "total_docs": 20378391,
                            "total_size_in_bytes": 5110398375,
                            "total_stopped_time_in_millis": 0,
                            "total_throttled_time_in_millis": 39458,
                            "total_auto_throttle_in_bytes": 784092200,
                            "unreferenced_file_cleanups_performed": 0
                        },
                        "refresh": {
                            "total": 52841,
                            "total_time_in_millis": 3333600,
                            "external_total": 52708,
                            "external_total_time_in_millis": 3353472,
                            "listeners": 0
                        },
                        "flush": {
                            "total": 105,
                            "periodic": 100,
                            "total_time_in_millis": 45000
                        },
                        "warmer": {
                            "current": 0,
                            "total": 7807,
                            "total_time_in_millis": 167
                        },
                        "query_cache": {
                            "memory_size_in_bytes": 0,
                            "total_count": 0,
                            "hit_count": 0,
                            "miss_count": 0,
                            "cache_size": 0,
                            "cache_count": 0,
                            "evictions": 0
                        },
                        "fielddata": {
                            "memory_size_in_bytes": 0,
                            "evictions": 0
                        },
                        "completion": {
                            "size_in_bytes": 0
                        },
                        "segments": {
                            "count": 99,
                            "memory_in_bytes": 0,
                            "terms_memory_in_bytes": 0,
                            "stored_fields_memory_in_bytes": 0,
                            "term_vectors_memory_in_bytes": 0,
                            "norms_memory_in_bytes": 0,
                            "points_memory_in_bytes": 0,
                            "doc_values_memory_in_bytes": 0,
                            "index_writer_memory_in_bytes": 2168432,
                            "version_map_memory_in_bytes": 145033,
                            "fixed_bit_set_memory_in_bytes": 0,
                            "max_unsafe_auto_id_timestamp": -1,
                            "remote_store": {
                                "upload": {
                                    "total_upload_size": {
                                        "started_bytes": 0,
                                        "succeeded_bytes": 0,
                                        "failed_bytes": 0
                                    },
                                    "refresh_size_lag": {
                                        "total_bytes": 0,
                                        "max_bytes": 0
                                    },
                                    "max_refresh_time_lag_in_millis": 0,
                                    "total_time_spent_in_millis": 0
                                },
                                "download": {
                                    "total_download_size": {
                                        "started_bytes": 0,
                                        "succeeded_bytes": 0,
                                        "failed_bytes": 0
                                    },
                                    "total_time_spent_in_millis": 0
                                }
                            },
                            "segment_replication": {
                                "max_bytes_behind": "0b",
                                "total_bytes_behind": "0b",
                                "max_replication_lag": "0s"
                            },
                            "file_sizes": {}
                        },
                        "translog": {
                            "operations": 1006848,
                            "size_in_bytes": 834131527,
                            "uncommitted_operations": 1006848,
                            "uncommitted_size_in_bytes": 834131527,
                            "earliest_last_modified_age": 24209,
                            "remote_store": {
                                "upload": {
                                    "total_uploads": {
                                        "started": 0,
                                        "failed": 0,
                                        "succeeded": 0
                                    },
                                    "total_upload_size": {
                                        "started_bytes": 0,
                                        "failed_bytes": 0,
                                        "succeeded_bytes": 0
                                    }
                                }
                            }
                        },
                        "request_cache": {
                            "memory_size_in_bytes": 0,
                            "evictions": 0,
                            "hit_count": 2,
                            "miss_count": 11
                        },
                        "recovery": {
                            "current_as_source": 0,
                            "current_as_target": 0,
                            "throttle_time_in_millis": 0
                        }
                    }
                },
                "indices": {
                    ".opensearch-observability": {
                        "uuid": "ATc0YY96SayiN3HoXLo09Q",
                        "primaries": {
                            "docs": {
                                "count": 0,
                                "deleted": 0
                            },
                            "store": {
                                "size_in_bytes": 208,
                                "reserved_in_bytes": 0
                            },
                            "indexing": {
                                "index_total": 0,
                                "index_time_in_millis": 0,
                                "index_current": 0,
                                "index_failed": 0,
                                "delete_total": 0,
                                "delete_time_in_millis": 0,
                                "delete_current": 0,
                                "noop_update_total": 0,
                                "is_throttled": false,
                                "throttle_time_in_millis": 0,
                                "doc_status": {}
                            },
                            "get": {
                                "total": 0,
                                "time_in_millis": 0,
                                "exists_total": 0,
                                "exists_time_in_millis": 0,
                                "missing_total": 0,
                                "missing_time_in_millis": 0,
                                "current": 0
                            },
                            "search": {
                                "open_contexts": 0,
                                "query_total": 0,
                                "query_time_in_millis": 0,
                                "query_current": 0,
                                "fetch_total": 0,
                                "fetch_time_in_millis": 0,
                                "fetch_current": 0,
                                "scroll_total": 0,
                                "scroll_time_in_millis": 0,
                                "scroll_current": 0,
                                "point_in_time_total": 0,
                                "point_in_time_time_in_millis": 0,
                                "point_in_time_current": 0,
                                "suggest_total": 0,
                                "suggest_time_in_millis": 0,
                                "suggest_current": 0
                            },
                            "merges": {
                                "current": 0,
                                "current_docs": 0,
                                "current_size_in_bytes": 0,
                                "total": 0,
                                "total_time_in_millis": 0,
                                "total_docs": 0,
                                "total_size_in_bytes": 0,
                                "total_stopped_time_in_millis": 0,
                                "total_throttled_time_in_millis": 0,
                                "total_auto_throttle_in_bytes": 20971520,
                                "unreferenced_file_cleanups_performed": 0
                            },
                            "refresh": {
                                "total": 5,
                                "total_time_in_millis": 0,
                                "external_total": 2,
                                "external_total_time_in_millis": 1,
                                "listeners": 0
                            },
                            "flush": {
                                "total": 1,
                                "periodic": 1,
                                "total_time_in_millis": 0
                            },
                            "warmer": {
                                "current": 0,
                                "total": 1,
                                "total_time_in_millis": 0
                            },
                            "query_cache": {
                                "memory_size_in_bytes": 0,
                                "total_count": 0,
                                "hit_count": 0,
                                "miss_count": 0,
                                "cache_size": 0,
                                "cache_count": 0,
                                "evictions": 0
                            },
                            "fielddata": {
                                "memory_size_in_bytes": 0,
                                "evictions": 0
                            },
                            "completion": {
                                "size_in_bytes": 0
                            },
                            "segments": {
                                "count": 0,
                                "memory_in_bytes": 0,
                                "terms_memory_in_bytes": 0,
                                "stored_fields_memory_in_bytes": 0,
                                "term_vectors_memory_in_bytes": 0,
                                "norms_memory_in_bytes": 0,
                                "points_memory_in_bytes": 0,
                                "doc_values_memory_in_bytes": 0,
                                "index_writer_memory_in_bytes": 0,
                                "version_map_memory_in_bytes": 0,
                                "fixed_bit_set_memory_in_bytes": 0,
                                "max_unsafe_auto_id_timestamp": -1,
                                "remote_store": {
                                    "upload": {
                                        "total_upload_size": {
                                            "started_bytes": 0,
                                            "succeeded_bytes": 0,
                                            "failed_bytes": 0
                                        },
                                        "refresh_size_lag": {
                                            "total_bytes": 0,
                                            "max_bytes": 0
                                        },
                                        "max_refresh_time_lag_in_millis": 0,
                                        "total_time_spent_in_millis": 0
                                    },
                                    "download": {
                                        "total_download_size": {
                                            "started_bytes": 0,
                                            "succeeded_bytes": 0,
                                            "failed_bytes": 0
                                        },
                                        "total_time_spent_in_millis": 0
                                    }
                                },
                                "segment_replication": {
                                    "max_bytes_behind": "0b",
                                    "total_bytes_behind": "0b",
                                    "max_replication_lag": "0s"
                                },
                                "file_sizes": {}
                            },
                            "translog": {
                                "operations": 0,
                                "size_in_bytes": 55,
                                "uncommitted_operations": 0,
                                "uncommitted_size_in_bytes": 55,
                                "earliest_last_modified_age": 18698230,
                                "remote_store": {
                                    "upload": {
                                        "total_uploads": {
                                            "started": 0,
                                            "failed": 0,
                                            "succeeded": 0
                                        },
                                        "total_upload_size": {
                                            "started_bytes": 0,
                                            "failed_bytes": 0,
                                            "succeeded_bytes": 0
                                        }
                                    }
                                }
                            },
                            "request_cache": {
                                "memory_size_in_bytes": 0,
                                "evictions": 0,
                                "hit_count": 0,
                                "miss_count": 0
                            },
                            "recovery": {
                                "current_as_source": 0,
                                "current_as_target": 0,
                                "throttle_time_in_millis": 0
                            }
                        },
                        "total": {
                            "docs": {
                                "count": 0,
                                "deleted": 0
                            },
                            "store": {
                                "size_in_bytes": 416,
                                "reserved_in_bytes": 0
                            },
                            "indexing": {
                                "index_total": 0,
                                "index_time_in_millis": 0,
                                "index_current": 0,
                                "index_failed": 0,
                                "delete_total": 0,
                                "delete_time_in_millis": 0,
                                "delete_current": 0,
                                "noop_update_total": 0,
                                "is_throttled": false,
                                "throttle_time_in_millis": 0,
                                "doc_status": {}
                            },
                            "get": {
                                "total": 0,
                                "time_in_millis": 0,
                                "exists_total": 0,
                                "exists_time_in_millis": 0,
                                "missing_total": 0,
                                "missing_time_in_millis": 0,
                                "current": 0
                            },
                            "search": {
                                "open_contexts": 0,
                                "query_total": 0,
                                "query_time_in_millis": 0,
                                "query_current": 0,
                                "fetch_total": 0,
                                "fetch_time_in_millis": 0,
                                "fetch_current": 0,
                                "scroll_total": 0,
                                "scroll_time_in_millis": 0,
                                "scroll_current": 0,
                                "point_in_time_total": 0,
                                "point_in_time_time_in_millis": 0,
                                "point_in_time_current": 0,
                                "suggest_total": 0,
                                "suggest_time_in_millis": 0,
                                "suggest_current": 0
                            },
                            "merges": {
                                "current": 0,
                                "current_docs": 0,
                                "current_size_in_bytes": 0,
                                "total": 0,
                                "total_time_in_millis": 0,
                                "total_docs": 0,
                                "total_size_in_bytes": 0,
                                "total_stopped_time_in_millis": 0,
                                "total_throttled_time_in_millis": 0,
                                "total_auto_throttle_in_bytes": 41943040,
                                "unreferenced_file_cleanups_performed": 0
                            },
                            "refresh": {
                                "total": 8,
                                "total_time_in_millis": 0,
                                "external_total": 4,
                                "external_total_time_in_millis": 1,
                                "listeners": 0
                            },
                            "flush": {
                                "total": 2,
                                "periodic": 2,
                                "total_time_in_millis": 0
                            },
                            "warmer": {
                                "current": 0,
                                "total": 2,
                                "total_time_in_millis": 0
                            },
                            "query_cache": {
                                "memory_size_in_bytes": 0,
                                "total_count": 0,
                                "hit_count": 0,
                                "miss_count": 0,
                                "cache_size": 0,
                                "cache_count": 0,
                                "evictions": 0
                            },
                            "fielddata": {
                                "memory_size_in_bytes": 0,
                                "evictions": 0
                            },
                            "completion": {
                                "size_in_bytes": 0
                            },
                            "segments": {
                                "count": 0,
                                "memory_in_bytes": 0,
                                "terms_memory_in_bytes": 0,
                                "stored_fields_memory_in_bytes": 0,
                                "term_vectors_memory_in_bytes": 0,
                                "norms_memory_in_bytes": 0,
                                "points_memory_in_bytes": 0,
                                "doc_values_memory_in_bytes": 0,
                                "index_writer_memory_in_bytes": 0,
                                "version_map_memory_in_bytes": 0,
                                "fixed_bit_set_memory_in_bytes": 0,
                                "max_unsafe_auto_id_timestamp": -1,
                                "remote_store": {
                                    "upload": {
                                        "total_upload_size": {
                                            "started_bytes": 0,
                                            "succeeded_bytes": 0,
                                            "failed_bytes": 0
                                        },
                                        "refresh_size_lag": {
                                            "total_bytes": 0,
                                            "max_bytes": 0
                                        },
                                        "max_refresh_time_lag_in_millis": 0,
                                        "total_time_spent_in_millis": 0
                                    },
                                    "download": {
                                        "total_download_size": {
                                            "started_bytes": 0,
                                            "succeeded_bytes": 0,
                                            "failed_bytes": 0
                                        },
                                        "total_time_spent_in_millis": 0
                                    }
                                },
                                "segment_replication": {
                                    "max_bytes_behind": "0b",
                                    "total_bytes_behind": "0b",
                                    "max_replication_lag": "0s"
                                },
                                "file_sizes": {}
                            },
                            "translog": {
                                "operations": 0,
                                "size_in_bytes": 110,
                                "uncommitted_operations": 0,
                                "uncommitted_size_in_bytes": 110,
                                "earliest_last_modified_age": 18673677,
                                "remote_store": {
                                    "upload": {
                                        "total_uploads": {
                                            "started": 0,
                                            "failed": 0,
                                            "succeeded": 0
                                        },
                                        "total_upload_size": {
                                            "started_bytes": 0,
                                            "failed_bytes": 0,
                                            "succeeded_bytes": 0
                                        }
                                    }
                                }
                            },
                            "request_cache": {
                                "memory_size_in_bytes": 0,
                                "evictions": 0,
                                "hit_count": 0,
                                "miss_count": 0
                            },
                            "recovery": {
                                "current_as_source": 0,
                                "current_as_target": 0,
                                "throttle_time_in_millis": 0
                            }
                        }
                    }
                }
            }
            """;

}
