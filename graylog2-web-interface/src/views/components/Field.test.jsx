// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import FieldType from 'views/logic/fieldtypes/FieldType';

import Field from './Field';
import InteractiveContext from './contexts/InteractiveContext';

describe('Field', () => {
  describe('handles value action menu depending on interactive context', () => {
    const component = (interactive) => (props) => (
      <InteractiveContext.Provider value={interactive}>
        <Field {...props} />
      </InteractiveContext.Provider>
    );

    it('does not show value actions for field if interactive context is `false`', () => {
      const NoninteractiveComponent = component(false);
      const wrapper = mount(
        <NoninteractiveComponent name="Field name"
                                 queryId="someQueryId"
                                 type={FieldType.Unknown}>
          Field options like sorting
        </NoninteractiveComponent>,
      );
      const fieldActions = wrapper.find('FieldActions');

      expect(fieldActions).not.toExist();
      expect(wrapper).toHaveText('Field name Field options like sorting');
    });

    it('shows value actions for field if interactive context is `true`', () => {
      const InteractiveComponent = component(true);
      const wrapper = mount(
        <InteractiveComponent name="Field name"
                              queryId="someQueryId"
                              type={FieldType.Unknown} />,
      );
      const fieldActions = wrapper.find('FieldActions');

      expect(fieldActions).toExist();
      expect(wrapper).toHaveText('Field name');
    });
  });
});
