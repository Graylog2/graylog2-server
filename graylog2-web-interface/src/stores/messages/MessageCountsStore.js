import Reflux from 'reflux';

import URLUtils from 'util/URLUtils';
import ApiRoutes from 'routing/ApiRoutes';
import fetch from 'logic/rest/FetchProvider';

import ActionsProvider from 'injection/ActionsProvider';
const MessageCountsActions = ActionsProvider.getActions('MessageCounts');

const MessageCountsStore = Reflux.createStore({
  listenables: [MessageCountsActions],
  events: undefined,

  init() {
    this.total();
  },
  total() {
    const url = URLUtils.qualifyUrl(ApiRoutes.CountsApiController.total().url);
    const promise = fetch('GET', url).then((response) => {
      this.events = response.events;
      this.trigger({ events: response.events });
      return response.events;
    });

    MessageCountsActions.total.promise(promise);

    return promise;
  },
});

export default MessageCountsStore;
