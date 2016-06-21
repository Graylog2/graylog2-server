import Reflux from 'reflux';
import moment from 'moment';

import fetch from 'logic/rest/FetchProvider';
import MessageFieldsFilter from 'logic/message/MessageFieldsFilter';
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
        response => {
          const message = response.message;
          const fields = message.fields;
          const filteredFields = MessageFieldsFilter.filterFields(fields);

          return {
            id: message.id,
            timestamp: moment(message.timestamp).unix(),
            filtered_fields: filteredFields,
            formatted_fields: filteredFields,
            fields: fields,
            index: response.index,
            source_node_id: fields.gl2_source_node,
            source_input_id: fields.gl2_source_input,
            stream_ids: message.streams,
          };
        },
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
