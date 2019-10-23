import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const ComparisonExpression = (props) => {
  const { expression, onChildChange, onChange, validation } = props;

  const handleExpressionOperatorChange = (nextOperator) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.expr = nextOperator;
    onChange('conditions', nextExpression);
  };

  return (
    <Col md={10}>
      <Row className="row-sm">
        <AggregationConditionExpression {...props}
                                        expression={expression.left}
                                        onChange={onChildChange('left')} />
        <Col md={3}>
          <FormGroup controlId="aggregation-condition" validationState={validation.errors.conditions ? 'error' : null}>
            <ControlLabel>Is</ControlLabel>
            <Select id="aggregation-condition"
                    matchProp="label"
                    placeholder="Select Condition"
                    onChange={handleExpressionOperatorChange}
                    options={[
                      { label: '<', value: '<' },
                      { label: '<=', value: '<=' },
                      { label: '>', value: '>' },
                      { label: '>=', value: '>=' },
                      { label: '=', value: '==' },
                    ]}
                    value={expression.expr} />
            {validation.errors.conditions && (
              <HelpBlock>{lodash.get(validation, 'errors.conditions[0]')}</HelpBlock>
            )}
          </FormGroup>
        </Col>
        <AggregationConditionExpression {...props}
                                        expression={expression.right}
                                        onChange={onChildChange('right')} />
      </Row>
    </Col>
  );
};

ComparisonExpression.propTypes = {
  expression: PropTypes.shape({
    expr: PropTypes.string,
    left: PropTypes.object,
    right: PropTypes.object,
  }).isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
  validation: PropTypes.object.isRequired,
};

export default ComparisonExpression;
