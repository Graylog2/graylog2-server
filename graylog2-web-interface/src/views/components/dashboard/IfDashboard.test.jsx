import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import IfDashboard from './IfDashboard';

describe('IfDashboard', () => {
  it('should render children with dashboard context', () => {
    const wrapper = renderer.create(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </ViewTypeContext.Provider>,
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should not render children without dashboard context', () => {
    const wrapper = renderer.create(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </ViewTypeContext.Provider>,
    );

    expect(wrapper).toMatchSnapshot();
  });


  it('should not render children without context', () => {
    const wrapper = renderer.create(
      <div>
        <span>I must not fear.</span>
        <IfDashboard>
          <span>Fear is the mind-killer.</span>
        </IfDashboard>
      </div>,
    );

    expect(wrapper).toMatchSnapshot();
  });
});
