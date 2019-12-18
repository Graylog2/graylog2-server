// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import FieldType from 'views/logic/fieldtypes/FieldType';
import Field from './Field';
import InteractiveContext from './contexts/InteractiveContext';

describe('Field', () => {
  describe('handles value action menu depending on interactive context', () => {
    const component = interactive => props => (
      <InteractiveContext.Provider value={interactive}>
        <Field {...props} />
      </InteractiveContext.Provider>
    );
    it('does not show value actions if interactive context is `false`', () => {
      const NoninteractiveComponent = component(false);
      const wrapper = mount(<NoninteractiveComponent name="foo"
                                                     queryId="someQueryId"
                                                     type={FieldType.Unknown} />);
      const fieldActions = wrapper.find('FieldActions');
      expect(fieldActions).not.toExist();
      expect(wrapper).toHaveText('foo');
    });
    it('shows value actions if interactive context is `true`', () => {
      const InteractiveComponent = component(true);
      const wrapper = mount(<InteractiveComponent name="foo"
                                                  queryId="someQueryId"
                                                  type={FieldType.Unknown} />);
      const fieldActions = wrapper.find('FieldActions');
      expect(fieldActions).toExist();
      expect(wrapper).toHaveText('foo');
    });
  });
});
