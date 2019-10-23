import React from 'react';
import PropTypes from 'prop-types';

import { Clearfix, Col } from 'components/graylog';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const BooleanExpression = (props) => {
  const { expression, onChildChange } = props;

  return (
    <>
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      onChange={onChildChange('left')} />
      <Clearfix />
      <Col md={1}>
        <p>{expression.expr === '&&' ? 'AND' : 'OR'}</p>
      </Col>
      <AggregationConditionExpression {...props}
                                      expression={expression.right}
                                      onChange={onChildChange('right')} />
    </>
  );
};

BooleanExpression.propTypes = {
  expression: PropTypes.shape({
    expr: PropTypes.string,
    left: PropTypes.object,
    right: PropTypes.object,
  }).isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
};

export default BooleanExpression;
