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
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Link } from 'components/graylog/router';
import { Col, Row } from 'components/graylog';
import { Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import PermissionsMixin from 'util/PermissionsMixin';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';

class EventDetails extends React.Component {
  static propTypes = {
    event: PropTypes.object.isRequired,
    eventDefinitionContext: PropTypes.object.isRequired,
    currentUser: PropTypes.object.isRequired,
  };

  getConditionPlugin = (type) => {
    if (type === undefined) {
      return {};
    }

    return PluginStore.exports('eventDefinitionTypes').find((edt) => edt.type === type) || {};
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

  render() {
    const { event, eventDefinitionContext } = this.props;
    const plugin = this.getConditionPlugin(event.event_definition_type);

    return (
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
              {this.renderLinkToEventDefinition(event, eventDefinitionContext)}
              &emsp;
              ({plugin.displayName || event.event_definition_type})
            </dd>
          </dl>
        </Col>
        <Col md={6}>
          <dl>
            {event.timerange_start && event.timerange_end && (
              <>
                <dt>Aggregation time range</dt>
                <dd>
                  <Timestamp dateTime={event.timerange_start} />
                  &ensp;&mdash;&ensp;
                  <Timestamp dateTime={event.timerange_end} />
                </dd>
              </>
            )}
            <dt>Event Key</dt>
            <dd>{event.key || 'No Key set for this Event.'}</dd>
            <dt>Additional Fields</dt>
            {lodash.isEmpty(event.fields)
              ? <dd>No additional Fields added to this Event.</dd>
              : this.renderEventFields(event.fields)}
            <dt>Group-By Fields</dt>
            {lodash.isEmpty(event.group_by_fields)
              ? <dd>No group-by fields on this Event.</dd>
              : this.renderEventFields(event.group_by_fields)}
          </dl>
        </Col>
      </Row>
    );
  }
}

export default EventDetails;
