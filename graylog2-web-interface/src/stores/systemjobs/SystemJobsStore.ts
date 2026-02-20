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

import type { Store } from 'stores/StoreTypes';
import * as URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';
import { singletonStore, singletonActions } from 'logic/singleton';

type SystemJob = {
  id: string;
  description: string;
  name: string;
  info: string;
  node_id: string;
  started_at: string;
  percent_complete: number;
  provides_progress: boolean;
  is_cancelable: boolean;
};

type SystemJobsListResponse = Record<string, { jobs: Array<SystemJob> }>;

type SystemJobsActionsType = {
  list: () => Promise<SystemJobsListResponse>;
  getJob: (jobId: string) => Promise<unknown>;
  acknowledgeJob: (jobId: string) => Promise<unknown>;
  cancelJob: (jobId: string) => Promise<unknown>;
};

export const SystemJobsActions = singletonActions('core.SystemJobs', () =>
  Reflux.createActions<SystemJobsActionsType>({
    list: { asyncResult: true },
    getJob: { asyncResult: true },
    acknowledgeJob: { asyncResult: true },
    cancelJob: { asyncResult: true },
  }),
);

export const SystemJobsStore: Store<{ jobs: unknown; jobsById: Record<string, unknown> }> = singletonStore(
  'core.SystemJobs',
  () =>
    Reflux.createStore({
      listenables: [SystemJobsActions],

      jobsById: {} as Record<string, unknown>,

      getInitialState() {
        return { jobs: this.jobs, jobsById: this.jobsById };
      },
      list() {
        const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.list().url);
        const promise = fetchPeriodically<SystemJobsListResponse>('GET', url).then((response) => {
          this.jobs = response;
          this.trigger({ jobs: response });

          return response;
        });

        SystemJobsActions.list.promise(promise);
      },
      getJob(jobId: string) {
        const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.getJob(jobId).url);
        const promise = fetch<{ id: string }>('GET', url).then(
          (response) => {
            this.jobsById = { ...this.jobsById, [response.id]: response };
            this.trigger({ jobsById: this.jobsById });

            return response;
          },
          () => {
            // If we get an error (probably 404 because the job is gone), remove the job from the cache and trigger an update.

            const { [jobId]: _, ...rest } = this.jobsById;

            this.jobsById = rest;
            this.trigger({ jobsById: this.jobsById });
          },
        );

        SystemJobsActions.getJob.promise(promise);
      },
      acknowledgeJob(jobId: string) {
        const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.acknowledgeJob(jobId).url);
        const promise = fetch('DELETE', url).then(() => {
          delete this.jobsById[jobId];
        });

        SystemJobsActions.acknowledgeJob.promise(promise);
      },
      cancelJob(jobId: string) {
        const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.cancelJob(jobId).url);
        const promise = fetch<{ id: string }>('DELETE', url).then((response) => {
          delete this.jobsById[response.id];
        });

        SystemJobsActions.cancelJob.promise(promise);
      },
    }),
);
