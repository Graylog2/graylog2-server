import React from 'react';
import { mount } from 'enzyme';

import AggregationConditionExpression from './AggregationConditionExpression';

const getComparisonExpression = (operator, value = 0) => {
  return {
    expr: operator,
    left: {
      expr: 'number-ref',
      ref: '1234',
    },
    right: {
      expr: 'number',
      value: value,
    },
  };
};

const getBooleanExpression = (operator, left = getComparisonExpression(), right = getComparisonExpression()) => {
  return {
    expr: operator,
    left: left,
    right: right,
  };
};

const getGroupExpression = (operator, child = getComparisonExpression()) => {
  return {
    expr: 'group',
    operator: operator,
    child: child,
  };
};

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
    it('should render empty comparison expression', () => {
      const eventDefinition = {
        config: {
          series: [],
        },
      };

      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={eventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={getComparisonExpression()} />,
      );
      expect(wrapper.find('BooleanOperatorSelector').length).toBe(1);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render simple comparison expression', () => {
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={getComparisonExpression('<', 12)} />,
      );
      expect(wrapper.find('BooleanOperatorSelector').length).toBe(1);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render a boolean expression', () => {
      const expression = getBooleanExpression('&&');
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => {}}
                                        expression={expression} />,
      );
      expect(wrapper.find('BooleanOperatorSelector').length).toBe(1);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render a group expression with a comparison expression', () => {
      const expression = getGroupExpression('&&');
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={expression} />,
      );
      expect(wrapper.find('BooleanOperatorSelector').length).toBe(1);
      expect(wrapper).toMatchSnapshot();
    });

    it('should render a group expression with a boolean expression', () => {
      const expression = getBooleanExpression('&&', getComparisonExpression(), getGroupExpression('&&'));
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={() => { }}
                                        expression={expression} />,
      );
      expect(wrapper.find('BooleanOperatorSelector').length).toBe(2);
      expect(wrapper).toMatchSnapshot();
    });
  });

  describe('managing conditions', () => {
    it('should generate right expression when adding conditions', () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('&&');
        expect(update.left).toBe(expression);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );
      const addConditionButton = wrapper.find('button > [name="plus"]');
      expect(addConditionButton).toHaveLength(1);
      addConditionButton.simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should generate right expression when adding conditions with different operator', () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('||');
        expect(update.left).toBe(expression);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );
      wrapper.setState({ globalGroupOperator: '||' }, () => {
        const addConditionButton = wrapper.find('button > [name="plus"]');
        expect(addConditionButton).toHaveLength(1);
        addConditionButton.simulate('click');
        expect(onChange.mock.calls.length).toBe(1);
      });
    });

    it('should generate right expression when deleting conditions', () => {
      const remainingExpression = getComparisonExpression('>', 42);
      const expression = getBooleanExpression('&&', getComparisonExpression('<', 12), remainingExpression);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update).toBe(remainingExpression);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );
      const removeConditionButton = wrapper.find('button > [name="minus"]');
      expect(removeConditionButton).toHaveLength(2);
      removeConditionButton.at(0).simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should propagate null update when deleting last condition', () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update).toBe(null);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );
      const removeConditionButton = wrapper.find('button > [name="minus"]');
      expect(removeConditionButton).toHaveLength(1);
      removeConditionButton.simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });
  });

  describe('managing groups', () => {
    it('should generate right expression when adding groups', () => {
      const expression = getComparisonExpression('<', 12);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('&&');
        expect(update.left).toBe(expression);
        expect(update.right.expr).toBe('group');
        expect(update.right.operator).toBe('||');
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const addGroupButton = wrapper.find('button > [name="code-fork"]');
      expect(addGroupButton).toHaveLength(1);
      addGroupButton.simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should generate right expression when deleting groups', () => {
      const leftExpression = getComparisonExpression();
      const expression = getBooleanExpression('&&', leftExpression, getGroupExpression('&&'));
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update).toBe(leftExpression);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const deleteConditionButton = wrapper.find('button > [name="minus"]');
      expect(deleteConditionButton).toHaveLength(2);
      deleteConditionButton.last().simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should generate right expression when adding conditions inside group', () => {
      const leftExpression = getComparisonExpression();
      const booleanExpression = getBooleanExpression('||');
      const groupExpression = getGroupExpression('||', booleanExpression);
      const expression = getBooleanExpression('&&', leftExpression, groupExpression);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('&&');
        expect(update.left).toStrictEqual(leftExpression);
        expect(update.right.expr).toBe('group');
        expect(update.right.operator).toBe('||');
        const nextBooleanExpression = update.right.child;
        expect(nextBooleanExpression.expr).toBe('||');
        expect(nextBooleanExpression.left).toStrictEqual(booleanExpression.left);
        expect(nextBooleanExpression.right.expr).toBe('||');
        expect(nextBooleanExpression.right.left).toStrictEqual(booleanExpression.right);
        expect(nextBooleanExpression.right.right.expr).toBe(undefined); // This is the added condition
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const addConditionButton = wrapper.find('button > [name="plus"]');
      expect(addConditionButton).toHaveLength(3);
      addConditionButton.last().simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should generate right expression when deleting conditions inside group', () => {
      const leftExpression = getComparisonExpression('<', 12);
      const booleanExpression = getBooleanExpression('&&', leftExpression);
      const expression = getGroupExpression('&&', booleanExpression);
      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('group');
        expect(update.child).toBe(leftExpression);
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const deleteConditionButton = wrapper.find('button > [name="minus"]');
      expect(deleteConditionButton).toHaveLength(2);
      deleteConditionButton.last().simulate('click');
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should update boolean expressions when group operator changes', () => {
      const comparisonExpression = getComparisonExpression('<', 12);
      const secondBooleanExpression = getBooleanExpression('||', comparisonExpression, comparisonExpression);
      const firstBooleanExpression = getBooleanExpression('||', comparisonExpression, secondBooleanExpression);
      const groupExpression = getGroupExpression('||', firstBooleanExpression);
      const expression = getBooleanExpression('&&', comparisonExpression, groupExpression);

      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('&&');
        const nextGroup = update.right;
        expect(nextGroup.expr).toBe('group');
        expect(nextGroup.operator).toBe('&&');
        const nextBoolean = nextGroup.child;
        expect(nextBoolean.expr).toBe('&&');
        expect(nextBoolean.right.expr).toBe('&&');
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const select = wrapper.find('Select Select.boolean-operator').at(1);
      select.prop('onChange')({ value: '&&' });
      expect(onChange.mock.calls.length).toBe(1);
    });

    it('should update boolean expressions when global group operator changes', () => {
      const comparisonExpression = getComparisonExpression('<', 12);
      const groupBooleanExpression = getBooleanExpression('||', comparisonExpression, comparisonExpression);
      const groupExpression = getGroupExpression('||', groupBooleanExpression);
      const booleanExpression = getBooleanExpression('&&', comparisonExpression, groupExpression);
      const expression = getBooleanExpression('&&', comparisonExpression, booleanExpression);

      const onChange = jest.fn((key, update) => {
        expect(key).toBe('conditions');
        expect(update.expr).toBe('||');
        const nextBoolean = update.right;
        expect(nextBoolean.expr).toBe('||');
        const nextGroup = nextBoolean.right;
        expect(nextGroup.operator).toBe('||');
        const nextGroupBoolean = nextGroup.child;
        expect(nextGroupBoolean.expr).toBe('||');
      });
      const wrapper = mount(
        <AggregationConditionExpression eventDefinition={defaultEventDefinition}
                                        validation={{ errors: {} }}
                                        formattedFields={[]}
                                        aggregationFunctions={[]}
                                        onChange={onChange}
                                        expression={expression} />,
      );

      const select = wrapper.find('Select Select.boolean-operator').at(0);
      select.prop('onChange')({ value: '||' });
      expect(onChange.mock.calls.length).toBe(1);
    });
  });
});
