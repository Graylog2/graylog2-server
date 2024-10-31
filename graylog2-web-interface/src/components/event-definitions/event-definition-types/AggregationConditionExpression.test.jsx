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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import AggregationConditionExpression from './AggregationConditionExpression';

const getComparisonExpression = (operator, value = 0) => ({
  expr: operator,
  left: {
    expr: 'number-ref',
    ref: '1234',
  },
  right: {
    expr: 'number',
    value: value,
  },
});

const getBooleanExpression = (operator, left = getComparisonExpression(), right = getComparisonExpression()) => ({
  expr: operator,
  left: left,
  right: right,
});

const getGroupExpression = (operator, child = getComparisonExpression()) => ({
  expr: 'group',
  operator: operator,
  child: child,
});

describe('AggregationConditionExpression', () => {
  const defaultEventDefinition = {
    config: {
      series: [
        {
          id: '1234',
          function: 'count',
          field: 'timestamp',
        },
      ],
    },
  };

  describe('rendering conditions', () => {
    it('should render empty comparison expression', async () => {
      const eventDefinition = {
        config: {
          series: [],
        },
      };

      render(
        <AggregationConditionExpression eventDefinition={eventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={getComparisonExpression()} />,
      );

      await screen.findByText(/Messages must meet/);
    });

    it('should render simple comparison expression', async () => {
      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={getComparisonExpression('<', 12)} />,
      );

      await screen.findByText(/Messages must meet/);
    });

    it('should render a boolean expression', async () => {
      const expression = getBooleanExpression('&&');

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => {}}
                                        expression={expression} />,
      );

      await screen.findByText(/Messages must meet/);
    });

    it('should render a group expression with a comparison expression', async () => {
      const expression = getGroupExpression('&&');

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={expression} />,
      );

      await screen.findByText(/all/);
    });

    it('should render a group expression with a boolean expression', async () => {
      const expression = getBooleanExpression('&&', getComparisonExpression(), getGroupExpression('&&'));

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={expression} />,
      );

      expect(await screen.findAllByText(/all/)).toHaveLength(2);
    });
  });

  describe('managing conditions', () => {
    it('should generate right expression when adding conditions', async () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('&&');
        expect(conditions.left).toBe(expression);
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const addConditionButton = await screen.findByTitle('Add Expression');

      addConditionButton.click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should generate right expression when deleting conditions', async () => {
      const remainingExpression = getComparisonExpression('>', 42);
      const expression = getBooleanExpression('&&', getComparisonExpression('<', 12), remainingExpression);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions).toBe(remainingExpression);
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const removeConditionButton = await screen.findAllByTitle('Delete Expression');

      expect(removeConditionButton).toHaveLength(2);

      removeConditionButton[0].click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should propagate null update when deleting last condition', async () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions).toBe(null);
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const removeConditionButton = await screen.findByTitle('Delete Expression');

      removeConditionButton.click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });
  });

  describe('managing groups', () => {
    it('should generate right expression when adding groups', async () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('&&');
        expect(conditions.left).toBe(expression);
        expect(conditions.right.expr).toBe('group');
        expect(conditions.right.operator).toBe('||');
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const addGroupButton = await screen.findByRole('button', { name: 'Add Group' });

      addGroupButton.click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should generate right expression when deleting groups', async () => {
      const leftExpression = getComparisonExpression();
      const expression = getBooleanExpression('&&', leftExpression, getGroupExpression('&&'));
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions).toBe(leftExpression);
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const deleteConditionButton = await screen.findAllByTitle('Delete Expression');

      expect(deleteConditionButton).toHaveLength(2);

      deleteConditionButton[1].click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should generate right expression when adding conditions inside group', async () => {
      const leftExpression = getComparisonExpression();
      const booleanExpression = getBooleanExpression('||');
      const groupExpression = getGroupExpression('||', booleanExpression);
      const expression = getBooleanExpression('&&', leftExpression, groupExpression);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('&&');
        expect(conditions.left).toStrictEqual(leftExpression);
        expect(conditions.right.expr).toBe('group');
        expect(conditions.right.operator).toBe('||');

        const nextBooleanExpression = conditions.right.child;

        expect(nextBooleanExpression.expr).toBe('||');
        expect(nextBooleanExpression.left).toStrictEqual(booleanExpression.left);
        expect(nextBooleanExpression.right.expr).toBe('||');
        expect(nextBooleanExpression.right.left).toStrictEqual(booleanExpression.right);
        expect(nextBooleanExpression.right.right.expr).toBe(undefined); // This is the added condition
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const addConditionButton = await screen.findAllByTitle('Add Expression');

      expect(addConditionButton).toHaveLength(3);

      addConditionButton[2].click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should generate right expression when deleting conditions inside group', async () => {
      const leftExpression = getComparisonExpression('<', 12);
      const booleanExpression = getBooleanExpression('&&', leftExpression);
      const expression = getGroupExpression('&&', booleanExpression);
      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('group');
        expect(conditions.child).toBe(leftExpression);
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const deleteConditionButton = await screen.findAllByTitle('Delete Expression');

      expect(deleteConditionButton).toHaveLength(2);

      deleteConditionButton[1].click();

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should update boolean expressions when group operator changes', async () => {
      const comparisonExpression = getComparisonExpression('<', 12);
      const secondBooleanExpression = getBooleanExpression('||', comparisonExpression, comparisonExpression);
      const firstBooleanExpression = getBooleanExpression('||', comparisonExpression, secondBooleanExpression);
      const groupExpression = getGroupExpression('||', firstBooleanExpression);
      const expression = getBooleanExpression('&&', comparisonExpression, groupExpression);

      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('&&');

        const nextGroup = conditions.right;

        expect(nextGroup.expr).toBe('group');
        expect(nextGroup.operator).toBe('&&');

        const nextBoolean = nextGroup.child;

        expect(nextBoolean.expr).toBe('&&');
        expect(nextBoolean.right.expr).toBe('&&');
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const select = (await screen.findAllByLabelText('Boolean Operator'))[1];

      await selectEvent.openMenu(select);
      await selectEvent.select(select, 'all');

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should update boolean expressions when global group operator changes', async () => {
      const comparisonExpression = getComparisonExpression('<', 12);
      const groupBooleanExpression = getBooleanExpression('||', comparisonExpression, comparisonExpression);
      const groupExpression = getGroupExpression('||', groupBooleanExpression);
      const booleanExpression = getBooleanExpression('&&', comparisonExpression, groupExpression);
      const expression = getBooleanExpression('&&', comparisonExpression, booleanExpression);

      const onChange = jest.fn(({ conditions }) => {
        expect(conditions).toBeDefined();
        expect(conditions.expr).toBe('||');

        const nextBoolean = conditions.right;

        expect(nextBoolean.expr).toBe('||');

        const nextGroup = nextBoolean.right;

        expect(nextGroup.operator).toBe('||');

        const nextGroupBoolean = nextGroup.child;

        expect(nextGroupBoolean.expr).toBe('||');
      });

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const select = (await screen.findAllByLabelText('Boolean Operator'))[0];

      await selectEvent.openMenu(select);
      await selectEvent.select(select, 'any');

      await waitFor(() => {
        expect(onChange).toHaveBeenCalled();
      });
    });

    it('should display the right default initial global group operator from props', async () => {
      const expression = getBooleanExpression('||');

      render(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => {}}
                                        expression={expression} />,
      );

      const select = (await screen.findAllByLabelText('Boolean Operator'))[0];

      expect(screen.queryByText('all')).not.toBeInTheDocument();

      await selectEvent.openMenu(select);
      await selectEvent.select(select, 'all');

      await screen.findByText('all');
    });
  });
});
