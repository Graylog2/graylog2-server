import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

const FieldsStore = Reflux.createStore({
  loadFields() {
    const { url } = ApiRoutes.SystemApiController.fields();
    let promise = fetch('GET', URLUtils.qualifyUrl(url));
    promise = promise.then(data => data.fields);
    promise.catch((errorThrown) => {
      UserNotification.error(`Loading field information failed with status: ${errorThrown.additional.message}`,
        'Could not load field information');
    });
    return promise;
  },
});

export default FieldsStore;
