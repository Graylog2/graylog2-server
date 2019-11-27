import React from 'react';
import PropTypes from 'prop-types';

import { Row } from 'components/graylog';
import { emptyComparisonExpressionConfig, enrichExpressionTree, cleanExpressionTree } from 'logic/alerts/AggregationExpressionConfig';

import AggregationConditionExpression from './AggregationConditionExpression';

import commonStyles from '../common/commonStyles.css';

const initialEmptyConditionConfig = emptyComparisonExpressionConfig();

const extractSeriesReferences = (expression, acc = []) => {
  if (expression.expr === 'number-ref') {
    acc.push(expression.ref);
  }
  if (expression.left && expression.right) {
    return extractSeriesReferences(expression.left).concat(extractSeriesReferences(expression.right));
  }
  return acc;
};

class AggregationConditionsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  constructor(props) {
    super(props);
    const { eventDefinition } = this.props;
    this.state = {
      groupNodes: [],
      expression: enrichExpressionTree(eventDefinition.config.conditions.expression || initialEmptyConditionConfig),
    };
  }

  handleChange = (key, update) => {
    const { eventDefinition, onChange } = this.props;
    if (key === 'groups') {
      const { groupNodes } = this.state;
      const nextGroupNodes = groupNodes.concat(update);
      this.setState({ groupNodes: nextGroupNodes });
      return;
    }

    if (key === 'conditions') {
      // Propagate empty comparison expression, if the last expression was removed
      const nextConditions = update || emptyComparisonExpressionConfig();

      // Keep series up-to-date with changes in conditions
      const seriesReferences = extractSeriesReferences(nextConditions);
      const nextSeries = eventDefinition.config.series.filter(s => seriesReferences.includes(s.id));

      // Keep enriched expression tree with existing IDs, propagate cleaned up tree
      this.setState({ expression: enrichExpressionTree(nextConditions) });
      onChange({
        conditions: { expression: cleanExpressionTree(nextConditions) },
        series: nextSeries,
      });
      return;
    }

    onChange({ [key]: update });
  };

  render() {
    const { expression, groupNodes } = this.state;

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>

        <Row>
          <AggregationConditionExpression expression={expression}
                                          groupNodes={groupNodes}
                                          {...this.props}
                                          onChange={this.handleChange} />
        </Row>
      </React.Fragment>
    );
  }
}

export default AggregationConditionsForm;
