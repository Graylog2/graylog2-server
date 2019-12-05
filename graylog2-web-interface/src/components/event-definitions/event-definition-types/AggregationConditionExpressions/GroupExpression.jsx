import React from 'react';
import PropTypes from 'prop-types';
import { cloneDeep } from 'lodash';

import { Clearfix } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';
// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';
import BooleanOperatorSelector from './BooleanOperatorSelector';

const GroupExpression = (props) => {
  const { expression, level, onChange, onChildChange } = props;

  const handleOperatorChange = (nextOperator) => {
    const nextExpression = cloneDeep(expression);
    nextExpression.operator = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <>
      <BooleanOperatorSelector operator={expression.operator} onOperatorChange={handleOperatorChange} />
      <Clearfix />
      <AggregationConditionExpression {...props}
                                      expression={expression.child}
                                      parent={expression}
                                      onChange={onChildChange('child')}
                                      level={level + 1} />
    </>
  );
};

GroupExpression.propTypes = {
  expression: internalNodePropType.isRequired,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
  validation: PropTypes.object.isRequired,
};

export default GroupExpression;
