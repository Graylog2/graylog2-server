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
    const promise = fetch('PUT', url, {startpage: payload})
      .then(
        () => {
          this.trigger();
          UserNotification.success('Your start page was changed successfully')
        },
        (error) => UserNotification.error(`Changing your start page failed with error: ${error}`, 'Could not change your start page')
      );

    return promise;
  },
});

export default StartpageStore;
