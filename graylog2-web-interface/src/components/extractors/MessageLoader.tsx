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
import * as React from 'react';
import { useCallback, useEffect, useRef, useState } from 'react';
import PropTypes from 'prop-types';

import ActionsProvider from 'injection/ActionsProvider';

import { Button } from '../graylog';

const MessagesActions = ActionsProvider.getActions('Messages');

type LoadMessageFormProps = {
  loadMessage: (e: React.FormEvent) => void,
  children: React.ReactNode,
  loading: boolean,
};
const LoadMessageForm = ({ loadMessage, children, loading }: LoadMessageFormProps) => (
  <div>
    <form className="form-inline message-loader-form" onSubmit={loadMessage}>
      {children}
      <Button bsStyle="info"
              disabled={loading}
              type="submit">
        {loading ? 'Loading message...' : 'Load message'}
      </Button>
    </form>
  </div>
);

type Props = {
  hidden: boolean,
  hideText: string,
  onMessageLoaded: (data: any) => void,
  messageId: undefined | string,
  index: undefined | string,
};

const useMessageLoader = (defaultMessageId: string, defaultIndex: string, onMessageLoaded: (data: any) => void): [boolean, (messageId: string, index: string) => void] => {
  const [loading, setLoading] = useState(false);
  const loadMessage = useCallback((messageId: string, index: string) => {
    if (messageId === '' || index === '') {
      return;
    }

    setLoading(true);
    const promise = MessagesActions.loadMessage(index, messageId);

    promise.then((data) => onMessageLoaded(data));
    promise.finally(() => setLoading(false));
  }, [onMessageLoaded]);

  useEffect(() => {
    if (defaultMessageId && defaultIndex) {
      loadMessage(defaultMessageId, defaultIndex);
    }
  }, [defaultMessageId, defaultIndex, loadMessage]);

  return [loading, loadMessage];
};

const MessageLoader = ({ hidden, hideText, onMessageLoaded, messageId: defaultMessageId, index: defaultIndex }: Props) => {
  const [isHidden, setIsHidden] = useState(hidden);

  const [messageId, setMessageId] = useState<string>(defaultMessageId);
  const onChangeMessageId = useCallback((e) => setMessageId(e.target.value), []);
  const messageIdRef = useRef<HTMLInputElement>();

  const [index, setIndex] = useState<string>(defaultIndex);
  const onChangeIndex = useCallback((e) => setIndex(e.target.value), []);

  const [loading, loadMessage] = useMessageLoader(defaultMessageId, defaultIndex, onMessageLoaded);

  const _loadMessage = useCallback((e?: React.FormEvent) => {
    if (e?.preventDefault) {
      e.preventDefault();
    }

    loadMessage(messageId, index);
  }, [index, loadMessage, messageId]);

  const toggleMessageForm = useCallback(() => {
    const newIsHidden = !isHidden;
    setIsHidden(newIsHidden);

    if (!newIsHidden) {
      messageIdRef.current?.focus?.();
    }
  }, [isHidden]);

  return (
    <div className="message-loader">
      {hideText || (
        <p>
          Wrong example? You can{' '}
          <Button bsStyle="link" bsSize="sm" onClick={toggleMessageForm}>load another message</Button>.
        </p>
      )}
      {hidden || (
        <LoadMessageForm loading={loading} loadMessage={_loadMessage}>
          <input ref={messageIdRef} type="text" className="form-control message-id-input" placeholder="Message ID" required value={messageId} onChange={onChangeMessageId} />
          <input type="text" className="form-control" placeholder="Index" required value={index} onChange={onChangeIndex} />
        </LoadMessageForm>
      )}
    </div>
  );
};

MessageLoader.propTypes = {
  hidden: PropTypes.bool,
  hideText: PropTypes.bool,
  onMessageLoaded: PropTypes.func.isRequired,
};

MessageLoader.defaultProps = {
  hidden: true,
  hideText: false,
};

export default MessageLoader;
