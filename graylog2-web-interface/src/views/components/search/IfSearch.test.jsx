import React from 'react';
import renderer from 'react-test-renderer';
import 'helpers/mocking/react-dom_mock';

import View from 'views/logic/views/View';
import ViewTypeContext from 'views/components/contexts/ViewTypeContext';

import IfSearch from './IfSearch';

describe('IfSearch', () => {
  it('should render children with search context', () => {
    const wrapper = renderer.create(
      <ViewTypeContext.Provider value={View.Type.Search}>
        <span>I must not fear.</span>
        <IfSearch>
          <span>Fear is the mind-killer.</span>
        </IfSearch>
      </ViewTypeContext.Provider>,
    );
    expect(wrapper).toMatchSnapshot();
  });

  it('should not render children without context', () => {
    const wrapper = renderer.create(
      <div>
        <span>I must not fear.</span>
        <IfSearch>
          <span>Fear is the mind-killer.</span>
        </IfSearch>
      </div>,
    );

    expect(wrapper).toMatchSnapshot();
  });

  it('should not render children without search context', () => {
    const wrapper = renderer.create(
      <ViewTypeContext.Provider value={View.Type.Dashboard}>
        <span>I must not fear.</span>
        <IfSearch>
          <span>Fear is the mind-killer.</span>
        </IfSearch>
      </ViewTypeContext.Provider>,
    );

    expect(wrapper).toMatchSnapshot();
  });
});
