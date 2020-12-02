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
import lodash from 'lodash';
import styled, { css } from 'styled-components';

import { Link, LinkContainer } from 'components/graylog/router';
import { Alert, Col, Label, OverlayTrigger, Row, Table, Tooltip, Button } from 'components/graylog';
import { EmptyEntity, IfPermitted, PaginatedList, Timestamp, Icon } from 'components/common';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { isPermitted } from 'util/PermissionsMixin';

import EventsSearchBar from './EventsSearchBar';
import EventDetails from './EventDetails';

const HEADERS = ['Description', 'Key', 'Type', 'Event Definition', 'Timestamp'];

const ExpandedTR = styled.tr(({ theme }) => css`
  > td {
    border-top: 1px solid ${theme.colors.gray[80]} !important;
    padding: 10px 8px 8px 35px !important;
  }

  dd {
    margin-bottom: 0.25em;
  }

  dl {
    > dl,
    > ul {
      padding-left: 1.5em;
    }
  }

  ul {
    list-style-type: disc;
  }
`);

const EventsTbody = styled.tbody(({ expanded, theme }) => css`
    border-left: ${expanded ? `3px solid ${theme.colors.variant.light.info}` : ''};
    border-collapse: ${expanded ? 'separate' : 'collapse'};
`);

const CollapsibleTr = styled.tr`
  cursor: pointer;
`;

const EventsTable = styled(Table)(({ theme }) => css`
  tr {
    &:hover {
      background-color: ${theme.colors.gray[90]};
    }

    &${ExpandedTR} {
      &:hover {
        background-color: ${theme.colors.global.contentBackground};
      }
    }
  }
`);

const EventsIcon = styled(Icon)(({ theme }) => css`
  font-size: ${theme.fonts.size.large};
  vertical-align: top;
`);

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    totalEventDefinitions: PropTypes.number.isRequired,
    context: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onAlertFilterChange: PropTypes.func.isRequired,
    onTimeRangeChange: PropTypes.func.isRequired,
    onSearchReload: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      expanded: [],
    };
  }

  handlePageSizeChange = (nextPageSize) => {
    const { onPageChange } = this.props;

    onPageChange(1, nextPageSize);
  };

  expandRow = (eventId) => {
    return () => {
      const { expanded } = this.state;
      const nextExpanded = expanded.includes(eventId) ? lodash.without(expanded, eventId) : expanded.concat([eventId]);

      this.setState({ expanded: nextExpanded });
    };
  };

  priorityFormatter = (eventId, priority) => {
    const priorityName = lodash.capitalize(EventDefinitionPriorityEnum.properties[priority].name);
    let icon;
    let style;

    switch (priority) {
      case EventDefinitionPriorityEnum.LOW:
        icon = 'thermometer-empty';
        style = 'text-muted';
        break;
      case EventDefinitionPriorityEnum.HIGH:
        icon = 'thermometer-full';
        style = 'text-danger';
        break;
      default:
        icon = 'thermometer-half';
        style = 'text-info';
    }

    const tooltip = <Tooltip id={`priority-${eventId}`}>{priorityName} Priority</Tooltip>;

    return (
      <>
        <OverlayTrigger placement="top" overlay={tooltip}>
          <EventsIcon name={icon} fixedWidth className={style} />
        </OverlayTrigger>
      </>
    );
  };

  renderLinkToEventDefinition = (event, eventDefinitionContext) => {
    const { currentUser } = this.props;

    if (!eventDefinitionContext) {
      return <em>{event.event_definition_id}</em>;
    }

    return isPermitted(currentUser.permissions,
      `eventdefinitions:edit:${eventDefinitionContext.id}`)
      ? <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>{eventDefinitionContext.title}</Link>
      : eventDefinitionContext.title;
  };

  renderEvent = (event) => {
    const { context, currentUser } = this.props;
    const { expanded } = this.state;
    const eventDefinitionContext = context.event_definitions[event.event_definition_id];

    return (
      <EventsTbody key={event.id} expanded={expanded.includes(event.id)}>
        <CollapsibleTr className={event.priority === EventDefinitionPriorityEnum.HIGH ? 'bg-danger' : ''}
                       onClick={this.expandRow(event.id)}>
          <td>
            {this.priorityFormatter(event.id, event.priority)}
            &nbsp;
            {event.message}
          </td>
          <td>{event.key || <em>none</em>}</td>
          <td>{event.alert ? <Label bsStyle="warning">Alert</Label> : <Label bsStyle="info">Event</Label>}</td>
          <td>
            {this.renderLinkToEventDefinition(event, eventDefinitionContext)}
          </td>
          <td><Timestamp dateTime={event.timestamp} format={DateTime.Formats.DATETIME} /></td>
        </CollapsibleTr>
        {expanded.includes(event.id) && (
          <ExpandedTR>
            <td colSpan={HEADERS.length + 1}>
              <EventDetails event={event} eventDefinitionContext={eventDefinitionContext} currentUser={currentUser} />
            </td>
          </ExpandedTR>
        )}
      </EventsTbody>
    );
  };

  renderEmptyContent = () => {
    return (
      <Row>
        <Col md={6} mdOffset={3} lg={4} lgOffset={4}>
          <EmptyEntity title="Looks like you didn't define any Events yet">
            <p>
              Create Event Definitions that are able to search, aggregate or correlate Messages and other
              Events, allowing you to record significant Events in Graylog and alert on them.
            </p>
            <IfPermitted permissions="eventdefinitions:create">
              <LinkContainer to={Routes.ALERTS.DEFINITIONS.CREATE}>
                <Button bsStyle="success">Get Started!</Button>
              </LinkContainer>
            </IfPermitted>
          </EmptyEntity>
        </Col>
      </Row>
    );
  };

  render() {
    const {
      events,
      parameters,
      totalEvents,
      totalEventDefinitions,
      onPageChange,
      onQueryChange,
      onAlertFilterChange,
      onTimeRangeChange,
      onSearchReload,
    } = this.props;

    const eventList = events.map((e) => e.event);

    if (totalEventDefinitions === 0) {
      return this.renderEmptyContent();
    }

    const filter = parameters.filter.alerts;
    const excludedFile = filter === 'exclude' ? 'Events' : 'Alerts & Events';
    const entity = (filter === 'only' ? 'Alerts' : excludedFile);

    return (
      <>
        <Row>
          <Col md={12}>
            <EventsSearchBar parameters={parameters}
                             onQueryChange={onQueryChange}
                             onAlertFilterChange={onAlertFilterChange}
                             onTimeRangeChange={onTimeRangeChange}
                             onPageSizeChange={this.handlePageSizeChange}
                             onSearchReload={onSearchReload}
                             pageSize={parameters.pageSize}
                             pageSizes={[10, 25, 50, 100]} />
            <PaginatedList activePage={parameters.page}
                           pageSize={parameters.pageSize}
                           showPageSizeSelect={false}
                           totalItems={totalEvents}
                           onChange={onPageChange}>
              {eventList.length === 0 ? (
                <Alert bsStyle="info">No {entity} found for the current search criteria.</Alert>
              ) : (
                <EventsTable id="events-table">
                  <thead>
                    <tr>
                      {HEADERS.map((header) => <th key={header}>{header}</th>)}
                    </tr>
                  </thead>
                  {eventList.map(this.renderEvent)}
                </EventsTable>
              )}
            </PaginatedList>
          </Col>
        </Row>
      </>
    );
  }
}

export default Events;
