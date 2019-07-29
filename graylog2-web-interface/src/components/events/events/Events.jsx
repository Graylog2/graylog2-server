import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Alert, Button, Col, Label, OverlayTrigger, Row, Table, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router';
import { LinkContainer } from 'react-router-bootstrap';

import { EmptyEntity, PaginatedList, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

import EventsSearchBar from './EventsSearchBar';
import EventDetails from './EventDetails';

import styles from './Events.css';

const HEADERS = ['Description', 'Key', 'Type', 'Event Definition', 'Timestamp'];

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    totalEventDefinitions: PropTypes.number.isRequired,
    context: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
    onAlertFilterChange: PropTypes.func.isRequired,
    onTimeRangeChange: PropTypes.func.isRequired,
  };

  state = {
    expanded: [],
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
        icon = 'fa-thermometer-empty';
        style = 'text-muted';
        break;
      case EventDefinitionPriorityEnum.HIGH:
        icon = 'fa-thermometer-full';
        style = 'text-danger';
        break;
      default:
        icon = 'fa-thermometer-half';
        style = 'text-info';
    }

    const tooltip = <Tooltip id={`priority-${eventId}`}>{priorityName} Priority</Tooltip>;

    return (
      <React.Fragment>
        <OverlayTrigger placement="top" overlay={tooltip}>
          <i className={`fa fa-fw ${icon} ${style} ${styles.priority}`} />
        </OverlayTrigger>
      </React.Fragment>
    );
  };

  renderEventFields = (eventFields) => {
    const fieldNames = Object.keys(eventFields);
    return (
      <ul>
        {fieldNames.map((fieldName) => {
          return (
            <React.Fragment key={fieldName}>
              <li><b>{fieldName}</b> {eventFields[fieldName]}</li>
            </React.Fragment>
          );
        })}
      </ul>
    );
  };

  renderEvent = (event) => {
    const { context } = this.props;
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
            {eventDefinitionContext ? (
              <Link to={Routes.NEXT_ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>
                {eventDefinitionContext.title}
              </Link>
            ) : (
              <em>{event.event_definition_id}</em>
            )}
          </td>
          <td><Timestamp dateTime={event.timestamp} format={DateTime.Formats.DATETIME} /></td>
        </tr>
        {expanded.includes(event.id) && (
          <tr className={styles.expandedRow}>
            <td colSpan={HEADERS.length + 1}>
              <EventDetails event={event} eventDefinitionContext={eventDefinitionContext} />
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
            <LinkContainer to={Routes.NEXT_ALERTS.DEFINITIONS.CREATE}>
              <Button bsStyle="success">Get Started!</Button>
            </LinkContainer>
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
    } = this.props;

    const eventList = events.map(e => e.event);

    if (totalEventDefinitions === 0) {
      return this.renderEmptyContent();
    }

    return (
      <React.Fragment>
        <Row>
          <Col md={12}>
            <EventsSearchBar parameters={parameters}
                             onQueryChange={onQueryChange}
                             onAlertFilterChange={onAlertFilterChange}
                             onTimeRangeChange={onTimeRangeChange} />
            <PaginatedList activePage={parameters.page}
                           pageSize={parameters.pageSize}
                           pageSizes={[10, 25, 50, 100]}
                           totalItems={totalEvents}
                           onChange={onPageChange}>
              {eventList.length === 0 ? (
                <Alert bsStyle="info">No Events found for the current search criteria.</Alert>
              ) : (
                <Table id="events-table" className={styles.eventsTable}>
                  <thead>
                    <tr>
                      {HEADERS.map(header => <th key={header}>{header}</th>)}
                    </tr>
                  </thead>
                  {eventList.map(this.renderEvent)}
                </Table>
              )}
            </PaginatedList>
          </Col>
        </Row>
      </React.Fragment>
    );
  }
}

export default Events;
