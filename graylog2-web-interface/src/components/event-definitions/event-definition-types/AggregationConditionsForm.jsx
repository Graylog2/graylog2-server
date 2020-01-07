import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Button, Row, Well } from 'components/graylog';
import { Icon } from 'components/common';
import { emptyComparisonExpressionConfig } from 'logic/alerts/AggregationExpressionConfig';

import AggregationConditionExpression from './AggregationConditionExpression';
import AggregationConditionSummary from './AggregationConditionSummary';

import commonStyles from '../common/commonStyles.css';

const initialEmptyConditionConfig = emptyComparisonExpressionConfig();

const extractSeriesReferences = (expression, acc = []) => {
  if (expression.expr === 'number-ref') {
    acc.push(expression.ref);
  }
  if (expression.left && expression.right) {
    return extractSeriesReferences(expression.left).concat(extractSeriesReferences(expression.right));
  }
  if (expression.child) {
    return extractSeriesReferences(expression.child);
  }
  return acc;
};

const StyledWell = styled(Well)`
  margin-top: 10px;
`;

class AggregationConditionsForm extends React.Component {
  static propTypes = {
    eventDefinition: PropTypes.object.isRequired,
    validation: PropTypes.object.isRequired,
    formattedFields: PropTypes.array.isRequired,
    aggregationFunctions: PropTypes.array.isRequired,
    onChange: PropTypes.func.isRequired,
  };

  state = {
    showConditionSummary: false,
  };

  toggleShowConditionSummary = () => {
    const { showConditionSummary } = this.state;
    this.setState({ showConditionSummary: !showConditionSummary });
  };

  handleChange = (changes) => {
    const { eventDefinition, onChange } = this.props;

    if (!Object.keys(changes).includes('conditions')) {
      onChange(changes);
      return;
    }

    // Propagate empty comparison expression, if the last expression was removed
    const nextConditions = changes.conditions || emptyComparisonExpressionConfig();

    // Keep series up-to-date with changes in conditions
    const seriesReferences = extractSeriesReferences(nextConditions);
    const nextSeries = (changes.series || eventDefinition.config.series).filter(s => seriesReferences.includes(s.id));

    onChange(Object.assign({}, changes, {
      conditions: { expression: nextConditions },
      series: nextSeries,
    }));
  };

  render() {
    const { showConditionSummary } = this.state;
    const { eventDefinition } = this.props;
    const expression = eventDefinition.config.conditions.expression || initialEmptyConditionConfig;

    return (
      <React.Fragment>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>

        <Row>
          <AggregationConditionExpression expression={expression}
                                          {...this.props}
                                          onChange={this.handleChange} />
        </Row>


        <Button bsSize="small" bsStyle="link" className="btn-text" onClick={this.toggleShowConditionSummary}>
          <Icon name={showConditionSummary ? 'caret-down' : 'caret-right'} />
          &nbsp;{showConditionSummary ? 'Hide' : 'Show'} condition preview
        </Button>
        {showConditionSummary && (
          <StyledWell bsSize="small">
            <AggregationConditionSummary series={eventDefinition.config.series} conditions={eventDefinition.config.conditions} />
          </StyledWell>
        )}
      </React.Fragment>
    );
  }
}

export default AggregationConditionsForm;
