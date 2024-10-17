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
import moment from 'moment';
import 'moment-duration-format';
import get from 'lodash/get';
import styled, { css } from 'styled-components';

import { describeExpression } from 'util/CronUtils';
import { Button, Col, Row } from 'components/bootstrap';
import { Icon, Pluralize, Timestamp } from 'components/common';
import { EventDefinitionsActions } from 'stores/event-definitions/EventDefinitionsStore';

const DetailsList = styled.dl`
`;

const DetailTitle = styled.dt`
  float: left;
  clear: left;
`;

const DetailValue = styled.dd(({ theme }) => css`
  margin-left: 180px;
  word-wrap: break-word;

  &:not(:last-child) {
    border-bottom: 1px solid ${theme.colors.variant.lightest.default};
    margin-bottom: 5px;
    padding-bottom: 5px;
  }
`);

type EventDefinitionDescriptionProps = {
  definition: any;
  context?: any;
};

class EventDefinitionDescription extends React.Component<EventDefinitionDescriptionProps, {
  [key: string]: any;
}> {
  static defaultProps = {
    context: {},
  };

  static describeSchedule = (isCron, value) => {
    if (isCron) {
      const cronDescription = describeExpression(value);

      // Lower case the A in At or the E in Every
      return cronDescription.charAt(0).toLowerCase() + cronDescription.slice(1);
    }

    return `every ${moment.duration(value)
      .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all', usePlural: false })}`;
  };

  static renderSchedulingInformation = (definition) => {
    let schedulingInformation = 'Not scheduled.';

    if (definition.config.search_within_ms && (definition.config.use_cron_scheduling || definition.config.execute_every_ms)) {
      const executeEveryFormatted = EventDefinitionDescription.describeSchedule(definition.config.use_cron_scheduling, definition.config.use_cron_scheduling ? definition.config.cron_expression : definition.config.execute_every_ms);
      const searchWithinFormatted = moment.duration(definition.config.search_within_ms)
        .format('d [days] h [hours] m [minutes] s [seconds]', { trim: 'all' });

      schedulingInformation = `Runs ${executeEveryFormatted}, searching within the last ${searchWithinFormatted}.`;
    }

    return schedulingInformation;
  };

  static renderNotificationsInformation = (definition) => {
    let notificationsInformation = <span>Does <b>not</b> trigger any Notifications.</span>;

    if (definition.notifications.length > 0) {
      notificationsInformation = (
        <span>
          Triggers {definition.notifications.length}{' '}
          <Pluralize singular="Notification" plural="Notifications" value={definition.notifications.length} />.
        </span>
      );
    }

    return notificationsInformation;
  };

  static clearNotifications = (definition) => () => {
    // eslint-disable-next-line no-alert
    if (window.confirm(`Are you sure you want to clear queued notifications for "${definition.title}"?`)) {
      EventDefinitionsActions.clearNotificationQueue(definition);
    }
  };

  constructor(props) {
    super(props);

    this.state = {
      showDetails: false,
    };
  }

  renderDetails = (definition, context) => {
    const { showDetails } = this.state;

    if (!showDetails) {
      return null;
    }

    const scheduleCtx = get(context, `scheduler.${definition.id}`, null);

    if (!scheduleCtx.is_scheduled) {
      return (<p>Event definition is not scheduled, no details available.</p>);
    }

    let timerange = null;

    if (get(scheduleCtx, 'data.type', null) === 'event-processor-execution-v1') {
      const from = scheduleCtx.data.timerange_from;
      const to = scheduleCtx.data.timerange_to;

      timerange = (
        <>
          <DetailTitle>Next timerange:</DetailTitle>
          <DetailValue><Timestamp dateTime={from} /> <Icon name="arrow_circle_right" /> <Timestamp dateTime={to} /></DetailValue>
        </>
      );
    }

    return (
      <Row>
        <Col md={6}>
          <DetailsList>
            <DetailTitle>Status:</DetailTitle>
            <DetailValue>{scheduleCtx.status}</DetailValue>
            {scheduleCtx.triggered_at && (
              <>
                <DetailTitle>Last execution:</DetailTitle>
                <DetailValue><Timestamp dateTime={scheduleCtx.triggered_at} /></DetailValue>
              </>
            )}
            {scheduleCtx.next_time && (
              <>
                <DetailTitle>Next execution:</DetailTitle>
                <DetailValue><Timestamp dateTime={scheduleCtx.next_time} /></DetailValue>
              </>
            )}
            {timerange}
            <DetailTitle>Queued notifications:</DetailTitle>
            <DetailValue>{scheduleCtx.queued_notifications}
              {scheduleCtx.queued_notifications > 0 && (
                <Button bsStyle="link" bsSize="xsmall" onClick={EventDefinitionDescription.clearNotifications(definition)}>
                  clear
                </Button>
              )}
            </DetailValue>
          </DetailsList>
        </Col>
      </Row>
    );
  };

  handleDetailsToggle = () => {
    const { showDetails } = this.state;

    this.setState({ showDetails: !showDetails });
  };

  render() {
    const { definition, context } = this.props;
    const { showDetails } = this.state;

    return (
      <>
        <p>{definition.description}</p>
        <p>
          {EventDefinitionDescription.renderSchedulingInformation(definition)} {EventDefinitionDescription.renderNotificationsInformation(definition)}
          <Button bsStyle="link" bsSize="xsmall" onClick={this.handleDetailsToggle}>
            {showDetails ? 'Hide' : 'Show'} details
          </Button>
        </p>
        {this.renderDetails(definition, context)}
      </>
    );
  }
}

export default EventDefinitionDescription;
