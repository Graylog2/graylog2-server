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

const DIALOG_TYPES = {
  COPY: 'copy',
  DELETE: 'delete',
  DISABLE: 'disable',
  ENABLE: 'enable',
};

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
      dialogBody: '',
      dialogTitle: '',
      showDialog: false,
      dialogType: null,
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
    this.setState({
      showDialog: true,
      dialogType: DIALOG_TYPES.DELETE,
      dialogBody: `Are you sure you want to delete "${definition.title}"?`,
      dialogTitle: 'Delete Event Definition',
      currentDefinition: definition,
    });
  };

  handleCopy = (definition) => {
    this.setState({
      showDialog: true,
      dialogType: DIALOG_TYPES.COPY,
      dialogBody: `Are you sure you want to create a copy of "${definition.title}"?`,
      dialogTitle: 'Copy Event Definition',
      currentDefinition: definition,
    });
  };

  handleEnable = (definition) => {
    this.setState({
      showDialog: true,
      dialogType: DIALOG_TYPES.ENABLE,
      dialogBody: `Are you sure you want to enable "${definition.title}"?`,
      dialogTitle: 'Enable Event Definition',
      currentDefinition: definition,
    });
  };

  handleDisable = (definition) => {
    this.setState({
      showDialog: true,
      dialogType: DIALOG_TYPES.DISABLE,
      dialogBody: 'Disable Event Definition',
      dialogTitle: `Are you sure you want to disable "${definition.title}"?`,
      currentDefinition: definition,
    });
  };

  handleConfirm = () => {
    const { dialogType, currentDefinition } = this.state;

    switch (dialogType) {
      case 'copy':
        EventDefinitionsActions.copy(currentDefinition);
        this.handleClearState();
        break;
      case 'delete':
        EventDefinitionsActions.delete(currentDefinition);
        this.handleClearState();
        break;
      case 'enable':
        EventDefinitionsActions.enable(currentDefinition);
        this.handleClearState();
        break;
      case 'disable':
        EventDefinitionsActions.disable(currentDefinition);
        this.handleClearState();
        break;
      default:
        break;
    }
  };

  handleClearState = () => {
    this.setState({
      showDialog: false,
      currentDefinition: null,
      dialogBody: '',
      dialogTitle: '',
    });
  };

  render() {
    const { eventDefinitions } = this.props;

    const {
      showDialog,
      dialogBody,
      dialogTitle,
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
        {showDialog && (
        <ConfirmDialog id="copy-event-definition-dialog"
                       title={dialogTitle}
                       show
                       onConfirm={this.handleConfirm}
                       onCancel={this.handleClearState}>
          {dialogBody}
        </ConfirmDialog>
        )}
      </>

    );
  }
}

export default connect(EventDefinitionsContainer, { eventDefinitions: EventDefinitionsStore });
