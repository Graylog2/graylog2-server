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
import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import ApiRoutes from 'routing/ApiRoutes';
import { qualifyUrl } from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { PipelineConnectionsActions } = CombinedProvider.get('PipelineConnections');

export type PipelineConnectionsType = {
  id?: string,
  stream_id: string,
  pipeline_ids: string[],
};

type PipelineReverseConnectionsType = {
  pipeline_id: string,
  stream_ids: string[],
};

const PipelineConnectionsStore = Reflux.createStore({
  listenables: [PipelineConnectionsActions],
  connections: undefined,

  getInitialState() {
    return { connections: this.connections };
  },

  list() {
    const failCallback = (error) => {
      UserNotification.error(`Fetching pipeline connections failed with status: ${error.message}`,
        'Could not retrieve pipeline connections');
    };

    const url = qualifyUrl(ApiRoutes.ConnectionsController.list().url);
    const promise = fetch('GET', url);

    promise.then((response) => {
      this.connections = response;
      this.trigger({ connections: response });
    }, failCallback);
  },

  connectToStream(connection) {
    const url = qualifyUrl(ApiRoutes.ConnectionsController.to_stream().url);
    const updatedConnection: PipelineConnectionsType = {
      stream_id: connection.stream,
      pipeline_ids: connection.pipelines,
    };
    const promise = fetch('POST', url, updatedConnection);

    promise.then(
      (response) => {
        if (this.connections.filter((c) => c.stream_id === response.stream_id)[0]) {
          this.connections = this.connections.map((c) => (c.stream_id === response.stream_id ? response : c));
        } else {
          this.connections.push(response);
        }

        this.trigger({ connections: this.connections });
        UserNotification.success('Pipeline connections updated successfully');
      },
      this._failUpdateCallback,
    );
  },

  connectToPipeline(reverseConnection) {
    const url = qualifyUrl(ApiRoutes.ConnectionsController.to_pipeline().url);
    const updatedConnection: PipelineReverseConnectionsType = {
      pipeline_id: reverseConnection.pipeline,
      stream_ids: reverseConnection.streams,
    };
    const promise = fetch('POST', url, updatedConnection);

    promise.then(
      (response) => {
        response.forEach((connection) => {
          if (this.connections.filter((c) => c.stream_id === connection.stream_id)[0]) {
            this.connections = this.connections.map((c) => (c.stream_id === connection.stream_id ? connection : c));
          } else {
            this.connections.push(connection);
          }
        });

        this.trigger({ connections: this.connections });
        UserNotification.success('Pipeline connections updated successfully');
      },
      this._failUpdateCallback,
    );
  },

  _failUpdateCallback(error) {
    UserNotification.error(`Updating pipeline connections failed with status: ${error.message}`,
      'Could not update pipeline connections');
  },
});

export default PipelineConnectionsStore;
