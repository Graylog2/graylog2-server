import Reflux from 'reflux';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import fetch from 'logic/rest/FetchProvider';

const SystemProcessingStore = Reflux.createStore({
  sourceUrl: (nodeId) => `/cluster/${nodeId}/processing`,

  pause(nodeId) {
    return fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl(nodeId)}/pause`))
      .then(
        () => {
          this.trigger({});
          UserNotification.success(`Message processing paused successfully in '${nodeId}'`);
        },
        (error) => {
          UserNotification.error(`Pausing message processing in '${nodeId}' failed: ${error}`,
            `Could not pause message processing in node '${nodeId}'`);
        },
      );
  },

  resume(nodeId) {
    return fetch('POST', URLUtils.qualifyUrl(`${this.sourceUrl(nodeId)}/resume`))
      .then(
        () => {
          this.trigger({});
          UserNotification.success(`Message processing resumed successfully in '${nodeId}'`);
        },
        (error) => {
          UserNotification.error(`Resuming message processing in '${nodeId}' failed: ${error}`,
            `Could not resume message processing in node '${nodeId}'`);
        },
      );
  },
});

export default SystemProcessingStore;
