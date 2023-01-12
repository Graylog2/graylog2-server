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
import React, { useMemo } from 'react';
import lodash from 'lodash';

import usePluginEntities from 'hooks/usePluginEntities';
import { Col, Row } from 'components/bootstrap';
import { Timestamp } from 'components/common';
import EventDefinitionPriorityEnum from 'logic/alerts/EventDefinitionPriorityEnum';
import type { Event, EventDefinitionContext } from 'components/events/events/types';
import ReplaySearchButton from 'views/components/widgets/ReplaySearchButton';
import EventFields from 'components/events/events/EventFields';
import EventDefinitionLink from 'components/event-definitions/event-definitions/EventDefinitionLink';
import type { AbsoluteTimeRange } from 'views/logic/queries/Query';

type Props = {
  event: Event,
  eventDefinitionContext: EventDefinitionContext,
};

const EventDetails = ({ event, eventDefinitionContext }: Props) => {
  const eventDefinitionTypes = usePluginEntities('eventDefinitionTypes');
  const timeRange: AbsoluteTimeRange = event.replay_info && { type: 'absolute', from: `${event.replay_info.timerange_start}`, to: `${event.replay_info.timerange_end}` };

  const plugin = useMemo(() => {
    if (event.event_definition_type === undefined) {
      return null;
    }

    return eventDefinitionTypes.find((edt) => edt.type === event.event_definition_type);
  }, [event, eventDefinitionTypes]);

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
            <EventDefinitionLink event={event} eventDefinitionContext={eventDefinitionContext} />
            &emsp;
            ({(plugin && plugin.displayName) || event.event_definition_type})
          </dd>
          {event.replay_info && (
            <>
              <dt>Actions</dt>
              <dd>
                <ReplaySearchButton queryString={event.replay_info.query}
                                    timerange={timeRange}
                                    streams={event.replay_info.streams}>
                  Replay search
                </ReplaySearchButton>
              </dd>
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
            : <EventFields fields={event.fields} />}
          <dt>Group-By Fields</dt>
          {lodash.isEmpty(event.group_by_fields)
            ? <dd>No group-by fields on this Event.</dd>
            : <EventFields fields={event.group_by_fields} />}
        </dl>
      </Col>
    </Row>
  );
};

export default EventDetails;
