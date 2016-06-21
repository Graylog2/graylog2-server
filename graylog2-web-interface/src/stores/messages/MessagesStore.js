import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import MessageFormatter from 'logic/message/MessageFormatter';
import ApiRoutes from 'routing/ApiRoutes';
import URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';

import ActionsProvider from 'injection/ActionsProvider';
const MessagesActions = ActionsProvider.getActions('Messages');

const MessagesStore = Reflux.createStore({
  listenables: [MessagesActions],
  sourceUrl: '',

  getInitialState() {
    return {};
  },

  loadMessage(index, messageId) {
    const url = ApiRoutes.MessagesController.single(index.trim(), messageId.trim()).url;
    const promise = fetch('GET', URLUtils.qualifyUrl(url))
      .then(
        response => MessageFormatter.formatResultMessage(response),
        errorThrown => {
          UserNotification.error(`Loading message information failed with status: ${errorThrown}`,
            'Could not load message information');
        });

    MessagesActions.loadMessage.promise(promise);
  },

  fieldTerms(index, string) {
    const url = ApiRoutes.MessagesController.analyze(index, string).url;
    const promise = fetch('GET', URLUtils.qualifyUrl(url))
      .then(
        response => response.tokens,
        error => {
          UserNotification.error(`Loading field terms failed with status: ${error}`,
            'Could not load field terms.');
        });

    MessagesActions.fieldTerms.promise(promise);
  },
});

export default MessagesStore;
