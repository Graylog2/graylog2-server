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

import { Clearfix } from 'components/graylog';
import { internalNodePropType } from 'logic/alerts/AggregationExpressionTypes';

// eslint-disable-next-line import/no-cycle
import AggregationConditionExpression from '../AggregationConditionExpression';

const BooleanExpression = (props) => {
  const { expression, level, onChildChange, validation } = props;

  return (
    <>
      <AggregationConditionExpression {...props}
                                      expression={expression.left}
                                      validation={validation.left}
                                      parent={expression}
                                      onChange={onChildChange('left')}
                                      level={level + 1} />
      <Clearfix />
      <AggregationConditionExpression {...props}
                                      expression={expression.right}
                                      validation={validation.right}
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
  validation: PropTypes.object,
};

BooleanExpression.defaultProps = {
  parent: undefined,
  validation: {},
};

export default BooleanExpression;
