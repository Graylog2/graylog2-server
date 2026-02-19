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

type SystemJobsActionsType = {
  list: () => Promise<unknown>;
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

export const SystemJobsStore: Store<{ jobs: unknown; jobsById: Record<string, unknown> }> = singletonStore('core.SystemJobs', () =>
  Reflux.createStore({
    listenables: [SystemJobsActions],

    jobsById: {} as Record<string, unknown>,

    getInitialState() {
      return { jobs: this.jobs, jobsById: this.jobsById };
    },
    list() {
      const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.list().url);
      const promise = fetchPeriodically('GET', url).then((response: unknown) => {
        this.jobs = response;
        this.trigger({ jobs: response });

        return response;
      });

      SystemJobsActions.list.promise(promise);
    },
    getJob(jobId: string) {
      const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.getJob(jobId).url);
      const promise = fetch('GET', url).then(
        (response: unknown) => {
          const resp = response as Record<string, unknown>;
          this.jobsById = { ...this.jobsById, [resp.id as string]: response };
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
      const promise = fetch('DELETE', url).then((response: unknown) => {
        const resp = response as Record<string, unknown>;
        delete this.jobsById[resp.id as string];
      });

      SystemJobsActions.cancelJob.promise(promise);
    },
  }),
);
