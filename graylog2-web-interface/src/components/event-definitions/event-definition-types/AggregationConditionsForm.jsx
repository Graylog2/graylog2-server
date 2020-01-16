import React from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { get } from 'lodash';

import { Alert, Button, Row, Well } from 'components/graylog';
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

const StyledPanel = styled(Panel)`
  margin-top: 10px;
`;

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

    const nextConditions = changes.conditions;

    let nextSeries;
    if (nextConditions) {
      // Keep series up-to-date with changes in conditions
      const seriesReferences = extractSeriesReferences(nextConditions);
      nextSeries = (changes.series || eventDefinition.config.series).filter(s => seriesReferences.includes(s.id));
    } else {
      nextSeries = [];
    }

    onChange(Object.assign({}, changes, {
      conditions: { expression: nextConditions },
      series: nextSeries,
    }));
  };

  render() {
    const { showConditionSummary } = this.state;
    const { eventDefinition, validation } = this.props;
    const expression = eventDefinition.config.conditions.expression || initialEmptyConditionConfig;

    return (
      <React.Fragment>
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
                                          validation={{}}
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
