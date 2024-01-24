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
import type * as Immutable from 'immutable';

import { LinkContainer } from 'components/common/router';
import Routes from 'routing/Routes';
import { ClipboardButton } from 'components/common';
import { Button, ButtonGroup, DropdownButton, MenuItem } from 'components/bootstrap';
import SurroundingSearchButton from 'components/search/SurroundingSearchButton';
import type { SearchesConfig } from 'components/search/SearchConfig';
import usePluginEntities from 'hooks/usePluginEntities';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import useLocation from 'routing/useLocation';
import { getPathnameWithoutId } from 'util/URLUtils';

const _getTestAgainstStreamButton = (streams: Immutable.List<any>, index: string, id: string) => {
  const sendTelemetry = useSendTelemetry();
  const location = useLocation();

  const sendEvent = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_MESSAGE_TABLE_TEST_AGAINST_STREAM, {
      app_pathname: getPathnameWithoutId(location.pathname),
      app_section: 'search-message-table',
      app_action_value: 'seach-message-table-test-against-stream',
    });
  };

  const streamList = streams.map((stream) => {
    if (stream.is_default) {
      return (
        <MenuItem key={stream.id}
                  onClick={() => sendEvent()}
                  disabled
                  title="Cannot test against the default stream">{stream.title}
        </MenuItem>
      );
    }

    return (
      <LinkContainer key={stream.id}
                     to={Routes.stream_edit_example(stream.id, index, id)}>
        <MenuItem onClick={() => sendEvent()}>{stream.title}</MenuItem>
      </LinkContainer>
    );
  });

  return (
    <DropdownButton pullRight
                    bsSize="small"
                    title="Test against stream"
                    id="select-stream-dropdown">
      {(streamList && !streamList.isEmpty()) ? streamList.toArray() : <MenuItem header>No streams available</MenuItem>}
    </DropdownButton>
  );
};

const usePluggableMessageActions = (id: string, index: string) => {
  const pluggableMenuActions = usePluginEntities('views.components.widgets.messageTable.messageActions');

  return pluggableMenuActions.filter(
    (perspective) => (perspective.useCondition ? !!perspective.useCondition() : true),
  ).map(
    ({ component: PluggableMenuAction, key }) => <PluggableMenuAction key={key} id={id} index={index} />,
  );
};

type Props = {
  index: string,
  id: string,
  fields: {
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

const MessageActions = ({
  index,
  id,
  fields,
  decorationStats,
  disabled,
  disableSurroundingSearch,
  disableTestAgainstStream,
  showOriginal,
  toggleShowOriginal,
  streams,
  searchConfig,
}: Props) => {
  const pluggableActions = usePluggableMessageActions(id, index);

  if (disabled) {
    return <ButtonGroup />;
  }

  const messageUrl = index ? Routes.message_show(index, id) : '#';

  const { timestamp, ...remainingFields } = fields;

  const surroundingSearchButton = disableSurroundingSearch || (
    <SurroundingSearchButton id={id}
                             timestamp={timestamp as string}
                             searchConfig={searchConfig}
                             messageFields={remainingFields} />
  );

  const showChanges = decorationStats
    && <Button onClick={toggleShowOriginal} active={showOriginal}>Show changes</Button>;

  return (
    <ButtonGroup>
      {showChanges}
      <Button bsSize="small" href={messageUrl}>Permalink</Button>
      {pluggableActions}

      <ClipboardButton title="Copy ID" text={id} bsSize="small" />
      <ClipboardButton title="Copy message" bsSize="small" text={JSON.stringify(fields, null, 2)} />
      {surroundingSearchButton}
      {disableTestAgainstStream ? null : _getTestAgainstStreamButton(streams, index, id)}
    </ButtonGroup>
  );
};

export default MessageActions;
