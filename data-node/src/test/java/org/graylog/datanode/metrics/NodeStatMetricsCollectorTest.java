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
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class NodeStatMetricsCollectorTest {

    private final String NODENAME = "datanode1";

    NodeStatMetricsCollector collector;
    @Mock
    RestHighLevelClient client;

    @Before
    public void setUp() throws IOException {
        Response response = mock(Response.class);
        HttpEntity entity = mock(HttpEntity.class);
        when(response.getEntity()).thenReturn(entity);
        when(entity.getContent()).thenReturn(new ByteArrayInputStream(nodeStatResponse.getBytes(Charset.defaultCharset())));
        RestClient lowLevelClient = mock(RestClient.class);
        when(client.getLowLevelClient()).thenReturn(lowLevelClient);
        when(lowLevelClient.performRequest(any())).thenReturn(response);
        this.collector = new NodeStatMetricsCollector(client, new ObjectMapperProvider().get());
    }

    @Test
    public void getNodeMetrics() {
        Map<String, Object> nodeMetrics = collector.getNodeMetrics(NODENAME, "");
        assertThat(nodeMetrics.get("cpu_load")).isEqualTo(26.4873046875);
        assertThat(nodeMetrics.get("disk_free")).isEqualTo(572.1824f);
    }


    private final static String nodeStatResponse = """
            {
                "_nodes": {
                    "total": 1,
                    "successful": 1,
                    "failed": 0
                },
                "cluster_name": "datanode-cluster",
                "nodes": {
                    "qvCZ9yQ5S-OG8IKTnUdhpw": {
                        "timestamp": 1705681973264,
                        "name": "datanode1",
                        "transport_address": "127.0.0.1:9301",
                        "host": "datanode1",
                        "ip": "127.0.0.1:9301",
                        "roles": [
                            "cluster_manager",
                            "data",
                            "ingest",
                            "remote_cluster_client"
                        ],
                        "attributes": {
                            "shard_indexing_pressure_enabled": "true"
                        },
                        "indices": {
                            "docs": {
                                "count": 60369559,
                                "deleted": 88
                            },
                            "store": {
                                "size_in_bytes": 14300209839,
                                "reserved_in_bytes": 0
                            },
                            "indexing": {
                                "index_total": 244589,
                                "index_time_in_millis": 24757,
                                "index_current": 0,
                                "index_failed": 23,
                                "delete_total": 59,
                                "delete_time_in_millis": 279,
                                "delete_current": 0,
                                "noop_update_total": 0,
                                "is_throttled": false,
                                "throttle_time_in_millis": 0,
                                "doc_status": {
                                    "2xx": 241582,
                                    "4xx": 50
                                }
                            },
                            "get": {
                                "total": 109,
                                "time_in_millis": 173,
                                "exists_total": 91,
                                "exists_time_in_millis": 173,
                                "missing_total": 18,
                                "missing_time_in_millis": 0,
                                "current": 0
                            },
                            "search": {
                                "open_contexts": 0,
                                "query_total": 176,
                                "query_time_in_millis": 97,
                                "query_current": 0,
                                "fetch_total": 174,
                                "fetch_time_in_millis": 66,
                                "fetch_current": 0,
                                "scroll_total": 0,
                                "scroll_time_in_millis": 0,
                                "scroll_current": 0,
                                "point_in_time_total": 0,
                                "point_in_time_time_in_millis": 0,
                                "point_in_time_current": 0,
                                "suggest_total": 0,
                                "suggest_time_in_millis": 0,
                                "suggest_current": 0,
                                "request": {
                                    "dfs_pre_query": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    },
                                    "query": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    },
                                    "fetch": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    },
                                    "dfs_query": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    },
                                    "expand": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    },
                                    "can_match": {
                                        "time_in_millis": 0,
                                        "current": 0,
                                        "total": 0
                                    }
                                }
                            },
                            "merges": {
                                "current": 0,
                                "current_docs": 0,
                                "current_size_in_bytes": 0,
                                "total": 37,
                                "total_time_in_millis": 8328,
                                "total_docs": 666460,
                                "total_size_in_bytes": 168442652,
                                "total_stopped_time_in_millis": 0,
                                "total_throttled_time_in_millis": 0,
                                "total_auto_throttle_in_bytes": 838860800,
                                "unreferenced_file_cleanups_performed": 0
                            },
                            "refresh": {
                                "total": 1947,
                                "total_time_in_millis": 102298,
                                "external_total": 1897,
                                "external_total_time_in_millis": 103024,
                                "listeners": 0
                            },
                            "flush": {
                                "total": 37,
                                "periodic": 37,
                                "total_time_in_millis": 8771
                            },
                            "warmer": {
                                "current": 0,
                                "total": 380,
                                "total_time_in_millis": 51
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
                                "memory_size_in_bytes": 772,
                                "evictions": 0
                            },
                            "completion": {
                                "size_in_bytes": 0
                            },
                            "segments": {
                                "count": 175,
                                "memory_in_bytes": 0,
                                "terms_memory_in_bytes": 0,
                                "stored_fields_memory_in_bytes": 0,
                                "term_vectors_memory_in_bytes": 0,
                                "norms_memory_in_bytes": 0,
                                "points_memory_in_bytes": 0,
                                "doc_values_memory_in_bytes": 0,
                                "index_writer_memory_in_bytes": 1040188,
                                "version_map_memory_in_bytes": 36920,
                                "fixed_bit_set_memory_in_bytes": 240,
                                "max_unsafe_auto_id_timestamp": 1705596946358,
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
                                "operations": 244573,
                                "size_in_bytes": 202510227,
                                "uncommitted_operations": 244573,
                                "uncommitted_size_in_bytes": 202510227,
                                "earliest_last_modified_age": 7034,
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
                                "miss_count": 1
                            },
                            "recovery": {
                                "current_as_source": 0,
                                "current_as_target": 0,
                                "throttle_time_in_millis": 0
                            }
                        },
                        "os": {
                            "timestamp": 1705681973267,
                            "cpu": {
                                "percent": 37,
                                "load_average": {
                                    "1m": 26.4873046875
                                }
                            },
                            "mem": {
                                "total_in_bytes": 34359738368,
                                "free_in_bytes": 53592064,
                                "used_in_bytes": 34306146304,
                                "free_percent": 0,
                                "used_percent": 100
                            },
                            "swap": {
                                "total_in_bytes": 2147483648,
                                "free_in_bytes": 463994880,
                                "used_in_bytes": 1683488768
                            }
                        },
                        "process": {
                            "timestamp": 1705681973267,
                            "open_file_descriptors": 708,
                            "max_file_descriptors": 10240,
                            "cpu": {
                                "percent": 2,
                                "total_in_millis": 114466
                            },
                            "mem": {
                                "total_virtual_in_bytes": 430050213888
                            }
                        },
                        "jvm": {
                            "timestamp": 1705681973267,
                            "uptime_in_millis": 1273457,
                            "mem": {
                                "heap_used_in_bytes": 148181504,
                                "heap_used_percent": 13,
                                "heap_committed_in_bytes": 1073741824,
                                "heap_max_in_bytes": 1073741824,
                                "non_heap_used_in_bytes": 195711296,
                                "non_heap_committed_in_bytes": 200278016,
                                "pools": {
                                    "young": {
                                        "used_in_bytes": 32505856,
                                        "max_in_bytes": 0,
                                        "peak_used_in_bytes": 636485632,
                                        "peak_max_in_bytes": 0,
                                        "last_gc_stats": {
                                            "used_in_bytes": 0,
                                            "max_in_bytes": 0,
                                            "usage_percent": -1
                                        }
                                    },
                                    "old": {
                                        "used_in_bytes": 108335616,
                                        "max_in_bytes": 1073741824,
                                        "peak_used_in_bytes": 108335616,
                                        "peak_max_in_bytes": 1073741824,
                                        "last_gc_stats": {
                                            "used_in_bytes": 0,
                                            "max_in_bytes": 1073741824,
                                            "usage_percent": 0
                                        }
                                    },
                                    "survivor": {
                                        "used_in_bytes": 7340032,
                                        "max_in_bytes": 0,
                                        "peak_used_in_bytes": 55112576,
                                        "peak_max_in_bytes": 0,
                                        "last_gc_stats": {
                                            "used_in_bytes": 7340032,
                                            "max_in_bytes": 0,
                                            "usage_percent": -1
                                        }
                                    }
                                }
                            },
                            "threads": {
                                "count": 112,
                                "peak_count": 129
                            },
                            "gc": {
                                "collectors": {
                                    "young": {
                                        "collection_count": 33,
                                        "collection_time_in_millis": 456
                                    },
                                    "old": {
                                        "collection_count": 0,
                                        "collection_time_in_millis": 0
                                    }
                                }
                            },
                            "buffer_pools": {
                                "mapped": {
                                    "count": 403,
                                    "used_in_bytes": 7339021456,
                                    "total_capacity_in_bytes": 7339021456
                                },
                                "direct": {
                                    "count": 82,
                                    "used_in_bytes": 11889332,
                                    "total_capacity_in_bytes": 11889331
                                },
                                "mapped - 'non-volatile memory'": {
                                    "count": 0,
                                    "used_in_bytes": 0,
                                    "total_capacity_in_bytes": 0
                                }
                            },
                            "classes": {
                                "current_loaded_count": 23833,
                                "total_loaded_count": 23833,
                                "total_unloaded_count": 0
                            }
                        },
                        "thread_pool": {
                            "ad-batch-task-threadpool": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "ad-threadpool": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "analyze": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "fetch_shard_started": {
                                "threads": 1,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 20,
                                "completed": 63
                            },
                            "fetch_shard_store": {
                                "threads": 1,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 15,
                                "completed": 15
                            },
                            "flush": {
                                "threads": 1,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 39
                            },
                            "force_merge": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "generic": {
                                "threads": 20,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 20,
                                "completed": 6454
                            },
                            "get": {
                                "threads": 1,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 1,
                                "completed": 1
                            },
                            "listener": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "management": {
                                "threads": 5,
                                "queue": 0,
                                "active": 1,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 6726
                            },
                            "open_distro_job_scheduler": {
                                "threads": 10,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 10,
                                "completed": 10
                            },
                            "opensearch_asynchronous_search_generic": {
                                "threads": 2,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 2,
                                "completed": 6
                            },
                            "opensearch_ml_deploy": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "opensearch_ml_execute": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "opensearch_ml_general": {
                                "threads": 9,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 9,
                                "completed": 126
                            },
                            "opensearch_ml_predict": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "opensearch_ml_register": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "opensearch_ml_train": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "refresh": {
                                "threads": 5,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 36172
                            },
                            "remote_purge": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "remote_recovery": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "remote_refresh_retry": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "replication_follower": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "replication_leader": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "search": {
                                "threads": 3,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 3,
                                "completed": 3,
                                "total_wait_time_in_nanos": 752917
                            },
                            "search_throttled": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0,
                                "total_wait_time_in_nanos": 0
                            },
                            "snapshot": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "system_read": {
                                "threads": 5,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 372
                            },
                            "system_write": {
                                "threads": 5,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 165
                            },
                            "translog_sync": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "translog_transfer": {
                                "threads": 0,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 0,
                                "completed": 0
                            },
                            "warmer": {
                                "threads": 5,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 5,
                                "completed": 1024
                            },
                            "write": {
                                "threads": 10,
                                "queue": 0,
                                "active": 0,
                                "rejected": 0,
                                "largest": 10,
                                "completed": 2043
                            }
                        },
                        "fs": {
                            "timestamp": 1705681973268,
                            "total": {
                                "total_in_bytes": 994662584320,
                                "free_in_bytes": 614376128512,
                                "available_in_bytes": 614376128512,
                                "cache_reserved_in_bytes": 0
                            },
                            "data": [
                                {
                                    "path": "/Users/oesterheld/dev/projects/graylog-project-master/graylog-project-repos/graylog2-server/data-node/data/datanode1/nodes/0",
                                    "mount": "/System/Volumes/Data (/dev/disk3s5)",
                                    "type": "apfs",
                                    "total_in_bytes": 994662584320,
                                    "free_in_bytes": 614376128512,
                                    "available_in_bytes": 614376128512,
                                    "cache_reserved_in_bytes": 0
                                }
                            ]
                        },
                        "transport": {
                            "server_open": 13,
                            "total_outbound_connections": 1,
                            "rx_count": 12539,
                            "rx_size_in_bytes": 128502213,
                            "tx_count": 12539,
                            "tx_size_in_bytes": 132371515
                        },
                        "http": {
                            "current_open": 9,
                            "total_opened": 11
                        },
                        "breakers": {
                            "request": {
                                "limit_size_in_bytes": 644245094,
                                "limit_size": "614.3mb",
                                "estimated_size_in_bytes": 0,
                                "estimated_size": "0b",
                                "overhead": 1.0,
                                "tripped": 0
                            },
                            "fielddata": {
                                "limit_size_in_bytes": 429496729,
                                "limit_size": "409.5mb",
                                "estimated_size_in_bytes": 772,
                                "estimated_size": "772b",
                                "overhead": 1.03,
                                "tripped": 0
                            },
                            "in_flight_requests": {
                                "limit_size_in_bytes": 1073741824,
                                "limit_size": "1gb",
                                "estimated_size_in_bytes": 0,
                                "estimated_size": "0b",
                                "overhead": 2.0,
                                "tripped": 0
                            },
                            "parent": {
                                "limit_size_in_bytes": 1020054732,
                                "limit_size": "972.7mb",
                                "estimated_size_in_bytes": 148181504,
                                "estimated_size": "141.3mb",
                                "overhead": 1.0,
                                "tripped": 0
                            }
                        },
                        "script": {
                            "compilations": 1,
                            "cache_evictions": 0,
                            "compilation_limit_triggered": 0
                        },
                        "discovery": {
                            "cluster_state_queue": {
                                "total": 0,
                                "pending": 0,
                                "committed": 0
                            },
                            "published_cluster_states": {
                                "full_states": 2,
                                "incompatible_diffs": 0,
                                "compatible_diffs": 84
                            }
                        },
                        "ingest": {
                            "total": {
                                "count": 0,
                                "time_in_millis": 0,
                                "current": 0,
                                "failed": 0
                            },
                            "pipelines": {}
                        },
                        "adaptive_selection": {
                            "qvCZ9yQ5S-OG8IKTnUdhpw": {
                                "outgoing_searches": 0,
                                "avg_queue_size": 0,
                                "avg_service_time_ns": 1433182,
                                "avg_response_time_ns": 3924883,
                                "rank": "3.9"
                            }
                        },
                        "script_cache": {
                            "sum": {
                                "compilations": 1,
                                "cache_evictions": 0,
                                "compilation_limit_triggered": 0
                            },
                            "contexts": [
                                {
                                    "context": "aggregation_selector",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "aggs",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "aggs_combine",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "aggs_init",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "aggs_map",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "aggs_reduce",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "analysis",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "bucket_aggregation",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "field",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "filter",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "ingest",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "interval",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "moving-function",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "number_sort",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "painless_test",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "processor_conditional",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "score",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "script_heuristic",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "search",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "similarity",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "similarity_weight",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "string_sort",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "template",
                                    "compilations": 1,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "terms_set",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                },
                                {
                                    "context": "update",
                                    "compilations": 0,
                                    "cache_evictions": 0,
                                    "compilation_limit_triggered": 0
                                }
                            ]
                        },
                        "indexing_pressure": {
                            "memory": {
                                "current": {
                                    "combined_coordinating_and_primary_in_bytes": 0,
                                    "coordinating_in_bytes": 0,
                                    "primary_in_bytes": 0,
                                    "replica_in_bytes": 0,
                                    "all_in_bytes": 0
                                },
                                "total": {
                                    "combined_coordinating_and_primary_in_bytes": 360646972,
                                    "coordinating_in_bytes": 236354572,
                                    "primary_in_bytes": 245265065,
                                    "replica_in_bytes": 0,
                                    "all_in_bytes": 360646972,
                                    "coordinating_rejections": 0,
                                    "primary_rejections": 0,
                                    "replica_rejections": 0
                                },
                                "limit_in_bytes": 107374182
                            }
                        },
                        "shard_indexing_pressure": {
                            "stats": {},
                            "total_rejections_breakup_shadow_mode": {
                                "node_limits": 0,
                                "no_successful_request_limits": 0,
                                "throughput_degradation_limits": 0
                            },
                            "enabled": false,
                            "enforced": false
                        },
                        "search_backpressure": {
                            "search_task": {
                                "resource_tracker_stats": {
                                    "cpu_usage_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_millis": 0,
                                        "current_avg_millis": 0
                                    },
                                    "heap_usage_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_bytes": 0,
                                        "current_avg_bytes": 0,
                                        "rolling_avg_bytes": 811
                                    },
                                    "elapsed_time_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_millis": 0,
                                        "current_avg_millis": 0
                                    }
                                },
                                "cancellation_stats": {
                                    "cancellation_count": 0,
                                    "cancellation_limit_reached_count": 0
                                }
                            },
                            "search_shard_task": {
                                "resource_tracker_stats": {
                                    "cpu_usage_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_millis": 0,
                                        "current_avg_millis": 0
                                    },
                                    "heap_usage_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_bytes": 0,
                                        "current_avg_bytes": 0,
                                        "rolling_avg_bytes": 2259
                                    },
                                    "elapsed_time_tracker": {
                                        "cancellation_count": 0,
                                        "current_max_millis": 0,
                                        "current_avg_millis": 0
                                    }
                                },
                                "cancellation_stats": {
                                    "cancellation_count": 0,
                                    "cancellation_limit_reached_count": 0
                                }
                            },
                            "mode": "monitor_only"
                        },
                        "cluster_manager_throttling": {
                            "stats": {
                                "total_throttled_tasks": 0,
                                "throttled_tasks_per_task_type": {}
                            }
                        },
                        "weighted_routing": {
                            "stats": {
                                "fail_open_count": 0
                            }
                        },
                        "task_cancellation": {
                            "search_shard_task": {
                                "current_count_post_cancel": 0,
                                "total_count_post_cancel": 0
                            }
                        },
                        "search_pipeline": {
                            "total_request": {
                                "count": 0,
                                "time_in_millis": 0,
                                "current": 0,
                                "failed": 0
                            },
                            "total_response": {
                                "count": 0,
                                "time_in_millis": 0,
                                "current": 0,
                                "failed": 0
                            },
                            "pipelines": {}
                        }
                    }
                }
            }
                        """;


}
