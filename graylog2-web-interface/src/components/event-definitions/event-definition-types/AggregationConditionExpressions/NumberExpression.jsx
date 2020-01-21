import React from 'react';
import PropTypes from 'prop-types';
import lodash from 'lodash';

import { Input } from 'components/bootstrap';
import { Col } from 'components/graylog';

import FormsUtils from 'util/FormsUtils';

import { numberExpressionNodePropType } from 'logic/alerts/AggregationExpressionTypes';

const NumberExpression = ({ expression, onChange, renderLabel, validation }) => {
  const handleChange = (event) => {
    const nextExpression = lodash.cloneDeep(expression);
    nextExpression.value = event.target.value === '' ? '' : FormsUtils.getValueFromInput(event.target);
    onChange({ conditions: nextExpression });
  };

  return (
    <Col md={3}>
      <Input id="aggregation-threshold"
             name="threshold"
             label={renderLabel ? 'Threshold' : ''}
             type="number"
             value={lodash.get(expression, 'value')}
             bsStyle={validation.errors.conditions ? 'error' : null}
             help={lodash.get(validation, 'errors.conditions[0]', null)}
             onChange={handleChange} />
    </Col>
  );
};

NumberExpression.propTypes = {
  expression: numberExpressionNodePropType.isRequired,
  onChange: PropTypes.func.isRequired,
  renderLabel: PropTypes.bool.isRequired,
  validation: PropTypes.object.isRequired,
};

export default NumberExpression;
