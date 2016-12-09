import Reflux from 'reflux';
import UserNotification from 'util/UserNotification';
import URLUtils from 'util/URLUtils';
import fetch from 'logic/rest/FetchProvider';

const StartpageStore = Reflux.createStore({
  listenables: [],
  sourceUrl: (username) => `/users/${username}`,

  set(username, type, id) {
    const url = URLUtils.qualifyUrl(this.sourceUrl(username));
    const payload = {};
    if (type && id) {
      payload.type = type;
      payload.id = id;
    }
    return fetch('PUT', url, {startpage: payload})
      .then(
        response => {
          this.trigger();
          UserNotification.success('Your start page was changed successfully');
          return response;
        },
        error => UserNotification.error(`Changing your start page failed with error: ${error}`, 'Could not change your start page')
      );
  },
});

export default StartpageStore;
