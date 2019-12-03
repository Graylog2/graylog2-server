import React from 'react';
import renderer from 'react-test-renderer';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';
import 'helpers/mocking/react-dom_mock';

import ColumnPivotConfiguration from './ColumnPivotConfiguration';

describe('<ColumnPivotConfiguraiton />', () => {
  it('should render as expected', () => {
    const wrapper = renderer.create(<ColumnPivotConfiguration rollup />);
    expect(wrapper.toJSON()).toMatchSnapshot();
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
