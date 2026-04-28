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

import { ClipboardButton, JSONClipboardButton } from 'components/common';
import SelectPopover from 'components/common/SelectPopover';
import Routes from 'routing/Routes';
import useHistory from 'routing/useHistory';
import { Button, ButtonGroup } from 'components/bootstrap';
import SurroundingSearchButton from 'components/search/SurroundingSearchButton';
import usePluginEntities from 'hooks/usePluginEntities';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import MessagePermalinkButton from 'views/components/common/MessagePermalinkButton';
import useFeature from 'hooks/useFeature';

import MessageEditFieldConfigurationAction from './fields/MessageEditFieldConfigurationAction';

const TestAgainstStreamButton = ({
  streams,
  index,
  id,
}: {
  streams: Immutable.List<any>;
  index: string;
  id: string;
}) => {
  const sendTelemetry = useSendTelemetry();
  const history = useHistory();

  const sendEvent = () => {
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_MESSAGE_TABLE_TEST_AGAINST_STREAM, {
      app_section: 'search-message-table',
      app_action_value: 'seach-message-table-test-against-stream',
    });
  };

  const sortedStreams = streams.sortBy((stream) => stream.title.toLowerCase());
  const streamTitles = sortedStreams.map((stream) => stream.title).toArray();

  const handleSelect = ([selectedTitle]: string[], hide: () => void) => {
    const stream = sortedStreams.find((s) => s.title === selectedTitle);

    if (!stream || stream.is_default) {
      hide();

      return;
    }

    sendEvent();
    hide();

    history.push(Routes.stream_edit_example(stream.id, index, id));
  };

  const itemFormatter = (title: string) => {
    const stream = sortedStreams.find((s) => s.title === title);

    if (stream?.is_default) {
      return (
        <span title="Cannot test against the default stream" style={{ opacity: 0.5, cursor: 'default' }}>
          {title}
        </span>
      );
    }

    return title;
  };

  return (
    <SelectPopover
      title="Test against stream"
      triggerNode={<Button bsSize="small" disabled={streams.isEmpty()}>Test against stream <span className="caret" /></Button>}
      items={streamTitles}
      itemFormatter={itemFormatter}
      onItemSelect={handleSelect}
      filterPlaceholder="Filter streams"
    />
  );
};

const usePluggableMessageActions = (id: string, index: string) => {
  const pluggableMenuActions = usePluginEntities('views.components.widgets.messageTable.messageActions');

  return pluggableMenuActions
    .filter((actions) => (actions.useCondition ? !!actions.useCondition() : true))
    .map(({ component: PluggableMenuAction, key }) => <PluggableMenuAction key={key} id={id} index={index} />);
};

type Props = {
  index: string;
  id: string;
  fields: {
    [key: string]: unknown;
  };
  decorationStats: any;
  disabled: boolean;
  disableSurroundingSearch: boolean;
  disableTestAgainstStream: boolean;
  showOriginal: boolean;
  toggleShowOriginal: () => void;
  streams: Immutable.List<any>;
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
}: Props) => {
  const pluggableActions = usePluggableMessageActions(id, index);
  const isFavoriteFieldsEnabled = useFeature('message_table_favorite_fields');

  if (disabled) {
    return <ButtonGroup />;
  }

  const { timestamp, ...remainingFields } = fields;

  const surroundingSearchButton = disableSurroundingSearch || (
    <SurroundingSearchButton id={id} timestamp={String(timestamp)} messageFields={remainingFields} />
  );

  const showChanges = decorationStats && (
    <Button onClick={toggleShowOriginal} active={showOriginal}>
      Show changes
    </Button>
  );

  return (
    <ButtonGroup>
      {showChanges}
      <MessagePermalinkButton messageIndex={index} messageId={id} />
      {pluggableActions}

      <ClipboardButton title="Copy ID" text={id} bsSize="small" />
      <JSONClipboardButton title="Copy message" bsSize="small" content={fields} />
      {surroundingSearchButton}
      {disableTestAgainstStream ? null : <TestAgainstStreamButton streams={streams} id={id} index={index} />}
      {isFavoriteFieldsEnabled && <MessageEditFieldConfigurationAction />}
    </ButtonGroup>
  );
};

export default MessageActions;
