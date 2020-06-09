import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch, { fetchPeriodically } from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';

const SystemJobsActions = ActionsProvider.getActions('SystemJobs');

const SystemJobsStore = Reflux.createStore({
  listenables: [SystemJobsActions],

  jobsById: {},

  getInitialState() {
    return { jobs: this.jobs, jobsById: this.jobsById };
  },
  list() {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.list().url);
    const promise = fetchPeriodically('GET', url).then((response) => {
      this.jobs = response;
      this.trigger({ jobs: response });

      return response;
    });
    SystemJobsActions.list.promise(promise);
  },
  getJob(jobId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.getJob(jobId).url);
    const promise = fetch('GET', url).then((response) => {
      this.jobsById = { ...this.jobsById, [response.id]: response };
      this.trigger({ jobsById: this.jobsById });

      return response;
    }, () => {
      // If we get an error (probably 404 because the job is gone), remove the job from the cache and trigger an update.
      const { [jobId]: currentJob, ...rest } = this.jobsById;
      this.jobsById = rest;
      this.trigger({ jobsById: this.jobsById });
    });
    SystemJobsActions.getJob.promise(promise);
  },
  cancelJob(jobId) {
    const url = URLUtils.qualifyUrl(ApiRoutes.SystemJobsApiController.cancelJob(jobId).url);
    const promise = fetch('DELETE', url).then((response) => {
      delete (this.jobsById[response.id]);
    });

    SystemJobsActions.cancelJob.promise(promise);
  },
});

export default SystemJobsStore;
