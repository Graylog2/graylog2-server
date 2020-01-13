import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Select } from 'components/common';
import { Col, ControlLabel, FormGroup, HelpBlock, Row } from 'components/graylog';

import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const ComparisonExpression = (props) => {
  const { expression, level, onChildChange, onChange, renderLabel, validation } = props;

  const handleExpressionOperatorChange = (nextOperator) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.expr = nextOperator;
    onChange({ conditions: nextExpression });
  };

  return (
    <Col md={10}>
      <Row className="row-sm">
        <AggregationConditionExpression {...props}
                                        expression={expression.left}
                                        parent={expression}
                                        onChange={onChildChange('left')}
                                        level={level + 1} />

        <Col md={3}>
          <FormGroup controlId="aggregation-condition" validationState={validation.errors.conditions ? 'error' : null}>
            {renderLabel && <ControlLabel>Is</ControlLabel>}
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
                    value={expression.expr}
                    clearable={false} />
            {validation.errors.conditions && (
              <HelpBlock>{lodash.get(validation, 'errors.conditions[0]')}</HelpBlock>
            )}
          </FormGroup>
        </Col>
        <AggregationConditionExpression {...props}
                                        expression={expression.right}
                                        parent={expression}
                                        onChange={onChildChange('right')}
                                        level={level + 1} />
      </Row>
    </Col>
  );
};

ComparisonExpression.propTypes = {
  expression: internalNodePropType.isRequired,
  level: PropTypes.number.isRequired,
  onChange: PropTypes.func.isRequired,
  onChildChange: PropTypes.func.isRequired,
  renderLabel: PropTypes.bool.isRequired,
  validation: PropTypes.object.isRequired,
};

export default ComparisonExpression;
