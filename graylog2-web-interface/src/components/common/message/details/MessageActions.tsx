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
import { useState } from 'react';
import debounce from 'lodash/debounce';

import { ClipboardButton, JSONClipboardButton, Link } from 'components/common';
import OverlayTrigger from 'components/common/OverlayTrigger';
import PaginatedList from 'components/common/PaginatedList';
import Spinner from 'components/common/Spinner';
import Routes from 'routing/Routes';
import { Button, ButtonGroup, Input, ListGroup, ListGroupItem } from 'components/bootstrap';
import SurroundingSearchButton from 'components/search/SurroundingSearchButton';
import usePluginEntities from 'hooks/usePluginEntities';
import { TELEMETRY_EVENT_TYPE } from 'logic/telemetry/Constants';
import useSendTelemetry from 'logic/telemetry/useSendTelemetry';
import MessagePermalinkButton from 'views/components/common/MessagePermalinkButton';
import useFeature from 'hooks/useFeature';
import useStreams from 'components/streams/hooks/useStreams';

import MessageEditFieldConfigurationAction from './fields/MessageEditFieldConfigurationAction';

const PAGE_SIZE = 10;

const TestAgainstStreamButton = ({ index, id }: { index: string; id: string }) => {
  const sendTelemetry = useSendTelemetry();
  const [searchParams, setSearchParams] = useState({
    query: '',
    page: 1,
    pageSize: PAGE_SIZE,
    sort: { attributeId: 'title', direction: 'asc' as const },
  });

  const {
    data: { list: streams, pagination },
    isInitialLoading,
  } = useStreams(searchParams);

  const sendEvent = () =>
    sendTelemetry(TELEMETRY_EVENT_TYPE.SEARCH_MESSAGE_TABLE_TEST_AGAINST_STREAM, {
      app_section: 'search-message-table',
      app_action_value: 'seach-message-table-test-against-stream',
    });

  const handleSearch = debounce((value: string) => {
    setSearchParams((cur) => ({ ...cur, query: value, page: 1 }));
  }, 300);

  const handlePageChange = (newPage: number) => {
    setSearchParams((cur) => ({ ...cur, page: newPage }));
  };

  const popoverContent = (
    <>
      <Input
        type="text"
        formGroupClassName=""
        placeholder="Filter streams"
        onChange={({ target: { value } }) => handleSearch(value)}
      />
      {isInitialLoading && <Spinner />}
      <PaginatedList
        showPageSizeSelect={false}
        totalItems={pagination.total}
        hidePreviousAndNextPageLinks
        hideFirstAndLastPageLinks
        activePage={searchParams.page}
        pageSize={PAGE_SIZE}
        onChange={handlePageChange}
        useQueryParameter={false}>
        <ListGroup>
          {streams.map((stream) =>
            stream.is_default ? (
              <ListGroupItem key={stream.id} disabled>
                <span title="Cannot test against the default stream">{stream.title}</span>
              </ListGroupItem>
            ) : (
              <ListGroupItem key={stream.id}>
                <Link to={Routes.stream_edit_example(stream.id, index, id)} onClick={sendEvent}>
                  {stream.title}
                </Link>
              </ListGroupItem>
            ),
          )}
          {!isInitialLoading && streams.length === 0 && <ListGroupItem>No streams available</ListGroupItem>}
        </ListGroup>
      </PaginatedList>
    </>
  );

  return (
    <OverlayTrigger trigger="click" placement="bottom" overlay={popoverContent} title="Test against stream" rootClose>
      <Button bsSize="small">
        Test against stream <span className="caret" />
      </Button>
    </OverlayTrigger>
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
      {disableTestAgainstStream ? null : <TestAgainstStreamButton id={id} index={index} />}
      {isFavoriteFieldsEnabled && <MessageEditFieldConfigurationAction />}
    </ButtonGroup>
  );
};

export default MessageActions;
