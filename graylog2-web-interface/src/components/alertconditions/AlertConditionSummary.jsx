import React from 'react';
import { Col } from 'react-bootstrap';

import { EntityListItem } from 'components/common';

import { GenericAlertConditionSummary } from 'components/alertconditions';
import AlertConditionsFactory from 'logic/alertconditions/AlertConditionsFactory';

const AlertConditionSummary = React.createClass({
  propTypes: {
    alertCondition: React.PropTypes.object.isRequired,
    typeDefinition: React.PropTypes.object.isRequired,
    stream: React.PropTypes.object,
    actions: React.PropTypes.array.isRequired,
  },

  alertConditionsFactory: new AlertConditionsFactory(),

  render() {
    const stream = this.props.stream;
    const condition = this.props.alertCondition;
    const typeDefinition = this.props.typeDefinition;
    const conditionTypes = this.alertConditionsFactory.get(condition.type);
    const conditionType = conditionTypes && conditionTypes.length > 0 && conditionTypes[0];
    const SummaryComponent = conditionType.summary || GenericAlertConditionSummary;

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
