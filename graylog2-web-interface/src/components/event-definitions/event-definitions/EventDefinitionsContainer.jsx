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
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import connect from 'stores/connect';
import { EventDefinitionsActions, EventDefinitionsStore } from 'stores/event-definitions/EventDefinitionsStore';

import {} from 'components/event-definitions/event-definition-types';

import EventDefinitions, { PAGE_SIZES } from './EventDefinitions';

const DIALOG_TYPES = {
  COPY: 'copy',
  DELETE: 'delete',
  DISABLE: 'disable',
  ENABLE: 'enable',
};

const DIALOG_TEXT = {
  [DIALOG_TYPES.COPY]: {
    dialogTitle: 'Copy Event Definition',
    dialogBody: (definitionTitle) => `Are you sure you want to create a copy of "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.DELETE]: {
    dialogTitle: 'Delete Event Definition',
    dialogBody: (definitionTitle) => `Are you sure you want to delete "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.DISABLE]: {
    dialogTitle: 'Disable Event Definition',
    dialogBody: (definitionTitle) => `Are you sure you want to disable "${definitionTitle}"?`,
  },
  [DIALOG_TYPES.ENABLE]: {
    dialogTitle: 'Enable Event Definition',
    dialogBody: (definitionTitle) => `Are you sure you want to enable "${definitionTitle}"?`,
  },
};

class EventDefinitionsContainer extends React.Component {
  static propTypes = {
    eventDefinitions: PropTypes.object.isRequired,
    paginationQueryParameter: PropTypes.object.isRequired,
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
      showDialog: false,
      dialogType: null,
    };
  }

  componentDidMount() {
    const { page, pageSize } = this.props.paginationQueryParameter;
    EventDefinitionsContainer.fetchData({ page, pageSize });
  }

  handlePageChange = (nextPage, nextPageSize) => {
    const { eventDefinitions } = this.props;

    EventDefinitionsContainer.fetchData({ page: nextPage, pageSize: nextPageSize, query: eventDefinitions.query });
  };

  handleQueryChange = (nextQuery, callback = () => {}) => {
    const { resetPage, pageSize } = this.props.paginationQueryParameter;

    resetPage();

    const promise = EventDefinitionsContainer.fetchData({ query: nextQuery, pageSize, page: 1 });

    promise.finally(callback);
  };

  handleAction = (action) => (definition) => {
    switch (action) {
      case DIALOG_TYPES.COPY:
        this.setState({
          showDialog: true,
          dialogType: DIALOG_TYPES.COPY,
          currentDefinition: definition,
        });

        break;
      case DIALOG_TYPES.DELETE:
        this.setState({
          showDialog: true,
          dialogType: DIALOG_TYPES.DELETE,
          currentDefinition: definition,
        });

        break;
      case DIALOG_TYPES.ENABLE:
        this.setState({
          showDialog: true,
          dialogType: DIALOG_TYPES.ENABLE,
          currentDefinition: definition,
        });

        break;
      case DIALOG_TYPES.DISABLE:
        this.setState({
          showDialog: true,
          dialogType: DIALOG_TYPES.DISABLE,
          currentDefinition: definition,
        });

        break;
      default:
        break;
    }
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
      dialogType: null,
    });
  };

  render() {
    const { eventDefinitions } = this.props;

    const {
      currentDefinition,
      dialogType,
      showDialog,
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
                          onDelete={this.handleAction(DIALOG_TYPES.DELETE)}
                          onCopy={this.handleAction(DIALOG_TYPES.COPY)}
                          onEnable={this.handleAction(DIALOG_TYPES.ENABLE)}
                          onDisable={this.handleAction(DIALOG_TYPES.DISABLE)} />
        {showDialog && (
        <ConfirmDialog id="copy-event-definition-dialog"
                       title={DIALOG_TEXT[dialogType].dialogTitle}
                       show
                       onConfirm={this.handleConfirm}
                       onCancel={this.handleClearState}>
          {DIALOG_TEXT[dialogType].dialogBody(currentDefinition.title)}
        </ConfirmDialog>
        )}
      </>

    );
  }
}

export default connect(withPaginationQueryParameter(EventDefinitionsContainer, { pageSizes: PAGE_SIZES }), { eventDefinitions: EventDefinitionsStore });
