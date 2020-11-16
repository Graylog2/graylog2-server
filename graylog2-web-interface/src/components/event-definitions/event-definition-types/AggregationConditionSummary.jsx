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

const AggregationConditionSummary = ({ conditions, series }) => {
  const renderExpression = (expression) => {
    if (!expression) {
      return 'No condition configured';
    }

    switch (expression.expr) {
      case 'number':
        return expression.value;
      case 'number-ref':
        // eslint-disable-next-line no-case-declarations
        const selectedSeries = series.find((s) => s.id === expression.ref);

        return (selectedSeries && selectedSeries.function
          ? <var>{selectedSeries.function}({selectedSeries.field})</var>
          : <span>No series selected</span>);
      case '&&':
      case '||':
        return (
          <>
            {renderExpression(expression.left)}{' '}
            <strong className="text-info">{expression.expr === '&&' ? 'AND' : 'OR'}</strong>{' '}
            {renderExpression(expression.right)}
          </>
        );
      case 'group':
        return <span>[{renderExpression(expression.child)}]</span>;
      case '<':
      case '<=':
      case '>':
      case '>=':
      case '==':
        return (
          <>
            {renderExpression(expression.left)}{' '}
            <strong className="text-primary">{expression.expr}{' '}</strong>
            {renderExpression(expression.right)}
          </>
        );
      default:
        return 'No condition configured';
    }
  };

  return renderExpression(conditions.expression);
};

AggregationConditionSummary.propTypes = {
  conditions: PropTypes.object.isRequired,
  series: PropTypes.array.isRequired,
};

export default AggregationConditionSummary;
