import React from 'react';
import { Col } from 'react-bootstrap';

import { EntityListItem } from 'components/common';

import { GenericAlertConditionSummary } from 'components/alertconditions';
import { PluginStore } from 'graylog-web-plugin/plugin';

const AlertConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    typeDefinition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
    actions: React.PropTypes.array.isRequired,
  },

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

    return (
      <EntityListItem key={`entry-list-${condition.id}`}
                      title={condition.title ? condition.title : 'Untitled'}
                      titleSuffix={`(${typeDefinition.name})`}
                      description={description}
                      actions={this.props.actions}
                      contentRow={content} />
    );
  },
});

export default AlertConditionSummary;
