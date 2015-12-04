import UserNotification = require('util/UserNotification');
import URLUtils = require('util/URLUtils');
const fetch = require('logic/rest/FetchProvider').default;

interface NodeDetails {
  operating_system: string;
}
interface Collector {
  id: string;
  node_id: string;
  node_details: NodeDetails;
  last_seen: number;
  collector_version: String;
}

const CollectorsStore = {
  URL: URLUtils.qualifyUrl('/system/collectors'),

  load(callback: (collectors: Array<Collector>) => void) {
    var failCallback = (error) => {
      UserNotification.error("Loading collectors failed with status: " + error.message,
        "Could not load collectors");
    };
    fetch('GET', this.URL).then(callback, failCallback);
  }
};

export default CollectorsStore;
