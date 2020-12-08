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
import PropTypes from 'prop-types';
import React from 'react';
import { PluginStore } from 'graylog-web-plugin/plugin';

import { Link } from 'components/graylog/router';
import Routes from 'routing/Routes';
import { Col } from 'components/graylog';
import { EntityListItem } from 'components/common';
import { GenericAlertConditionSummary } from 'components/alertconditions';

class AlertConditionSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
    conditionType: PropTypes.object.isRequired,
    stream: PropTypes.object,
    actions: PropTypes.array.isRequired,
    isDetailsView: PropTypes.bool,
  };

  static defaultProps = {
    stream: undefined,
    isDetailsView: false,
  };

  render() {
    const { stream } = this.props;
    const condition = this.props.alertCondition;
    const { conditionType } = this.props;
    const conditionPlugin = PluginStore.exports('alertConditions').find((c) => c.type === condition.type) || {};
    const SummaryComponent = conditionPlugin.summaryComponent || GenericAlertConditionSummary;

    const description = (stream
      ? <span>Alerting on stream <em>{stream.title}</em></span> : 'Not alerting on any stream');

    const content = (
      <Col md={12}>
        <strong>Configuration:</strong> <SummaryComponent alertCondition={condition} />
      </Col>
    );

    let title;

    if (this.props.isDetailsView) {
      title = (condition.title ? condition.title : 'Untitled');
    } else {
      title = (
        <Link to={Routes.show_alert_condition(stream.id, condition.id)}>
          {condition.title ? condition.title : 'Untitled'}
        </Link>
      );
    }

    return (
      <EntityListItem key={`entry-list-${condition.id}`}
                      title={title}
                      titleSuffix={`(${conditionType.name})`}
                      description={description}
                      actions={this.props.actions}
                      contentRow={content} />
    );
  }
}

export default AlertConditionSummary;
