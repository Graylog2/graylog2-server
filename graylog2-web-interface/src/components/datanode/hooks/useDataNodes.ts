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

import { CertificateRenewal, Datanode } from '@graylog/server-api';

import { qualifyUrl } from 'util/URLUtils';
import PaginationURL from 'util/PaginationURL';
import i18n from 'i18n';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';
import type { Attribute, SearchParams, PaginatedResponseType } from 'stores/PaginationTypes';
import type { DataNodes } from 'components/datanode/Types';
import { defaultOnError } from 'util/conditional/onError';

export const bulkRemoveDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkRemove({
      entity_ids,
    });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        i18n.t('data_node_notifications.remove_failed', { ns: 'datanode', status: JSON.stringify(failures) }),
        i18n.t('data_node_notifications.bulk_remove_failed_title', { ns: 'datanode' }),
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        i18n.t('data_node_notifications.bulk_remove_success', { ns: 'datanode', count: successfully_performed }),
      );
    }
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.remove_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.bulk_remove_failed_title', { ns: 'datanode' }),
    );
  }
};

export const bulkStartDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkStart({
      entity_ids,
    });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        i18n.t('data_node_notifications.start_failed', { ns: 'datanode', status: JSON.stringify(failures) }),
        i18n.t('data_node_notifications.bulk_start_failed_title', { ns: 'datanode' }),
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        i18n.t('data_node_notifications.bulk_start_success', { ns: 'datanode', count: successfully_performed }),
      );
    }
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.start_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.bulk_start_failed_title', { ns: 'datanode' }),
    );
  }
};

export const bulkStopDataNode = async (
  entity_ids: string[],
  selectBackFailedEntities: (entity_ids: string[]) => void,
) => {
  try {
    const { failures, successfully_performed } = await Datanode.bulkStop({ entity_ids });

    selectBackFailedEntities([]);

    if (failures?.length) {
      selectBackFailedEntities(failures.map(({ entity_id }) => entity_id));
    }

    if (failures?.length === entity_ids.length) {
      UserNotification.error(
        i18n.t('data_node_notifications.stop_failed', { ns: 'datanode', status: JSON.stringify(failures) }),
        i18n.t('data_node_notifications.bulk_stop_failed_title', { ns: 'datanode' }),
      );
    }

    if (successfully_performed) {
      UserNotification.success(
        i18n.t('data_node_notifications.bulk_stop_success', { ns: 'datanode', count: successfully_performed }),
      );
    }
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.stop_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.bulk_stop_failed_title', { ns: 'datanode' }),
    );
  }
};

export const removeDataNode = async (datanodeId: string) => {
  try {
    await Datanode.removeNode(datanodeId);

    UserNotification.success(i18n.t('data_node_notifications.remove_success', { ns: 'datanode', datanodeId }));
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.remove_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.remove_failed_title', { ns: 'datanode' }),
    );
  }
};

export const startDataNode = async (datanodeId: string) => {
  try {
    await Datanode.startNode(datanodeId);

    UserNotification.success(i18n.t('data_node_notifications.start_success', { ns: 'datanode', datanodeId }));
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.start_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.start_failed_title', { ns: 'datanode' }),
    );
  }
};

export const stopDataNode = async (datanodeId: string) => {
  try {
    await Datanode.stopNode(datanodeId);

    UserNotification.success(i18n.t('data_node_notifications.stop_success', { ns: 'datanode', datanodeId }));
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.stop_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.stop_failed_title', { ns: 'datanode' }),
    );
  }
};

export const rejoinDataNode = async (datanodeId: string) => {
  try {
    await Datanode.resetNode(datanodeId);

    UserNotification.success(i18n.t('data_node_notifications.rejoin_success', { ns: 'datanode', datanodeId }));
  } catch (errorThrown) {
    UserNotification.error(
      i18n.t('data_node_notifications.rejoin_failed', { ns: 'datanode', status: errorThrown }),
      i18n.t('data_node_notifications.rejoin_failed_title', { ns: 'datanode' }),
    );
  }
};

export type UseDataNodesOptions = {
  enabled: boolean;
};

export const renewDatanodeCertificate = (nodeId: string) =>
  CertificateRenewal.initiateCertRenewalForNode(nodeId)
    .then(() => {
      UserNotification.success(i18n.t('data_node_notifications.renew_certificate_success', { ns: 'datanode' }));
    })
    .catch((error) => {
      UserNotification.error(i18n.t('data_node_notifications.renew_certificate_failed', { ns: 'datanode', error }));
    });

export const fetchDataNodes = async (params: SearchParams): Promise<DataNodeResponse> => {
  const url = PaginationURL('/system/cluster/datanodes', params.page, params.pageSize, params.query, {
    sort: params.sort?.attributeId,
    order: params.sort?.direction,
  });

  return fetch('GET', qualifyUrl(url)).then(({ attributes, pagination, elements }) => ({
    attributes,
    list: elements,
    pagination,
  }));
};

export const keyFn = (searchParams: SearchParams) => ['datanodes', searchParams];

export type DataNodeResponse = {
  list: DataNodes;
  pagination: PaginatedResponseType;
  attributes: Array<Attribute>;
};

const useDataNodes = (
  searchParams: SearchParams = {
    query: '-datanode_status:UNAVAILABLE',
    page: 1,
    pageSize: 0,
    sort: undefined,
  },
  { enabled }: UseDataNodesOptions = { enabled: true },
  refetchInterval: number | false = 5000,
) => {
  const { data, refetch, isLoading, error } = useQuery({
    queryKey: keyFn(searchParams),

    queryFn: () =>
      defaultOnError(
        fetchDataNodes(searchParams),
        i18n.t('data_node_notifications.loading_failed', { ns: 'datanode' }),
        i18n.t('data_node_notifications.loading_failed_title', { ns: 'datanode' }),
      ),
    notifyOnChangeProps: ['data', 'error'],
    refetchInterval,
    enabled,
  });

  return {
    data: data || {
      attributes: [],
      list: [],
      pagination: {
        query: '',
        page: 1,
        per_page: 0,
        total: 0,
        count: 0,
      },
    },
    refetch,
    isLoading,
    error,
  };
};

export default useDataNodes;
