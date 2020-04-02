import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import { Alert, Col, Label, OverlayTrigger, Row, Table, Tooltip, Button } from 'components/graylog';
import { EmptyEntity, IfPermitted, PaginatedList, Timestamp, Icon } from 'components/common';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import PermissionsMixin from 'util/PermissionsMixin';

import EventsSearchBar from './EventsSearchBar';
import EventDetails from './EventDetails';

import styles from './Events.css';

const HEADERS = ['Description', 'Key', 'Type', 'Event Definition', 'Timestamp'];

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

  state = {
    expanded: [],
  };

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
          <Icon name={icon} fixedWidth className={`${style} ${styles.priority}`} />
        </OverlayTrigger>
      </>
    );
  };

  renderLinkToEventDefinition = (event, eventDefinitionContext) => {
    const { currentUser } = this.props;

    if (!eventDefinitionContext) {
      return <em>{event.event_definition_id}</em>;
    }

    return PermissionsMixin.isPermitted(currentUser.permissions,
      `eventdefinitions:edit:${eventDefinitionContext.id}`)
      ? <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>{eventDefinitionContext.title}</Link>
      : eventDefinitionContext.title;
  };

  renderEvent = (event) => {
    const { context, currentUser } = this.props;
    const { expanded } = this.state;
    const eventDefinitionContext = context.event_definitions[event.event_definition_id];

    const className = [
      styles.collapsible,
      event.priority === EventDefinitionPriorityEnum.HIGH ? 'bg-danger' : '',
    ].join(' ');

    return (
      <tbody key={event.id} className={expanded.includes(event.id) ? styles.expandedMarker : ''}>
        <tr className={className} onClick={this.expandRow(event.id)}>
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
        </tr>
        {expanded.includes(event.id) && (
          <tr className={styles.expandedRow}>
            <td colSpan={HEADERS.length + 1}>
              <EventDetails event={event} eventDefinitionContext={eventDefinitionContext} currentUser={currentUser} />
            </td>
          </tr>
        )}
      </tbody>
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
                <Table id="events-table" className={styles.eventsTable}>
                  <thead>
                    <tr>
                      {HEADERS.map((header) => <th key={header}>{header}</th>)}
                    </tr>
                  </thead>
                  {eventList.map(this.renderEvent)}
                </Table>
              )}
            </PaginatedList>
          </Col>
        </Row>
      </>
    );
  }
}

export default Events;
