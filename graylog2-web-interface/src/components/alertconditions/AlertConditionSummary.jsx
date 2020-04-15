import PropTypes from 'prop-types';
import React from 'react';
import { Link } from 'react-router';

import Routes from 'routing/Routes';
import { Col } from 'components/graylog';
import { EntityListItem } from 'components/common';
import { GenericAlertConditionSummary } from 'components/alertconditions';
import { PluginStore } from 'graylog-web-plugin/plugin';

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
