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
import { Streams } from '@graylog/server-api';

import i18n from 'i18n';
import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import PaginationURL from 'util/PaginationURL';
import { CurrentUserStore } from 'stores/users/CurrentUserStore';
import type { Attributes } from 'stores/PaginationTypes';
import type { Stream, MatchData } from 'logic/streams/types';

type StreamSummaryResponse = {
  total: number;
  streams: Array<Stream>;
};

type PaginatedResponse = {
  pagination: {
    count: number;
    total: number;
    page: number;
    per_page: number;
  };
  query: string;
  attributes: Attributes;
  elements: Array<Stream>;
};

export type SearchStreamsPaginatedResponse = {
  list: Array<Stream>;
  attributes: Attributes;
  pagination: {
    count: number;
    total: number;
    page: number;
    perPage: number;
    query: string;
  };
};

type SearchStreamsAdditional = {
  sort: string;
  order: string;
  filters: Array<string> | undefined;
};

export type CloneStreamRequest = {
  title: string;
  description: string;
  index_set_id?: string;
  remove_matches_from_default_stream?: boolean;
};

type TestMatchMessage = {
  message: { [field: string]: unknown };
};

export const fetchStreams = (): Promise<Array<Stream>> =>
  fetch('GET', qualifyUrl('/streams'))
    .then((result: StreamSummaryResponse) => result.streams)
    .catch((errorThrown): never => {
      UserNotification.error(
        i18n.t('notifications.load_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('notifications.load_failed_title', { ns: 'streams' }),
      );

      throw errorThrown;
    });

export const searchStreamsPaginated = (
  page: number,
  perPage: number,
  query: string,
  additional: SearchStreamsAdditional,
): Promise<SearchStreamsPaginatedResponse> => {
  const url = PaginationURL(ApiRoutes.StreamsApiController.paginated().url, page, perPage, query, additional);

  return fetch('GET', qualifyUrl(url)).then((response: PaginatedResponse) => {
    const {
      elements,
      query: responseQuery,
      attributes,
      pagination: { count, total, page: responsePage, per_page: responsePerPage },
    } = response;

    return {
      list: elements,
      attributes,
      pagination: {
        count,
        total,
        page: responsePage,
        perPage: responsePerPage,
        query: responseQuery,
      },
    };
  });
};

export const updateStream = (streamId: string, data: Partial<Stream>): Promise<Stream> =>
  fetch('PUT', qualifyUrl(ApiRoutes.StreamsApiController.update(streamId).url), data).catch((errorThrown): never => {
    UserNotification.error(
      i18n.t('notifications.update_failed', { ns: 'streams', error: errorThrown }),
      i18n.t('notifications.update_failed_title', { ns: 'streams' }),
    );

    throw errorThrown;
  });

export const removeStream = (streamId: string): Promise<unknown> =>
  Streams.remove(streamId).then(() => CurrentUserStore.reload());

export const pauseStream = (streamId: string): Promise<unknown> =>
  Streams.pause(streamId).catch((errorThrown): never => {
    UserNotification.error(
      i18n.t('notifications.pause_failed', { ns: 'streams', error: errorThrown }),
      i18n.t('notifications.pause_failed_title', { ns: 'streams' }),
    );

    throw errorThrown;
  });

export const resumeStream = (streamId: string): Promise<unknown> =>
  Streams.resume(streamId).catch((errorThrown): never => {
    UserNotification.error(
      i18n.t('notifications.resume_failed', { ns: 'streams', error: errorThrown }),
      i18n.t('notifications.resume_failed_title', { ns: 'streams' }),
    );

    throw errorThrown;
  });

export const cloneStream = (streamId: string, data: CloneStreamRequest): Promise<unknown> =>
  fetch('POST', qualifyUrl(ApiRoutes.StreamsApiController.cloneStream(streamId).url), data)
    .catch((errorThrown): never => {
      UserNotification.error(
        i18n.t('notifications.clone_failed', { ns: 'streams', error: errorThrown }),
        i18n.t('notifications.clone_failed_title', { ns: 'streams' }),
      );

      throw errorThrown;
    })
    .then(() => CurrentUserStore.reload());

export const testMatch = (streamId: string, message: TestMatchMessage): Promise<MatchData> =>
  fetch('POST', qualifyUrl(ApiRoutes.StreamsApiController.testMatch(streamId).url), message).catch(
    (error: Error): never => {
      UserNotification.error(
        i18n.t('notifications.test_match_failed', { ns: 'streams', error: error.message }),
        i18n.t('notifications.test_match_failed_title', { ns: 'streams' }),
      );

      throw error;
    },
  );
