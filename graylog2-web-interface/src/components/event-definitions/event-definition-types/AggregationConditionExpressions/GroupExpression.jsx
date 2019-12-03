import React from 'react';
import PropTypes from 'prop-types';
import { cloneDeep } from 'lodash';

import { Clearfix, Col, FormControl, FormGroup } from 'components/graylog';
import { Select } from 'components/common';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';
// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const GroupExpression = (props) => {
  const { expression, level, onChange, onChildChange } = props;

  const handleOperatorChange = (nextOperator) => {
    const nextExpression = cloneDeep(expression);
    nextExpression.operator = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <>
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
                    value={expression.operator} />
          </FormGroup>
          <FormGroup>
            <FormControl.Static> of the following rules:</FormControl.Static>
          </FormGroup>
        </div>
      </Col>
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
