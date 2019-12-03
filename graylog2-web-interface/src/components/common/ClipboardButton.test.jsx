import React from 'react';
import { mountWithTheme as mount } from 'theme/enzymeWithTheme';
import ClipboardJS from 'clipboard';
import ClipboardButton from './ClipboardButton';

jest.mock('clipboard');

describe('ClipboardButton', () => {
  it('does not pass container option to clipboard.js if not specified', () => {
    mount(<ClipboardButton title="Copy" />);
    expect(ClipboardJS).toHaveBeenCalledWith('[data-clipboard-button]', {});
  });
  it('uses `container` prop to pass as an option to clipboard.js', () => {
    mount(<ClipboardButton title="Copy" container={42} />);
    expect(ClipboardJS).toHaveBeenCalledWith('[data-clipboard-button]', { container: 42 });
  });
});
