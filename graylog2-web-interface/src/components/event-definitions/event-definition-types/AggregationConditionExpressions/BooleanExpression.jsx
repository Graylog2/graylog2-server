import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Select } from 'components/common';
import { Clearfix, Col, FormControl, FormGroup } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression.jsx';

const BooleanExpression = (props) => {
  const { expression, level, onChildChange, parent, onChange } = props;

  const handleOperatorChange = (nextOperator) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.expr = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <>
      {(!parent || parent.expr !== expression.expr) && (
        <Col md={12}>
          <div className="form-inline" style={{ marginBottom: '15px', fontSize: '14px' }}>
            <FormGroup>
              <FormControl.Static>Messages must meet </FormControl.Static>
            </FormGroup>
            <FormGroup style={{ width: '100px', marginLeft: '1em', marginRight: '1em' }}>
              <Select id="boolean-operator"
                      matchProp="label"
                      size="small"
                      onChange={handleOperatorChange}
                      options={[
                        { label: 'all', value: '&&' },
                        { label: 'any', value: '||' },
                      ]}
                      value={expression.expr} />
            </FormGroup>
            <FormGroup>
              <FormControl.Static> of the following rules:</FormControl.Static>
            </FormGroup>
          </div>
        </Col>
      )}
      <Clearfix />
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
  parent: internalNodePropType,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
};

BooleanExpression.defaultProps = {
  parent: undefined,
};

export default BooleanExpression;
