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
import React, { useState } from 'react';
import styled, { css } from 'styled-components';

import { Input } from 'components/bootstrap';
import ConfirmDialog from 'components/common/ConfirmDialog';
import UserNotification from 'util/UserNotification';
import useCurrentUser from 'hooks/useCurrentUser';
import StreamsStore, { type Stream } from 'stores/streams/StreamsStore';
import { isPermitted } from 'util/PermissionsMixin';

const StreamRuleConnector = styled.div(({ theme }) => css`
  margin-top: 10px;
  margin-bottom: 13px;

  label {
    font-size: ${theme.fonts.size.small};
  }

  .form-group {
    margin-bottom: 5px;
  }

  .radio {
    margin-top: 0;
    margin-bottom: 0;
  }

  input[type='radio'] {
    margin-top: 2px;
    margin-bottom: 2px;
  }
`);

type Props = {
  stream: Stream,
  onChange: () => void,
}

const MatchingTypeSwitcher = ({ stream, onChange }: Props) => {
  const [matchingType, setMatchingType] = useState<'AND'|'OR'|undefined>(undefined);
  const currentUser = useCurrentUser();
  const disabled = stream.is_default || !stream.is_editable || !isPermitted(currentUser.permissions, `streams:edit:${stream.id}`);

  const handleTypeChange = (newValue: 'AND'|'OR') => {
    StreamsStore.update(stream.id, { matching_type: newValue }, (response) => {
      onChange();

      UserNotification.success(`Messages will now be routed into the stream when ${newValue === 'AND' ? 'all' : 'any'} rules are matched`,
        'Success');

      return response;
    });
  };

  return (
    <StreamRuleConnector>
      <div>
        <Input id="streamrule-and-connector"
               type="radio"
               label="A message must match all of the following rules"
               checked={stream.matching_type === 'AND'}
               onChange={() => setMatchingType('AND')}
               disabled={disabled} />
        <Input id="streamrule-or-connector"
               type="radio"
               label="A message must match at least one of the following rules"
               checked={stream.matching_type === 'OR'}
               onChange={() => setMatchingType('OR')}
               disabled={disabled} />
      </div>
      {matchingType && (
        <ConfirmDialog show
                       title={`Switch matching type to ${matchingType === 'AND' ? 'ALL' : 'ANY'}`}
                       onConfirm={() => handleTypeChange(matchingType)}
                       onCancel={() => setMatchingType(undefined)}>
          You are about to change how rules are applied to this stream, do you want to continue? Changes will take effect immediately.
        </ConfirmDialog>
      )}
    </StreamRuleConnector>
  );
};

export default MatchingTypeSwitcher;
