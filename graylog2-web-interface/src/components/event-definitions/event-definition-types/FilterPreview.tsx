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
import React from 'react';

import { Panel, Table } from 'components/bootstrap';
import { Spinner } from 'components/common';
import HelpPanel from 'components/event-definitions/common/HelpPanel';

import styles from './FilterPreview.css';

type FilterPreviewProps = {
  searchResult?: any;
  errors?: any[];
  isFetchingData?: boolean;
  displayPreview?: boolean;
};

type Message = {
  index: string;
  message: {
    timestamp: string;
    _id: string;
    message: string;
  };
};

const Messages = ({ messages }: { messages: Array<Message> }) =>
  messages.map(({ index, message }) => (
    <tr key={`${index}-${message._id}`}>
      <td>{message.timestamp}</td>
      <td>{message.message}</td>
    </tr>
  ));

const SearchResult = ({
  searchResult,
  isFetchingData,
}: {
  isFetchingData: boolean;
  searchResult: { messages?: Array<Message> };
}) => {
  if (isFetchingData) return <Spinner text="Loading filter preview..." />;

  if (!searchResult.messages || searchResult.messages.length === 0) {
    return <p>Could not find any messages with the current search criteria.</p>;
  }

  return (
    <Table striped condensed bordered>
      <thead>
        <tr>
          <th>Timestamp</th>
          <th>Message</th>
        </tr>
      </thead>
      <tbody>
        <Messages messages={searchResult.messages} />
      </tbody>
    </Table>
  );
};

const FilterPreview = ({
  searchResult = {},
  displayPreview = false,
  errors = [],
  isFetchingData = false,
}: FilterPreviewProps) => {
  const hasError = errors?.length > 0;

  return (
    <>
      <HelpPanel
        collapsible
        defaultExpanded={!displayPreview}
        title="How many Events will Filter & Aggregation create?"
      >
        <p>
          The Filter & Aggregation Condition will generate different number of Events, depending on how it is
          configured:
        </p>
        <ul>
          <li>
            <b>Filter:</b>&emsp;One Event per message matching the filter
          </li>
          <li>
            <b>Aggregation without groups:</b>&emsp;One Event every time the aggregation result satisfies the condition
          </li>
          <li>
            <b>Aggregation with groups:</b>&emsp;One Event per group whose aggregation result satisfies the condition
          </li>
        </ul>
      </HelpPanel>

      {displayPreview && (
        <Panel className={styles.filterPreview} bsStyle={hasError ? 'danger' : 'default'}>
          <Panel.Heading>
            <Panel.Title>Filter Preview</Panel.Title>
          </Panel.Heading>
          <Panel.Body>
            {hasError ? (
              <p>{errors[0].description}</p>
            ) : (
              <SearchResult isFetchingData={isFetchingData} searchResult={searchResult} />
            )}
          </Panel.Body>
        </Panel>
      )}
    </>
  );
};

export default FilterPreview;
