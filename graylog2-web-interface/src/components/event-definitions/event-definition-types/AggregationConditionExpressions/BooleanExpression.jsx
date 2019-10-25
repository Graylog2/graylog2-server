import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Select } from 'components/common';
import { Clearfix, Col, FormGroup } from 'components/graylog';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const BooleanExpression = (props) => {
  const { expression, level, onChildChange, onChange } = props;

  const handleOperatorChange = (nextOperator) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.expr = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <>
      {/* Render an empty column in the first condition, to align it with others. */}
      {level === 0 && <Col md={1} />}
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      onChange={onChildChange('left')}
                                      level={level + 1} />
      <Clearfix />
      <Col md={1}>
        <FormGroup controlId="boolean-operator">
          <Select id="boolean-operator"
                  matchProp="label"
                  onChange={handleOperatorChange}
                  options={[
                    { label: 'AND', value: '&&' },
                    { label: 'OR', value: '||' },
                  ]}
                  value={expression.expr} />
        </FormGroup>
      </Col>
      <AggregationConditionExpression {...props}
                                      expression={expression.right}
                                      onChange={onChildChange('right')}
                                      level={level + 1}
                                      renderLabel={false} />
    </>
  );
};

BooleanExpression.propTypes = {
  expression: PropTypes.shape({
    expr: PropTypes.string,
    left: PropTypes.object,
    right: PropTypes.object,
  }).isRequired,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
};

export default BooleanExpression;
