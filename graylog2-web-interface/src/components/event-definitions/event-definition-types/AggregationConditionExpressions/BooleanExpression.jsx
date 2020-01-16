import React from 'react';
import PropTypes from 'prop-types';

import { Clearfix } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const BooleanExpression = (props) => {
  const { expression, level, onChildChange, validation } = props;

  return (
    <>
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      validation={validation.left}
                                      parent={expression}
                                      onChange={onChildChange('left')}
                                      level={level + 1} />
      <Clearfix />
      <AggregationConditionExpression {...props}
                                      expression={expression.right}
                                      validation={validation.right}
                                      parent={expression}
                                      onChange={onChildChange('right')}
                                      level={level + 1}
                                      renderLabel={false} />
    </>
  );
};

BooleanExpression.propTypes = {
  expression: internalNodePropType.isRequired,
  parent: internalNodePropType,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
  validation: PropTypes.object,
};

BooleanExpression.defaultProps = {
  parent: undefined,
  validation: {},
};

export default BooleanExpression;
