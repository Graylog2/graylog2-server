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

type ExpressionProps = {
  expression?: any;
  series: any[];
};

const Expression = ({
  expression = null,
  series,
}: ExpressionProps) => {
  if (!expression) {
    return 'No condition configured';
  }

  switch (expression.expr) {
    case 'number':
      return expression.value;
    case 'number-ref':
      // eslint-disable-next-line no-case-declarations
      const selectedSeries = series.find((s) => s.id === expression.ref);

      return (selectedSeries && selectedSeries.type
        ? <var>{selectedSeries.type}({selectedSeries.strategy ? `${selectedSeries.strategy}, ` : null}{selectedSeries.field}{selectedSeries.percentile ? `, ${selectedSeries.percentile}` : null})</var>
        : <span>No series selected</span>);
    case '&&':
    case '||':
      return (
        <>
          <Expression expression={expression.left} series={series} />{' '}
          <strong className="text-info">{expression.expr === '&&' ? 'AND' : 'OR'}</strong>{' '}
          <Expression expression={expression.right} series={series} />
        </>
      );
    case 'group':
      return <span>[<Expression expression={expression.child} series={series} />]</span>;
    case '<':
    case '<=':
    case '>':
    case '>=':
    case '==':
      return (
        <>
          <Expression expression={expression.left} series={series} />{' '}
          <strong className="text-primary">{expression.expr}{' '}</strong>
          <Expression expression={expression.right} series={series} />
        </>
      );
    default:
      return 'No condition configured';
  }
};

type AggregationConditionSummaryProps = {
  conditions: any;
  series: any[];
};

const AggregationConditionSummary = ({
  conditions,
  series,
}: AggregationConditionSummaryProps) => <Expression expression={conditions?.expression} series={series} />;

export default AggregationConditionSummary;
