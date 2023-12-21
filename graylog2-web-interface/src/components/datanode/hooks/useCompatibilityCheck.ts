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
import { useQuery } from '@tanstack/react-query';

import type { CompatibilityResponseType } from 'components/datanode/Types';

type Options = {
  enabled: boolean,
}

const fetchCompatibility = async () =>
// TODO use endpoint
// const url = '/system/cluster/datanodes/compatibility';

  //  return fetch('GET', qualifyUrl(url));
  Promise.resolve({
    opensearch_version: '2.10.0',
    info: {
      nodes: [{
        indices: [{
          index_id: 'prlnhUp_TvSof9U-K3FZ9A',
          shards: [{ documents_count: 10, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: '.opendistro_security',
          creation_date: '2023-11-17T09:57:36.511',
          index_version_created: '2.10.0',
        }, {
          index_id: '_0rOpzX_Qg2bCQ6BIF6ZPQ',
          shards: [{ documents_count: 0, name: 'S0', primary: true }],
          index_name: '.opensearch-observability',
          creation_date: '2023-11-17T09:57:35.744',
          index_version_created: '2.10.0',
        }, {
          index_id: 'c0VSR7fDQqytcDpCxDWRxQ',
          shards: [{ documents_count: 440, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: '.opensearch-sap-log-types-config',
          creation_date: '2023-11-17T09:57:35.544',
          index_version_created: '2.10.0',
        }, {
          index_id: '-OVSZTU0RQmZJEYys6A2EQ',
          shards: [{ documents_count: 1, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: '.plugins-ml-config',
          creation_date: '2023-11-17T09:57:45.491',
          index_version_created: '2.10.0',
        }, {
          index_id: 'xaVxlRaRSBaenAGzOrGbWw',
          shards: [{ documents_count: 0, name: 'S0', primary: true }],
          index_name: 'gl-events_0',
          creation_date: '2023-11-22T15:04:39.029',
          index_version_created: '2.10.0',
        }, {
          index_id: 'kFa5LkcOSlG1RwoZ2SIwYw',
          shards: [{ documents_count: 0, name: 'S0', primary: true }],
          index_name: 'gl-failures_0',
          creation_date: '2023-11-22T15:04:40.217',
          index_version_created: '2.10.0',
        }, {
          index_id: 'trNqtGUyQv-vxMHVrpkpPQ',
          shards: [{ documents_count: 1, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: 'gl-system-events_0',
          creation_date: '2023-11-22T15:04:40.591',
          index_version_created: '2.10.0',
        }, {
          index_id: 'dfP1lNFRTbSNw8p5SMS9rA',
          shards: [{ documents_count: 96457, name: 'S0', primary: true, min_lucene_version: '9.7.0' }],
          index_name: 'graylog_0',
          creation_date: '2023-11-22T15:04:38.393',
          index_version_created: '2.10.0',
        }, {
          index_id: 'UgiPogQpSEi_8N90mYDSLA',
          shards: [{
            documents_count: 66816,
            name: 'S0',
            primary: true,
            min_lucene_version: '9.7.0',
          }, { documents_count: 66599, name: 'S1', primary: true, min_lucene_version: '9.7.0' }, {
            documents_count: 66956,
            name: 'S2',
            primary: true,
            min_lucene_version: '9.7.0',
          }],
          index_name: 'graylog_1',
          creation_date: '2023-11-23T09:54:46.519',
          index_version_created: '2.10.0',
        }, {
          index_id: 'TmBrAXVSTJaOVqz5JOUIyg',
          shards: [{
            documents_count: 66784,
            name: 'S0',
            primary: true,
            min_lucene_version: '9.7.0',
          }, { documents_count: 66866, name: 'S1', primary: true, min_lucene_version: '9.7.0' }, {
            documents_count: 66522,
            name: 'S2',
            primary: true,
            min_lucene_version: '9.7.0',
          }],
          index_name: 'graylog_2',
          creation_date: '2023-11-23T11:19:33.398',
          index_version_created: '2.10.0',
        }, {
          index_id: 'V_XzvJ4_TV-zrSILDwPxSA',
          shards: [{
            documents_count: 66809,
            name: 'S0',
            primary: true,
            min_lucene_version: '9.7.0',
          }, { documents_count: 66511, name: 'S1', primary: true, min_lucene_version: '9.7.0' }, {
            documents_count: 66951,
            name: 'S2',
            primary: true,
            min_lucene_version: '9.7.0',
          }],
          index_name: 'graylog_3',
          creation_date: '2023-11-23T12:44:13.380',
          index_version_created: '2.10.0',
        }, {
          index_id: '-F3fGGxXQ_WEftKZrXy23Q',
          shards: [{
            documents_count: 17735,
            name: 'S0',
            primary: true,
            min_lucene_version: '9.7.0',
          }, { documents_count: 17370, name: 'S1', primary: true, min_lucene_version: '9.7.0' }, {
            documents_count: 17344,
            name: 'S2',
            primary: true,
            min_lucene_version: '9.7.0',
          }],
          index_name: 'graylog_4',
          creation_date: '2023-11-28T11:23:15.118',
          index_version_created: '2.10.0',
        }, {
          index_id: '12exg4yXQGmM_Zp7XAx47g',
          shards: [{ documents_count: 0, name: 'S0', primary: true }],
          index_name: 'investigation_event_index_0',
          creation_date: '2023-11-22T15:04:39.464',
          index_version_created: '2.10.0',
        }, {
          index_id: 'kep6CyCFSXWedhorolIkBA',
          shards: [{ documents_count: 0, name: 'S0', primary: true }],
          index_name: 'investigation_message_index_0',
          creation_date: '2023-11-22T15:04:39.848',
          index_version_created: '2.10.0',
        }],
        node_version: '2.10.0',
      }],
      opensearch_data_location: '/home/tdvorak/bin/datanode/data',
    },
    compatibility_errors: [],
  });

const useCompatibilityCheck = ({ enabled }: Options = { enabled: true }): {
  data: CompatibilityResponseType,
  error: Error,
  refetch: () => void,
  isInitialLoading: boolean,
  isError: boolean,
} => {
  const { data, refetch, isInitialLoading, error, isError } = useQuery<CompatibilityResponseType, Error>(
    ['datanodes', 'compatibility'],
    () => fetchCompatibility(),
    {
      keepPreviousData: true,
      enabled,
    });

  return ({
    data,
    error,
    refetch,
    isInitialLoading,
    isError,
  });
};

export default useCompatibilityCheck;
