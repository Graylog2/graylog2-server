import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const SystemLoadBalancerStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/lbstatus`,

  override(nodeId, status) {
    return fetch('PUT', URLUtils.qualifyUrl(`${this.sourceUrl(nodeId)}/override/${status}`))
      .then(
        () => {
          this.trigger({});
          UserNotification.success(`Load balancer status successfully changed do '${status}' in node '${nodeId}'`);
        },
        (error) => {
          UserNotification.error(`Changing load balancer status in '${nodeId}' failed: ${error}`,
            `Could not change load balancer status to '${status}' in node '${nodeId}'`);
        },
      );
  },
});

export default SystemLoadBalancerStore;
