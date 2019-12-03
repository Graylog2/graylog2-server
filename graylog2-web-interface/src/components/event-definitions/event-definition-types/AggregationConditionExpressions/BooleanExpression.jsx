import React from 'react';
import PropTypes from 'prop-types';

import { Clearfix } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const BooleanExpression = (props) => {
  const { expression, level, onChildChange } = props;

  return (
    <>
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      parent={expression}
                                      onChange={onChildChange('left')}
                                      level={level + 1} />
      <Clearfix />
      <AggregationConditionExpression {...props}
                                      expression={expression.right}
                                      parent={expression}
                                      onChange={onChildChange('right')}
                                      level={level + 1}
                                      renderLabel={false} />
    </>
  );
};

BooleanExpression.propTypes = {
  expression: internalNodePropType.isRequired,
  groupNodes: PropTypes.array.isRequired,
  parent: internalNodePropType,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
};

BooleanExpression.defaultProps = {
  parent: undefined,
};

export default BooleanExpression;
