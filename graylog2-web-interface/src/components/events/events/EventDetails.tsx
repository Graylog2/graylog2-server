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
import lodash from 'lodash';

import useCurrentUser from 'hooks/useCurrentUser';
import usePluginEntities from 'hooks/usePluginEntities';
import { isPermitted } from 'util/PermissionsMixin';
import { Link } from 'components/common/router';
import { Col, Row } from 'components/bootstrap';
import { Timestamp } from 'components/common';
import Routes from 'routing/Routes';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import type { Event, EventDefinitionContext } from 'components/events/events/types';
import ReplaySearchButton from 'views/components/widgets/ReplaySearchButton';

type Props = {
  event: Event,
  eventDefinitionContext: EventDefinitionContext,
};

const EventDetails = ({ event, eventDefinitionContext }: Props) => {
  const eventDefinitionTypes = usePluginEntities('eventDefinitionTypes');

  const getConditionPlugin = (type: string): EventDefinitionType => {
    if (type === undefined) {
      return null;
    }

    return eventDefinitionTypes.find((edt) => edt.type === type);
  };

  const renderEventFields = (eventFields: Object[]) => {
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

  const currentUser = useCurrentUser();
  const plugin = getConditionPlugin(event.event_definition_type);

  const renderLinkToEventDefinition = () => {
    if (!eventDefinitionContext) {
      return <em>{event.event_definition_id}</em>;
    }

    return isPermitted(currentUser.permissions,
      `eventdefinitions:edit:${eventDefinitionContext.id}`)
      ? <Link to={Routes.ALERTS.DEFINITIONS.edit(eventDefinitionContext.id)}>{eventDefinitionContext.title}</Link>
      : eventDefinitionContext.title;
  };

  const renderReplaySearchLink = () => {
    const replayInfo = event.replay_info;
    let range;
    let streams;

    if (replayInfo.timerange_start && replayInfo.timerange_end) {
      range = { type: 'absolute', from: `${replayInfo.timerange_start}`, to: `${replayInfo.timerange_end}` };
    }

    if (replayInfo.streams) {
      streams = replayInfo.streams;
    }

    return <ReplaySearchButton query={replayInfo.query} timerange={range} streams={streams} />;
  };

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
            {renderLinkToEventDefinition()}
            &emsp;
            ({(plugin && plugin.displayName) || event.event_definition_type})
          </dd>
          {event.replay_info && (
            <>
              <dt>Actions</dt>
              <dd>{renderReplaySearchLink()}</dd>
            </>
          )}
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
            : renderEventFields(event.fields)}
          <dt>Group-By Fields</dt>
          {lodash.isEmpty(event.group_by_fields)
            ? <dd>No group-by fields on this Event.</dd>
            : renderEventFields(event.group_by_fields)}
        </dl>
      </Col>
    </Row>
  );
};

export default EventDetails;
