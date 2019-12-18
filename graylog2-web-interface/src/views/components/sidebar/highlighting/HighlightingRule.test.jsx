// @flow strict
import * as React from 'react';
import { mount } from 'wrappedEnzyme';

import mockAction from 'helpers/mocking/MockAction';
import Rule from 'views/logic/views/formatting/highlighting/HighlightingRule';
import { HighlightingRulesActions } from 'views/stores/HighlightingRulesStore';
import HighlightingRule from './HighlightingRule';

jest.mock('components/common/ColorPickerPopover', () => 'color-picker-popover');
jest.mock('views/stores/HighlightingRulesStore', () => ({ HighlightingRulesActions: {} }));

describe('HighlightingRule', () => {
  const rule = Rule.create('response_time', '250', undefined, '#f44242');

  it('should display field and value of rule', () => {
    const wrapper = mount(<HighlightingRule rule={rule} />);
    expect(wrapper).toIncludeText('response_time');
    expect(wrapper).toIncludeText('250');
  });

  it('should update rule if color was changed', () => {
    HighlightingRulesActions.update = mockAction(jest.fn(updatedRule => Promise.resolve([updatedRule])));
    const wrapper = mount(<HighlightingRule rule={rule} />);

    const { onChange } = wrapper.find('color-picker-popover').props();
    const hidePopover = jest.fn();

    return onChange('#416af4', undefined, hidePopover).then(() => {
      expect(HighlightingRulesActions.update)
        .toHaveBeenCalledWith(Rule.builder()
          .field('response_time')
          .value('250')
          .color('#416af4')
          .build());
    });
  });

  it('should close popover when color was changed', () => {
    HighlightingRulesActions.update = mockAction(jest.fn(updatedRule => Promise.resolve([updatedRule])));
    const wrapper = mount(<HighlightingRule rule={rule} />);

    const { onChange } = wrapper.find('color-picker-popover').props();
    const hidePopover = jest.fn();

    return onChange('#416af4', undefined, hidePopover)
      .then(() => expect(hidePopover).toHaveBeenCalled());
  });

  describe('rule removal:', () => {
    let oldConfirm = null;
    let deleteIcon;
    beforeEach(() => {
      oldConfirm = window.confirm;
      window.confirm = jest.fn(() => false);

      HighlightingRulesActions.remove = mockAction(jest.fn(() => Promise.resolve([])));
      const wrapper = mount(<HighlightingRule rule={rule} />);

      deleteIcon = wrapper.find('span[title="Remove this Highlighting Rule"]');
    });
    afterEach(() => {
      window.confirm = oldConfirm;
    });
    it('asks for confirmation before rule is removed', () => {
      deleteIcon.simulate('click');
      expect(window.confirm).toHaveBeenCalledWith('Do you really want to remove this highlighting?');
    });
    it('does not remove rule if confirmation was cancelled', () => {
      deleteIcon.simulate('click');
      expect(HighlightingRulesActions.remove).not.toHaveBeenCalled();
    });
    it('removes rule rule if confirmation was acknowledged', () => {
      window.confirm = jest.fn(() => true);
      deleteIcon.simulate('click');
      expect(HighlightingRulesActions.remove).toHaveBeenCalledWith(rule);
    });
  });
});
