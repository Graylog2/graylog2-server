import React from 'react';
import { mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import ColumnPivotConfiguration from './ColumnPivotConfiguration';

describe('<ColumnPivotConfiguraiton />', () => {
  it('should render as expected', () => {
    const wrapper = mount(<ColumnPivotConfiguration rollup />);
    expect(wrapper).toMatchSnapshot();
  });

  it('should change config when clicked on checkbox', () => {
    const changeFn = jest.fn((checked) => {
      expect(checked).toBe(true);
    });
    const wrapper = mount(<ColumnPivotConfiguration onRollupChange={changeFn}
                                                    rollup={false} />);
    wrapper.find('input[name="rollup"]').simulate('change', { target: { checked: true } });
    expect(changeFn.mock.calls.length).toBe(1);
  });
});
