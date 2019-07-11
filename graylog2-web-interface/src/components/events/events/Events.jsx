import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';
import { Col, Label, OverlayTrigger, Row, Tooltip } from 'react-bootstrap';
import { Link } from 'react-router';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { DataTable, PaginatedList, SearchForm, Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import DateTime from 'logic/datetimes/DateTime';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

import styles from './Events.css';

class Events extends React.Component {
  static propTypes = {
    events: PropTypes.array.isRequired,
    parameters: PropTypes.object.isRequired,
    totalEvents: PropTypes.number.isRequired,
    context: PropTypes.object.isRequired,
    onPageChange: PropTypes.func.isRequired,
    onQueryChange: PropTypes.func.isRequired,
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }
    return PluginStore.exports('eventDefinitionTypes').find(edt => edt.type === type);
  };

  eventDefinitionFormatter = (event, eventDefinitionContext) => {
    const plugin = this.getConditionPlugin(event.event_definition_type);
    return (
      <React.Fragment>
        <Link to={Routes.NEXT_ALERTS.DEFINITIONS.show(eventDefinitionContext.id)}>
          {eventDefinitionContext.title}
        </Link>
        &emsp;
        ({plugin.displayName || event.event_definition_type})
      </React.Fragment>
    );
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

  eventsFormatter = (event) => {
    const { context } = this.props;
    return (
      <tr key={event.id} className={event.priority === EventDefinitionPriorityEnum.HIGH && 'bg-danger'}>
        <td>
          {this.priorityFormatter(event.id, event.priority)}
          &nbsp;
          {event.message}
        </td>
        <td>{event.alert ? <Label bsStyle="warning">Alert</Label> : <Label bsStyle="info">Event</Label>}</td>
        <td>{this.eventDefinitionFormatter(event, context.event_definitions[event.event_definition_id])}</td>
        <td><Timestamp dateTime={event.timestamp} format={DateTime.Formats.DATETIME} /></td>
      </tr>
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
            <DataTable id="events-table"
                       className="table-hover"
                       headers={['Information', 'Type', 'Event Definition', 'Timestamp']}
                       rows={eventList}
                       dataRowFormatter={this.eventsFormatter}
                       filterKeys={[]} />
          </PaginatedList>
        </Col>
      </Row>
    );
  }
}

export default Events;
