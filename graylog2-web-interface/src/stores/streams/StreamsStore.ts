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
// @flow strict
import Reflux from 'reflux';
import lodash from 'lodash';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import CombinedProvider from 'injection/CombinedProvider';
import PaginationURL from 'util/PaginationURL';

const { CurrentUserStore } = CombinedProvider.get('CurrentUser');

export type Stream = {
  id: string,
  title: string,
  description: string,
  remove_matches_from_default_stream: boolean,
  isDefaultStream: boolean,
  creatorUser: string,
  createdAt: number,
};

type TestMatchResponse = {
  matches: boolean,
  rules: any,
};

type Callback = {
  (): void,
};

type StreamSummaryResponse = {
  total: number,
  streams: Array<Stream>,
};

type PaginatedResponse = {
  pagination: {
    count: number,
    total: number,
    page: number,
    per_page: number,
    query: string,
  },
  streams: Array<any>,
};

const StreamsStore = Reflux.createStore({
  callbacks: [],

  searchPaginated(page, perPage, query) {
    const url = PaginationURL(ApiRoutes.StreamsApiController.paginated().url, page, perPage, query);

    return fetch('GET', qualifyUrl(url))
      .then((response: PaginatedResponse) => {
        const pagination = {
          count: response.pagination.count,
          total: response.pagination.total,
          page: response.pagination.page,
          perPage: response.pagination.per_page,
          query: response.pagination.query,
        };

        return {
          streams: response.streams,
          pagination,
        };
      })
      .catch((errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
      });
  },
  listStreams() {
    const url = '/streams';

    return fetch('GET', qualifyUrl(url))
      .then((result: StreamSummaryResponse) => result.streams)
      .catch((errorThrown) => {
        UserNotification.error(`Loading streams failed with status: ${errorThrown}`,
          'Could not load streams');
      });
  },
  load(callback: ((streams: Array<Stream>) => void)) {
    this.listStreams()
      .then((streams) => {
        callback(streams);
      });
  },
  get(streamId: string, callback: ((stream: Stream) => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Loading Stream failed with status: ${errorThrown}`,
        'Could not retrieve Stream');
    };

    const { url } = ApiRoutes.StreamsApiController.get(streamId);

    fetch('GET', qualifyUrl(url))
      .then(callback, failCallback);
  },
  remove(streamId: string, callback: (() => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Removing Stream failed with status: ${errorThrown}`,
        'Could not remove Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.delete(streamId).url);

    fetch('DELETE', url)
      .then(callback, failCallback)
      .then(() => CurrentUserStore.reload()
        .then(this._emitChange.bind(this)));
  },
  pause(streamId: string, callback: (() => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Pausing Stream failed with status: ${errorThrown}`,
        'Could not pause Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.pause(streamId).url);

    return fetch('POST', url)
      .then(callback, failCallback)
      .then((response) => {
        this._emitChange();

        return response;
      });
  },
  resume(streamId: string, callback: (() => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Resuming Stream failed with status: ${errorThrown}`,
        'Could not resume Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.resume(streamId).url);

    return fetch('POST', url)
      .then(callback, failCallback)
      .then((response) => {
        this._emitChange();

        return response;
      });
  },
  save(stream: any, callback: ((streamId: string) => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Saving Stream failed with status: ${errorThrown}`,
        'Could not save Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.create().url);

    fetch('POST', url, stream)
      .then(callback, failCallback)
      .then(() => CurrentUserStore.reload()
        .then(this._emitChange.bind(this)));
  },
  update(streamId: string, data: any, callback: ((stream: Stream) => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Updating Stream failed with status: ${errorThrown}`,
        'Could not update Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.update(streamId).url);

    fetch('PUT', url, data)
      .then(callback, failCallback)
      .then(this._emitChange.bind(this));
  },
  cloneStream(streamId: string, data: any, callback: ((streamId: string) => void)) {
    const failCallback = (errorThrown) => {
      UserNotification.error(`Cloning Stream failed with status: ${errorThrown}`,
        'Could not clone Stream');
    };

    const url = qualifyUrl(ApiRoutes.StreamsApiController.cloneStream(streamId).url);

    fetch('POST', url, data)
      .then(callback, failCallback)
      .then(() => CurrentUserStore.reload()
        .then(this._emitChange.bind(this)));
  },
  removeOutput(streamId: string, outputId: string, callback: () => void) {
    const url = qualifyUrl(ApiRoutes.StreamOutputsApiController.delete(streamId, outputId).url);

    fetch('DELETE', url)
      .then(callback, (errorThrown) => {
        UserNotification.error(`Removing output from stream failed with status: ${errorThrown}`,
          'Could not remove output from stream');
      })
      .then(this._emitChange.bind(this));
  },
  addOutput(streamId: string, outputId: string, callback: (response: any) => void) {
    const url = qualifyUrl(ApiRoutes.StreamOutputsApiController.add(streamId, outputId).url);

    fetch('POST', url, { outputs: [outputId] })
      .then(callback, (errorThrown) => {
        UserNotification.error(`Adding output to stream failed with status: ${errorThrown}`,
          'Could not add output to stream');
      })
      .then(this._emitChange.bind(this));
  },
  testMatch(streamId: string, message: any, callback: (response: TestMatchResponse) => void) {
    const url = qualifyUrl(ApiRoutes.StreamsApiController.testMatch(streamId).url);

    fetch('POST', url, message)
      .then(callback, (error) => {
        UserNotification.error(`Testing stream rules of stream failed with status: ${error.message}`,
          'Could not test stream rules of stream');
      });
  },
  onChange(callback: Callback) {
    this.callbacks.push(callback);
  },
  _emitChange() {
    this.callbacks.forEach((callback) => callback());
  },
  unregister(callback: Callback) {
    lodash.pull(this.callbacks, callback);
  },
});

export default StreamsStore;
