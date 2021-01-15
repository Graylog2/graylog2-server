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
import * as Immutable from 'immutable';

import { LinkContainer } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { ClipboardButton } from 'components/common';
import { Button, ButtonGroup, DropdownButton, MenuItem } from 'components/graylog';
import SurroundingSearchButton from 'components/search/SurroundingSearchButton';
import type { SearchesConfig } from 'components/search/SearchConfig';

type Props = {
  index: string,
  id: string,
  fields: {
    timestamp: string,
    [key: string]: unknown,
  },
  decorationStats: any,
  disabled: boolean,
  disableSurroundingSearch: boolean,
  disableTestAgainstStream: boolean,
  showOriginal: boolean,
  toggleShowOriginal: () => void,
  streams: Immutable.List<any>,
  searchConfig: SearchesConfig,
};

const _getTestAgainstStreamButton = (streams, index, id) => {
  const streamList = streams.map((stream) => {
    if (stream.is_default) {
      return <MenuItem key={stream.id} disabled title="Cannot test against the default stream">{stream.title}</MenuItem>;
    }

    return (
      <LinkContainer key={stream.id}
                     to={Routes.stream_edit_example(stream.id, index, id)}>
        <MenuItem>{stream.title}</MenuItem>
      </LinkContainer>
    );
  });

  return (
    <DropdownButton pullRight
                    bsSize="small"
                    title="Test against stream"
                    id="select-stream-dropdown">
      {streamList || <MenuItem header>No streams available</MenuItem>}
    </DropdownButton>
  );
};

const MessageActions = ({ index, id, fields, decorationStats, disabled, disableSurroundingSearch, disableTestAgainstStream, showOriginal, toggleShowOriginal, streams, searchConfig }: Props) => {
  if (disabled) {
    return <ButtonGroup className="pull-right" bsSize="small" />;
  }

  const messageUrl = index ? Routes.message_show(index, id) : '#';

  const { timestamp, ...remainingFields } = fields;

  const surroundingSearchButton = disableSurroundingSearch || (
    <SurroundingSearchButton id={id}
                             timestamp={timestamp}
                             searchConfig={searchConfig}
                             messageFields={remainingFields} />
  );

  const showChanges = decorationStats && <Button onClick={toggleShowOriginal} active={showOriginal}>Show changes</Button>;

  return (
    <ButtonGroup className="pull-right" bsSize="small">
      {showChanges}
      <Button href={messageUrl}>Permalink</Button>

      <ClipboardButton title="Copy ID" text={id} />
      {surroundingSearchButton}
      {disableTestAgainstStream ? null : _getTestAgainstStreamButton(streams, index, id)}
    </ButtonGroup>
  );
};

export default MessageActions;
