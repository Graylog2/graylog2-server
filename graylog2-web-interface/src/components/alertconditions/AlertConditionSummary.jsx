import PropTypes from 'prop-types';
import React from 'react';
import { Col } from 'react-bootstrap';
import { Link } from 'react-router';

import Routes from 'routing/Routes';

import { EntityListItem } from 'components/common';

import { GenericAlertConditionSummary } from 'components/alertconditions';
import { PluginStore } from 'graylog-web-plugin/plugin';

class AlertConditionSummary extends React.Component {
  static propTypes = {
    alertCondition: PropTypes.object.isRequired,
    typeDefinition: PropTypes.object.isRequired,
    stream: PropTypes.object,
    actions: PropTypes.array.isRequired,
    linkToDetails: PropTypes.bool,
  };

  render() {
    const stream = this.props.stream;
    const condition = this.props.alertCondition;
    const typeDefinition = this.props.typeDefinition;
    const conditionType = PluginStore.exports('alertConditions').find(c => c.type === condition.type) || {};
    const SummaryComponent = conditionType.summaryComponent || GenericAlertConditionSummary;

    const description = (stream ?
      <span>Alerting on stream <em>{stream.title}</em></span> : 'Not alerting on any stream');

    const content = (
      <Col md={12}>
        <strong>Configuration:</strong> <SummaryComponent alertCondition={condition} />
      </Col>
    );

    let title;
    if (this.props.linkToDetails) {
      title = (
        <Link to={Routes.show_alert_condition(stream.id, condition.id)}>
          {condition.title ? condition.title : 'Untitled'}
        </Link>
      );
    } else {
      title = (condition.title ? condition.title : 'Untitled');
    }

    return (
      <EntityListItem key={`entry-list-${condition.id}`}
                      title={title}
                      titleSuffix={`(${typeDefinition.name})`}
                      description={description}
                      actions={this.props.actions}
                      contentRow={content} />
    );
  }
}

export default AlertConditionSummary;
