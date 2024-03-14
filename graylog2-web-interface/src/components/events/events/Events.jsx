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
import capitalize from 'lodash/capitalize';
import without from 'lodash/without';
import styled, { css } from 'styled-components';

import { Link, LinkContainer } from 'components/common/router';
import { OverlayTrigger, EmptyEntity, NoSearchResult, NoEntitiesExist, IfPermitted, PaginatedList, Timestamp, Icon } from 'components/common';
import { Col, Row, Table, Button } from 'components/bootstrap';
import withPaginationQueryParameter from 'components/common/withPaginationQueryParameter';
import Routes from 'routing/Routes';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import { isPermitted } from 'util/PermissionsMixin';

import EventsSearchBar from './EventsSearchBar';
import EventDetails from './EventDetails';
import EventTypeLabel from './EventTypeLabel';

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

const EventListContainer = styled.div`
  margin-top: -50px;
`;

export const PAGE_SIZES = [10, 25, 50, 100];
export const EVENTS_MAX_OFFSET_LIMIT = 10000;

const priorityFormatter = (eventId, priority) => {
  const priorityName = capitalize(EventDefinitionPriorityEnum.properties[priority].name);
  let style;

  switch (priority) {
    case EventDefinitionPriorityEnum.LOW:
      style = 'text-muted';
      break;
    case EventDefinitionPriorityEnum.HIGH:
      style = 'text-danger';
      break;
    default:
      style = 'text-info';
  }

  const tooltip = <>{priorityName} Priority</>;

  return (
    <OverlayTrigger placement="top" trigger={['hover', 'click', 'focus']} overlay={tooltip}>
      <EventsIcon name="thermometer" className={style} />
    </OverlayTrigger>
  );
};

const renderEmptyContent = () => (
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
    paginationQueryParameter: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);

    this.state = {
      expanded: [],
    };
  }

  expandRow = (eventId) => () => {
    const { expanded } = this.state;
    const nextExpanded = expanded.includes(eventId) ? without(expanded, eventId) : expanded.concat([eventId]);

    this.setState({ expanded: nextExpanded });
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
            {priorityFormatter(event.id, event.priority)}
            &nbsp;
            {event.message}
          </td>
          <td>{event.key || <em>none</em>}</td>
          <td><EventTypeLabel isAlert={event.alert} /></td>
          <td>
            {this.renderLinkToEventDefinition(event, eventDefinitionContext)}
          </td>
          <td><Timestamp dateTime={event.timestamp} /></td>
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
      paginationQueryParameter,
    } = this.props;

    const eventList = events.map((e) => e.event);

    if (totalEventDefinitions === 0) {
      return renderEmptyContent();
    }

    const { query, filter: { alerts: filter } } = parameters;
    const excludedFile = filter === 'exclude' ? 'Events' : 'Alerts & Events';
    const entity = (filter === 'only' ? 'Alerts' : excludedFile);
    const offsetLimitError = paginationQueryParameter.page * paginationQueryParameter.pageSize > EVENTS_MAX_OFFSET_LIMIT;

    const emptyListComponent = query ? (
      <NoSearchResult>
        No {entity} found for the current search criteria.
      </NoSearchResult>
    ) : (
      <NoEntitiesExist>
        No {entity} exist.
      </NoEntitiesExist>
    );

    const offsetLimitErrorComponent = (
      <tbody>
        <tr>
          <td colSpan={5}>
            <NoSearchResult>
              Unfortunately we can only fetch Events with an Offset (page number * rows per page) less than or equal to: [10000].
              Please use more advanced methods (Search Field and Date Filter) in order to get distant chunks of results.
            </NoSearchResult>
          </td>
        </tr>
      </tbody>
    );

    return (
      <Row>
        <Col md={12}>
          <EventsSearchBar parameters={parameters}
                           onQueryChange={onQueryChange}
                           onAlertFilterChange={onAlertFilterChange}
                           onTimeRangeChange={onTimeRangeChange}
                           onSearchReload={onSearchReload} />
          {(eventList.length === 0 && !offsetLimitError) ? (
            emptyListComponent
          ) : (
            <EventListContainer>
              <PaginatedList totalItems={totalEvents}
                             onChange={onPageChange}
                             pageSizes={PAGE_SIZES}>
                <EventsTable id="events-table">
                  <thead>
                    <tr>
                      {HEADERS.map((header) => <th key={header}>{header}</th>)}
                    </tr>
                  </thead>
                  {offsetLimitError ? (
                    offsetLimitErrorComponent
                  ) : (
                    eventList.map(this.renderEvent)
                  )}
                </EventsTable>
              </PaginatedList>
            </EventListContainer>
          )}
        </Col>
      </Row>
    );
  }
}

export default withPaginationQueryParameter(Events);
