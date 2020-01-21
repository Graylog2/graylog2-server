import React from 'react';
import PropTypes from 'prop-types';
import { cloneDeep } from 'lodash';
import styled from 'styled-components';

import { Clearfix } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';
import { replaceBooleanExpressionOperatorInGroup } from 'logic/alerts/AggregationExpressionConfig';
// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';
import BooleanOperatorSelector from './BooleanOperatorSelector';

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
