import Reflux from 'reflux';
import ApiRoutes from 'routing/ApiRoutes';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const StartpageStore = Reflux.createStore({
  listenables: [],

  set(username, type, id) {
    const url = URLUtils.qualifyUrl(ApiRoutes.UsersApiController.update(username).url);
    const payload = {};
    if (type && id) {
      payload.type = type;
      payload.id = id;
    }
    return fetch('PUT', url, { startpage: payload })
      .then(
        (response) => {
          this.trigger();
          UserNotification.success('Your start page was changed successfully');
          return response;
        },
        (error) => UserNotification.error(`Changing your start page failed with error: ${error}`, 'Could not change your start page'),
      );
  },
});

export default StartpageStore;
