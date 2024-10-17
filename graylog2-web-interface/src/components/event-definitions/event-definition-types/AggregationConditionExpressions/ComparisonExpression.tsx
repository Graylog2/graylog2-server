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
import cloneDeep from 'lodash/cloneDeep';

import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/bootstrap';
import { Select } from 'components/common';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

import AggregationConditionExpression from '../AggregationConditionExpression';

type ComparisonExpressionProps = {
  eventDefinition: EventDefinition;
  formattedFields: any[];
  aggregationFunctions: any[];
  expression: any;
  level: number;
  onChange: (...args: any[]) => void;
  onChildChange: (...args: any[]) => (...args: any[]) => void;
  renderLabel: boolean;
  validation?: any;
  parent?: any;
};

const ComparisonExpression = ({
  expression,
  level,
  onChildChange,
  onChange,
  renderLabel,
  validation = {},
  ...props
}: ComparisonExpressionProps) => {
  const handleExpressionOperatorChange = (nextOperator) => {
    const nextExpression = cloneDeep(expression);

    nextExpression.expr = nextOperator;
    onChange({ conditions: nextExpression });
  };

  return (
    <Col md={10}>
      <Row className="row-sm">
        <AggregationConditionExpression {...props}
                                        expression={expression.left}
                                        validation={validation.left}
                                        parent={expression}
                                        onChange={onChildChange('left')}
                                        level={level + 1} />

        <Col md={3}>
          <FormGroup controlId="aggregation-condition" validationState={validation.message ? 'error' : null}>
            {renderLabel && <ControlLabel>Is</ControlLabel>}
            <Select id="aggregation-condition"
                    matchProp="label"
                    placeholder="Select Condition"
                    onChange={handleExpressionOperatorChange}
                    options={[
                      { label: '<', value: '<' },
                      { label: '<=', value: '<=' },
                      { label: '>', value: '>' },
                      { label: '>=', value: '>=' },
                      { label: '=', value: '==' },
                    ]}
                    value={expression.expr}
                    clearable={false} />
            {validation.message && <HelpBlock>{validation.message}</HelpBlock>}
          </FormGroup>
        </Col>
        <AggregationConditionExpression {...props}
                                        expression={expression.right}
                                        validation={validation.right}
                                        parent={expression}
                                        onChange={onChildChange('right')}
                                        level={level + 1} />
      </Row>
    </Col>
  );
};

export default ComparisonExpression;
