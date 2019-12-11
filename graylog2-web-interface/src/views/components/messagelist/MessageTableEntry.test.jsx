// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';
import * as Immutable from 'immutable';

import MessageTableEntry from './MessageTableEntry';

describe('MessageTableEntry', () => {
  it('renders message for unknown selected fields', () => {
    const message = {
      id: 'deadbeef',
      index: 'test_0',
      fields: {
        message: 'Something happened!',
      },
    };
    const wrapper = mount((
      <table>
        <MessageTableEntry expandAllRenderAsync
                           toggleDetail={() => {}}
                           fields={Immutable.List()}
                           message={message}
                           selectedFields={Immutable.OrderedSet(['message', 'notexisting'])}
                           expanded={false} />
      </table>
    ));
    expect(wrapper).toIncludeText('Something happened!');
  });
});
