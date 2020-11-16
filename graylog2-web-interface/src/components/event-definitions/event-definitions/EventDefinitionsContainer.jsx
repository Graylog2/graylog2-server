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

import { Spinner } from 'components/common';
import connect from 'stores/connect';
import CombinedProvider from 'injection/CombinedProvider';

import {} from 'components/event-definitions/event-definition-types';

import EventDefinitions from './EventDefinitions';

const { EventDefinitionsStore, EventDefinitionsActions } = CombinedProvider.get('EventDefinitions');

class EventDefinitionsContainer extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
  };

  componentDidMount() {
    this.fetchData({});
  }

  fetchData = ({ page, pageSize, query }) => {
    return EventDefinitionsActions.listPaginated({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  handlePageChange = (nextPage, nextPageSize) => {
    const { eventDefinitions } = this.props;

    this.fetchData({ page: nextPage, pageSize: nextPageSize, query: eventDefinitions.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { eventDefinitions } = this.props;
    const promise = this.fetchData({ query: nextQuery, pageSize: eventDefinitions.pagination.pageSize });

    promise.finally(callback);
  };

  handleDelete = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to delete "${definition.title}"?`)) {
        EventDefinitionsActions.delete(definition);
      }
    };
  };

  handleEnable = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to enable "${definition.title}"?`)) {
        EventDefinitionsActions.enable(definition);
      }
    };
  };

  handleDisable = (definition) => {
    return () => {
      if (window.confirm(`Are you sure you want to disable "${definition.title}"?`)) {
        EventDefinitionsActions.disable(definition);
      }
    };
  };

  render() {
    const { eventDefinitions } = this.props;

    if (!eventDefinitions.eventDefinitions) {
      return <Spinner text="Loading Event Definitions information..." />;
    }

    return (
      <EventDefinitions eventDefinitions={eventDefinitions.eventDefinitions}
                        context={eventDefinitions.context}
                        pagination={eventDefinitions.pagination}
                        query={eventDefinitions.query}
                        onPageChange={this.handlePageChange}
                        onQueryChange={this.handleQueryChange}
                        onDelete={this.handleDelete}
                        onEnable={this.handleEnable}
                        onDisable={this.handleDisable} />
    );
  }
}

export default connect(EventDefinitionsContainer, { eventDefinitions: EventDefinitionsStore });
