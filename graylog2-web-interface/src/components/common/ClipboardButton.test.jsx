/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React from 'react';
import { mount } from 'wrappedEnzyme';
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
