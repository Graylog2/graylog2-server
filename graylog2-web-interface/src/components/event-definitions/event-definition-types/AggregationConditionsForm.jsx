import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { get } from 'lodash';

import { Alert, Row } from 'components/graylog';
import { Icon } from 'components/common';
import { emptyComparisonExpressionConfig } from 'logic/alerts/AggregationExpressionConfig';
import validateExpression from 'logic/alerts/AggregationExpressionValidation';

import AggregationConditionExpression from './AggregationConditionExpression';
import AggregationConditionsFormSummary from './AggregationConditionsFormSummary';

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

const StyledAlert = styled(Alert)`
  margin-bottom: 10px !important;
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
    showInlineValidation: false,
  };

  toggleShowInlineValidation = () => {
    const { showInlineValidation } = this.state;
    this.setState({ showInlineValidation: !showInlineValidation });
  };

  handleChange = (changes) => {
    const { eventDefinition, onChange } = this.props;

    if (!Object.keys(changes).includes('conditions')) {
      onChange(changes);
      return;
    }

    const nextConditions = changes.conditions;

    let nextSeries;
    if (nextConditions) {
      // Keep series up-to-date with changes in conditions
      const seriesReferences = extractSeriesReferences(nextConditions);
      nextSeries = (changes.series || eventDefinition.config.series).filter((s) => seriesReferences.includes(s.id));
    } else {
      nextSeries = [];
    }

    onChange({
      ...changes,
      conditions: { expression: nextConditions },
      series: nextSeries,
    });
  };

  render() {
    const { showInlineValidation } = this.state;
    const { eventDefinition, validation } = this.props;
    const expression = eventDefinition.config.conditions.expression || initialEmptyConditionConfig;
    const expressionValidation = validateExpression(expression, eventDefinition.config.series);

    return (
      <>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>
        {validation.errors.conditions && (
          <StyledAlert bsStyle="danger">
            <h4><Icon name="warning" />&nbsp;Errors found</h4>
            <p>{get(validation, 'errors.conditions[0]')}</p>
          </StyledAlert>
        )}

        <Row>
          <AggregationConditionExpression expression={expression}
                                          {...this.props}
                                          validation={showInlineValidation ? expressionValidation.validationTree : {}}
                                          onChange={this.handleChange} />
        </Row>

        <AggregationConditionsFormSummary conditions={eventDefinition.config.conditions}
                                          series={eventDefinition.config.series}
                                          expressionValidation={expressionValidation}
                                          showInlineValidation={showInlineValidation}
                                          toggleShowValidation={this.toggleShowInlineValidation} />
      </>
    );
  }
}

export default AggregationConditionsForm;
