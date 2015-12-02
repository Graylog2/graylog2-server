import Reflux from 'reflux';

import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import jsRoutes from 'routing/jsRoutes';
import fetch from 'logic/rest/FetchProvider';

import SystemJobsActions from 'actions/systemjobs/SystemJobsActions';

const SystemJobsStore = Reflux.createStore({
  listenables: [SystemJobsActions],
  getInitialState() {
    return {jobs: this.jobs};
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
});

export default SystemJobsStore;
