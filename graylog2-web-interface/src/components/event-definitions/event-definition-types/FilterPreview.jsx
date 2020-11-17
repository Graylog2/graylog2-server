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
import PropTypes from 'prop-types';

import { Panel, Table } from 'components/graylog';
import { Spinner } from 'components/common';
import HelpPanel from 'components/event-definitions/common/HelpPanel';

import styles from './FilterPreview.css';

class FilterPreview extends React.Component {
  static propTypes = {
    searchResult: PropTypes.object,
    errors: PropTypes.array,
    isFetchingData: PropTypes.bool,
    displayPreview: PropTypes.bool,
  };

  static defaultProps = {
    searchResult: {},
    errors: [],
    isFetchingData: false,
    displayPreview: false,
  };

  renderMessages = (messages) => {
    return messages.map(({ index, message }) => {
      return (
        <tr key={`${index}-${message._id}`}>
          <td>{message.timestamp}</td>
          <td>{message.message}</td>
        </tr>
      );
    });
  };

  renderSearchResult = (searchResult = {}) => {
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
          {this.renderMessages(searchResult.messages)}
        </tbody>
      </Table>
    );
  };

  render() {
    const { isFetchingData, searchResult, errors, displayPreview } = this.props;

    const renderedResults = isFetchingData ? <Spinner text="Loading filter preview..." /> : this.renderSearchResult(searchResult);

    return (
      <>
        <HelpPanel collapsible
                   defaultExpanded={!displayPreview}
                   title="How many Events will Filter & Aggregation create?">
          <p>
            The Filter & Aggregation Condition will generate different number of Events, depending on how it is
            configured:
          </p>
          <ul>
            <li><b>Filter:</b>&emsp;One Event per message matching the filter</li>
            <li>
              <b>Aggregation without groups:</b>&emsp;One Event every time the aggregation result satisfies
              the condition
            </li>
            <li>
              <b>Aggregation with groups:</b>&emsp;One Event per group whose aggregation result satisfies
              the condition
            </li>
          </ul>
        </HelpPanel>

        {displayPreview && (
          <Panel className={styles.filterPreview} bsStyle="default">
            <Panel.Heading>
              <Panel.Title>Filter Preview</Panel.Title>
            </Panel.Heading>
            <Panel.Body>
              {errors.length > 0 ? <p className="text-danger">{errors[0].description}</p> : renderedResults}
            </Panel.Body>
          </Panel>
        )}
      </>
    );
  }
}

export default FilterPreview;
