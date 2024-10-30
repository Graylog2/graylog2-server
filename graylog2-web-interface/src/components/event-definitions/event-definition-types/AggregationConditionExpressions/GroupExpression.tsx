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
import styled from 'styled-components';

import { Clearfix } from 'components/bootstrap';
import { replaceBooleanExpressionOperatorInGroup } from 'logic/alerts/AggregationExpressionConfig';
import type { EventDefinition } from 'components/event-definitions/event-definitions-types';

import BooleanOperatorSelector from './BooleanOperatorSelector';

import AggregationConditionExpression from '../AggregationConditionExpression';

const Group = styled.div`
  padding-left: 40px;
`;

type GroupExpressionProps = {
  eventDefinition: EventDefinition;
  formattedFields: any[];
  aggregationFunctions: any[];
  expression: any;
  level: number;
  onChange: (...args: any[]) => void;
  onChildChange: (...args: any[]) => () => void;
  validation?: any;
  parent?: any;
};

const GroupExpression = ({
  expression,
  level,
  onChange,
  onChildChange,
  validation = {},
  ...props
}: GroupExpressionProps) => {
  const handleOperatorChange = (nextOperator) => {
    const nextExpression = cloneDeep(expression);

    nextExpression.operator = nextOperator;
    nextExpression.child = replaceBooleanExpressionOperatorInGroup(nextOperator, nextExpression.child);
    onChange({ conditions: nextExpression });
  };

  return (
    <>
      <BooleanOperatorSelector operator={expression.operator} onOperatorChange={handleOperatorChange} />
      <Clearfix />
      <Group>
        <AggregationConditionExpression {...props}
                                        expression={expression.child}
                                        validation={validation.child}
                                        parent={expression}
                                        onChange={onChildChange('child')}
                                        level={level + 1} />
      </Group>
    </>
  );
};

export default GroupExpression;
