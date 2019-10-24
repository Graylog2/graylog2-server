import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Select } from 'components/common';
import { Clearfix, Col, FormGroup } from 'components/graylog';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

import styles from '../AggregationConditionExpression.css';

const BooleanExpression = (props) => {
  const { expression, onChildChange, onChange } = props;

  const handleOperatorChange = (nextOperator) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.expr = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <>
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      onChange={onChildChange('left')} />
      <Clearfix />
      <Col md={1}>
        <FormGroup controlId="boolean-operator">
          <div className={styles.formControlNoLabel}>
            <Select id="boolean-operator"
                    matchProp="label"
                    onChange={handleOperatorChange}
                    options={[
                      { label: 'AND', value: '&&' },
                      { label: 'OR', value: '||' },
                    ]}
                    value={expression.expr} />
          </div>
        </FormGroup>
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
