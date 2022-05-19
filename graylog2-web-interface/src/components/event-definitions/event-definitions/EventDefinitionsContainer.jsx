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

import { ConfirmDialog, Spinner } from 'components/common';
import connect from 'stores/connect';
import { EventDefinitionsActions, EventDefinitionsStore } from 'stores/event-definitions/EventDefinitionsStore';

import {} from 'components/event-definitions/event-definition-types';

import EventDefinitions from './EventDefinitions';

class EventDefinitionsContainer extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
  };

  static fetchData = ({ page, pageSize, query }) => {
    return EventDefinitionsActions.listPaginated({
      query: query,
      page: page,
      pageSize: pageSize,
    });
  };

  constructor(props) {
    super(props);
    this.props = props;

    this.state = {
      currentDefinition: null,
      showCopyDialog: false,
      showDeleteDialog: false,
      showDisableDialog: false,
      showEnableDialog: false,
    };
  }

  componentDidMount() {
    EventDefinitionsContainer.fetchData({});
  }

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
    this.setState({ showDeleteDialog: true, currentDefinition: definition });
  };

  handleCopy = (definition) => {
    this.setState({ showCopyDialog: true, currentDefinition: definition });
  };

  handleEnable = (definition) => {
    this.setState({ showEnableDialog: true, currentDefinition: definition });
  };

  handleDisable = (definition) => {
    this.setState({ showDisableDialog: true, currentDefinition: definition });
  };

  handleClearState = () => {
    this.setState({
      showCopyDialog: false,
      showDeleteDialog: false,
      showDisableDialog: false,
      showEnableDialog: false,
      currentDefinition: null,
    });
  };

  render() {
    const { eventDefinitions } = this.props;

    const {
      currentDefinition,
      showCopyDialog,
      showDeleteDialog,
      showDisableDialog,
      showEnableDialog,
    } = this.state;

    if (!eventDefinitions.eventDefinitions) {
      return <Spinner text="Loading Event Definitions information..." />;
    }

    return (
      <>
        <EventDefinitions eventDefinitions={eventDefinitions.eventDefinitions}
                          context={eventDefinitions.context}
                          pagination={eventDefinitions.pagination}
                          query={eventDefinitions.query}
                          onPageChange={this.handlePageChange}
                          onQueryChange={this.handleQueryChange}
                          onDelete={this.handleDelete}
                          onCopy={this.handleCopy}
                          onEnable={this.handleEnable}
                          onDisable={this.handleDisable} />
        {showCopyDialog && (
        <ConfirmDialog id="copy-event-definition-dialog"
                       title="Copy Event Definition"
                       show
                       onConfirm={() => {
                         EventDefinitionsActions.copy(currentDefinition);
                         this.handleClearState();
                       }}
                       onCancel={this.handleClearState}>
          {`Are you sure you want to create a copy of "${currentDefinition?.title || ''}"?`}
        </ConfirmDialog>
        )}
        {showDeleteDialog && (
        <ConfirmDialog id="delete-event-definition-dialog"
                       title="Delete Event Definition"
                       show
                       onConfirm={() => {
                         EventDefinitionsActions.delete(currentDefinition);
                         this.handleClearState();
                       }}
                       onCancel={this.handleClearState}>
          {`Are you sure you want to delete "${currentDefinition?.title || ''}"?`}
        </ConfirmDialog>
        )}
        {showDisableDialog && (
        <ConfirmDialog id="disable-event-definition-dialog"
                       title="Disable Event Definition"
                       show
                       onConfirm={() => {
                         EventDefinitionsActions.disable(currentDefinition);
                         this.handleClearState();
                       }}
                       onCancel={this.handleClearState}>
          {`Are you sure you want to disable "${currentDefinition?.title || ''}"?`}
        </ConfirmDialog>
        )}
        {showEnableDialog && (
        <ConfirmDialog id="enable-event-definition-dialog"
                       title="Enable Event Definition"
                       show
                       onConfirm={() => {
                         EventDefinitionsActions.enable(currentDefinition);
                         this.handleClearState();
                       }}
                       onCancel={this.handleClearState}>
          {`Are you sure you want to enable "${currentDefinition?.title || ''}"?`}
        </ConfirmDialog>
        )}

      </>

    );
  }
}

export default connect(EventDefinitionsContainer, { eventDefinitions: EventDefinitionsStore });
