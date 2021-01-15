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
import { qualifyUrl } from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';
import CombinedProvider from 'injection/CombinedProvider';

const { PipelinesActions } = CombinedProvider.get('Pipelines');

export type PipelineType = {
  id: string,
  title: string,
  description: string,
  source: string,
  created_at: string,
  modified_at: string,
  stages: StageType[],
  errors?: [],
};

export type StageType = {
  stage: number,
  match_all: boolean,
  rules: [string],
};

const PipelinesStore = Reflux.createStore({
  listenables: [PipelinesActions],
  pipelines: undefined,

  getInitialState() {
    return { pipelines: this.pipelines };
  },

  _updatePipelinesState(pipeline: PipelineType) {
    if (!this.pipelines) {
      this.pipelines = [pipeline];
    } else {
      const doesPipelineExist = this.pipelines.some((p) => p.id === pipeline.id);

      if (doesPipelineExist) {
        this.pipelines = this.pipelines.map((p) => (p.id === pipeline.id ? pipeline : p));
      } else {
        this.pipelines.push(pipeline);
      }
    }

    this.trigger({ pipelines: this.pipelines });
  },

  list() {
    const failCallback = (error) => {
      UserNotification.error(`Fetching pipelines failed with status: ${error.message}`,
        'Could not retrieve processing pipelines');
    };

    const url = qualifyUrl(ApiRoutes.PipelinesController.list().url);

    return fetch('GET', url).then((response) => {
      this.pipelines = response;
      this.trigger({ pipelines: response });
    }, failCallback);
  },

  get(pipelineId) {
    const failCallback = (error) => {
      UserNotification.error(`Fetching pipeline failed with status: ${error.message}`,
        `Could not retrieve processing pipeline "${pipelineId}"`);
    };

    const url = qualifyUrl(ApiRoutes.PipelinesController.get(pipelineId).url);
    const promise = fetch('GET', url);

    promise.then(this._updatePipelinesState, failCallback);
  },

  save(pipelineSource) {
    const failCallback = (error) => {
      UserNotification.error(`Saving pipeline failed with status: ${error.message}`,
        'Could not save processing pipeline');
    };

    const url = qualifyUrl(ApiRoutes.PipelinesController.create().url);
    const pipeline = {
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source,
    };
    const promise = fetch('POST', url, pipeline);

    promise.then(
      (response) => {
        this._updatePipelinesState(response);
        UserNotification.success(`Pipeline "${pipeline.title}" created successfully`);
      },
      failCallback,
    );

    PipelinesActions.save.promise(promise);
  },

  update(pipelineSource) {
    const failCallback = (error) => {
      UserNotification.error(`Updating pipeline failed with status: ${error.message}`,
        'Could not update processing pipeline');
    };

    const url = qualifyUrl(ApiRoutes.PipelinesController.update(pipelineSource.id).url);
    const pipeline = {
      id: pipelineSource.id,
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source,
    };
    const promise = fetch('PUT', url, pipeline);

    promise.then(
      (response) => {
        this._updatePipelinesState(response);
        UserNotification.success(`Pipeline "${pipeline.title}" updated successfully`);
      },
      failCallback,
    );

    PipelinesActions.update.promise(promise);
  },
  delete(pipelineId) {
    const failCallback = (error) => {
      UserNotification.error(`Deleting pipeline failed with status: ${error.message}`,
        `Could not delete processing pipeline "${pipelineId}"`);
    };

    const url = qualifyUrl(ApiRoutes.PipelinesController.delete(pipelineId).url);

    return fetch('DELETE', url).then(() => {
      const updatedPipelines = this.pipelines || [];

      this.pipelines = updatedPipelines.filter((el) => el.id !== pipelineId);
      this.trigger({ pipelines: this.pipelines });
      UserNotification.success(`Pipeline "${pipelineId}" deleted successfully`);
    }, failCallback);
  },
  parse(pipelineSource, callback) {
    const url = qualifyUrl(ApiRoutes.PipelinesController.parse().url);
    const pipeline = {
      title: pipelineSource.title,
      description: pipelineSource.description,
      source: pipelineSource.source,
    };

    return fetch('POST', url, pipeline).then(
      () => {
        // call to clear the errors, the parsing was successful
        callback([]);
      },
      (error) => {
        // a Bad Request indicates a parse error, set all the returned errors in the editor
        const response = error.additional.res;

        if (response.status === 400) {
          callback(response.body);
        }
      },
    );
  },
});

export default PipelinesStore;
