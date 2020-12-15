/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
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
             bsStyle={validation.message ? 'error' : null}
             help={validation.message}
             onChange={handleChange} />
    </Col>
  );
};

NumberExpression.propTypes = {
  expression: numberExpressionNodePropType.isRequired,
  onChange: PropTypes.func.isRequired,
  renderLabel: PropTypes.bool.isRequired,
  validation: PropTypes.object,
};

NumberExpression.defaultProps = {
  validation: {},
};

export default NumberExpression;
