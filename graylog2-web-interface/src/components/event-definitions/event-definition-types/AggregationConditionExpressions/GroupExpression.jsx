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
import PropTypes from 'prop-types';
import { cloneDeep } from 'lodash';
import styled from 'styled-components';

import { Clearfix } from 'components/graylog';
import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';
import { replaceBooleanExpressionOperatorInGroup } from 'logic/alerts/AggregationExpressionConfig';

// eslint-disable-next-line import/no-cycle
import BooleanOperatorSelector from './BooleanOperatorSelector';

import AggregationConditionExpression from '../AggregationConditionExpression';

const Group = styled.div`
  padding-left: 40px;
`;

const GroupExpression = (props) => {
  const { expression, level, onChange, onChildChange, validation } = props;

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

GroupExpression.propTypes = {
  expression: internalNodePropType.isRequired,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
  validation: PropTypes.object,
};

GroupExpression.defaultProps = {
  validation: {},
};

export default GroupExpression;
