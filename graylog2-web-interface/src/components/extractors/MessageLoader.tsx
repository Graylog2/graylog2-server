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
import styled from 'styled-components';
import { useTranslation } from 'react-i18next';

import { FormSubmit } from 'components/common';
import useHistory from 'routing/useHistory';
import { fetchMessage } from 'views/hooks/useMessage';

import { Button } from '../bootstrap';

const StyledFormSubmit = styled(FormSubmit)`
  margin-top: 10px;
`;

type LoadMessageFormProps = {
  loadMessage: (e: React.FormEvent) => void;
  children: React.ReactNode;
  loading: boolean;
};

const LoadMessageForm = ({ loadMessage, children, loading }: LoadMessageFormProps) => {
  const { t } = useTranslation('extractors');
  const history = useHistory();

  return (
    <div>
      <form className="form-inline message-loader-form" onSubmit={loadMessage}>
        {children}
        <StyledFormSubmit
          submitButtonText={t('message_loader.load')}
          isSubmitting={loading}
          submitLoadingText={t('message_loader.loading')}
          isAsyncSubmit
          displayCancel
          onCancel={() => history.goBack()}
        />
      </form>
    </div>
  );
};

type Props = {
  hidden?: boolean;
  hideText?: boolean;
  onMessageLoaded: (data: any) => void;
  messageId?: string;
  index?: string;
};

const MessageLoader = ({
  hidden = true,
  hideText = false,
  onMessageLoaded,
  messageId: defaultMessageId = '',
  index: defaultIndex = '',
}: Props) => {
  const { t } = useTranslation('extractors');
  const [loading, setLoading] = useState(false);
  const [isHidden, setIsHidden] = useState(hidden);

  const [messageId, setMessageId] = useState<string>(defaultMessageId);
  const onChangeMessageId = useCallback((e: React.ChangeEvent<HTMLInputElement>) => setMessageId(e.target.value), []);
  const messageIdRef = useRef<HTMLInputElement>();

  const [index, setIndex] = useState<string>(defaultIndex);
  const onChangeIndex = useCallback((e: React.ChangeEvent<HTMLInputElement>) => setIndex(e.target.value), []);

  const _loadMessage = useCallback(
    (e?: React.FormEvent) => {
      e?.preventDefault?.();

      setLoading(true);
      fetchMessage(index, messageId)
        .then(onMessageLoaded)
        .finally(() => setLoading(false));
    },
    [index, messageId, onMessageLoaded],
  );

  const toggleMessageForm = useCallback(() => {
    setIsHidden(!isHidden);
  }, [isHidden]);

  useEffect(() => {
    if (!isHidden && messageIdRef.current) {
      messageIdRef.current.focus();
    }
  }, [isHidden]);

  return (
    <div className="message-loader">
      {hideText || (
        <p>
          {t('message_loader.wrong_example')}{' '}
          <Button bsSize="sm" onClick={toggleMessageForm}>
            {t('message_loader.load_another')}
          </Button>
        </p>
      )}
      {isHidden || (
        <LoadMessageForm loading={loading} loadMessage={_loadMessage}>
          <input
            ref={messageIdRef}
            type="text"
            className="form-control message-id-input"
            placeholder={t('message_loader.message_id')}
            aria-label={t('message_loader.message_id')}
            required
            value={messageId}
            onChange={onChangeMessageId}
          />
          <input
            type="text"
            className="form-control"
            placeholder={t('message_loader.index')}
            aria-label={t('message_loader.index')}
            required
            value={index}
            onChange={onChangeIndex}
          />
        </LoadMessageForm>
      )}
    </div>
  );
};

export default MessageLoader;
