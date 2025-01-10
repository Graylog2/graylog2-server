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
import styled from 'styled-components';
import get from 'lodash/get';

import { Alert, Row } from 'components/bootstrap';
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

type AggregationConditionsFormProps = {
  eventDefinition: any;
  validation: any;
  formattedFields: any[];
  aggregationFunctions: any[];
  onChange: (...args: any[]) => void;
};

class AggregationConditionsForm extends React.Component<AggregationConditionsFormProps, {
  [key: string]: any;
}> {
  constructor(props) {
    super(props);

    this.state = {
      showInlineValidation: false,
    };
  }

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

      nextSeries = (changes.series || eventDefinition?.config?.series)?.filter((s) => seriesReferences.includes(s.id)) || [];
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
    const expression = eventDefinition?.config?.conditions?.expression || initialEmptyConditionConfig;
    const expressionValidation = validateExpression(expression, eventDefinition?.config?.series || []);

    return (
      <>
        <h3 className={commonStyles.title}>Create Events for Definition</h3>
        {validation.errors.conditions && (
          <StyledAlert bsStyle="danger" title="Errors found">
            <p>{get(validation, 'errors.conditions[0]')}</p>
          </StyledAlert>
        )}

        <Row>
          <AggregationConditionExpression expression={expression}
                                          {...this.props}
                                          validation={showInlineValidation ? expressionValidation.validationTree : {}}
                                          onChange={this.handleChange} />
        </Row>

        <AggregationConditionsFormSummary conditions={eventDefinition?.config?.conditions || {}}
                                          series={eventDefinition?.config?.series || []}
                                          expressionValidation={expressionValidation}
                                          showInlineValidation={showInlineValidation}
                                          toggleShowValidation={this.toggleShowInlineValidation} />
      </>
    );
  }
}

export default AggregationConditionsForm;
