import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Alert, Col, Label, OverlayTrigger, Row, Table, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { PaginatedList, SearchForm, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

import styles from './Events.css';

const HEADERS = ['Information', 'Type', 'Event Definition', 'Timestamp'];

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    context: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
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

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find(edt => edt.type === type);
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
      <dl>
        {fieldNames.map((fieldName) => {
          return (
            <React.Fragment key={fieldName}>
              <dt>{fieldName}</dt>
              <dd>{eventFields[fieldName]}</dd>
            </React.Fragment>
          );
        })}
      </dl>
    );
  };

  renderEventDetails = (event, eventDefinitionContext) => {
    const plugin = this.getConditionPlugin(event.event_definition_type);

    return (
      <tr className={styles.expandedRow}>
        <td colSpan={HEADERS.length + 1}>
          <Row>
            <Col md={6}>
              <dl>
                <dt>ID</dt>
                <dd>{event.id}</dd>
                <dt>Priority</dt>
                <dd>
                  {lodash.capitalize(EventDefinitionPriorityEnum.properties[event.priority].name)}
                </dd>
                <dt>Timestamp</dt>
                <dd>
                  <Timestamp dateTime={event.timestamp} />
                </dd>
                <dt>Event Definition</dt>
                <dd>
                  <Link to={Routes.NEXT_ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>
                    {eventDefinitionContext.title}
                  </Link>
                  &emsp;
                  ({plugin.displayName || event.event_definition_type})
                </dd>
              </dl>
            </Col>
            <Col md={6}>
              <dl>
                {event.timerange_start && event.timerange_end && (
                  <React.Fragment>
                    <dt>Aggregation time range</dt>
                    <dd>
                      <Timestamp dateTime={event.timerange_start} />
                      &ensp;&mdash;&ensp;
                      <Timestamp dateTime={event.timerange_end} />
                    </dd>
                  </React.Fragment>
                )}
                <dt>Event Key</dt>
                <dd>{event.key || 'No Key set for this Event.'}</dd>
                <dt>Additional Fields</dt>
                {lodash.isEmpty(event.fields)
                  ? <dd>No additional Fields added to this Event.</dd>
                  : this.renderEventFields(event.fields)}
              </dl>
            </Col>
          </Row>
        </td>
      </tr>
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
          <td>{event.alert ? <Label bsStyle="warning">Alert</Label> : <Label bsStyle="info">Event</Label>}</td>
          <td>
            <Link to={Routes.NEXT_ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>
              {eventDefinitionContext.title}
            </Link>
          </td>
          <td><Timestamp dateTime={event.timestamp} format={DateTime.Formats.DATETIME} /></td>
        </tr>
        {expanded.includes(event.id) && this.renderEventDetails(event, eventDefinitionContext)}
      </tbody>
    );
  };

  render() {
    const { events, parameters, totalEvents, onPageChange, onQueryChange } = this.props;

    const eventList = events.map(e => e.event);
    return (
      <Row>
        <Col md={12}>
          <SearchForm query={parameters.query}
                      onSearch={onQueryChange}
                      onReset={onQueryChange}
                      searchButtonLabel="Find"
                      placeholder="Find Events"
                      wrapperClass={styles.inline}
                      queryWidth={200}
                      topMargin={0}
                      useLoadingState />

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
    );
  }
}

export default Events;
