/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import Reflux from 'reflux';

import fetch from 'logic/rest/FetchProvider';
import MessageFormatter from 'logic/message/MessageFormatter';
import ApiRoutes from 'routing/ApiRoutes';
import * as URLUtils from 'util/URLUtils';
import UserNotification from 'util/UserNotification';
import StringUtils from 'util/StringUtils';
import ActionsProvider from 'injection/ActionsProvider';

const MessagesActions = ActionsProvider.getActions('Messages');

const MessagesStore = Reflux.createStore({
  listenables: [MessagesActions],
  sourceUrl: '',

  getInitialState() {
    return {};
  },

  loadMessage(index, messageId) {
    const { url } = ApiRoutes.MessagesController.single(index.trim(), messageId.trim());
    const promise = fetch('GET', URLUtils.qualifyUrl(url))
      .then(
        (response) => MessageFormatter.formatResultMessage(response),
        (errorThrown) => {
          UserNotification.error(`Loading message information failed with status: ${errorThrown}`,
            'Could not load message information');
        },
      );

    MessagesActions.loadMessage.promise(promise);
  },

  fieldTerms(index, string) {
    const { url } = ApiRoutes.MessagesController.analyze(index, encodeURIComponent(StringUtils.stringify(string)));
    const promise = fetch('GET', URLUtils.qualifyUrl(url))
      .then(
        (response) => response.tokens,
        (error) => {
          UserNotification.error(`Loading field terms failed with status: ${error}`,
            'Could not load field terms.');
        },
      );

    MessagesActions.fieldTerms.promise(promise);
  },

  loadRawMessage(message, remoteAddress, codec, codecConfiguration) {
    const { url } = ApiRoutes.MessagesController.parse();
    const payload = {
      message: message,
      remote_address: remoteAddress,
      codec: codec,
      configuration: codecConfiguration,
    };

    const promise = fetch('POST', URLUtils.qualifyUrl(url), payload)
      .then(
        (response) => MessageFormatter.formatResultMessage(response),
        (error) => {
          if (error.additional && error.additional.status === 400) {
            UserNotification.error('Please ensure the selected codec and its configuration are right. '
              + 'Check your server logs for more information.', 'Could not load raw message');

            return;
          }

          UserNotification.error(`Loading raw message failed with status: ${error}`,
            'Could not load raw message');
        },
      );

    MessagesActions.loadRawMessage.promise(promise);
  },
});

export default MessagesStore;
