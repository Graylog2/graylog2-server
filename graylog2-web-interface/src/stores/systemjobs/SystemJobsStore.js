import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import SystemJobsActions from 'actions/systemjobs/SystemJobsActions';

const SystemJobsStore = Reflux.createStore({
  listenables: [SystemJobsActions],

  jobsById: {},

  getInitialState() {
    return {jobs: this.jobs, jobsById: this.jobsById};
  },
  list() {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.SystemJobsApiController.list().url);
    const promise = fetch('GET', url).then((response) => {
      this.jobs = response;
      this.trigger({jobs: response});

      return response;
    });
    SystemJobsActions.list.promise(promise);
  },
  getJob(jobId) {
    const url = URLUtils.qualifyUrl(jsRoutes.controllers.api.SystemJobsApiController.getJob(jobId).url);
    const promise = fetch('GET', url).then((response) => {
      this.jobsById[response.id] = response;
      this.trigger({jobsById: this.jobsById});

      return response;
    }, () => {
      // If we get an error (probably 404 because the job is gone), remove the job from the cache and trigger an update.
      delete(this.jobsById[jobId]);
      this.trigger({jobsById: this.jobsById});
    });
    SystemJobsActions.getJob.promise(promise);
  },
});

export default SystemJobsStore;
