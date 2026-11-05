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
import { textOfTarget } from './captureTarget';

describe('textOfTarget', () => {
  it('leaves out the names of icons', () => {
    const header = document.createElement('span');
    header.innerHTML = 'Usernames <span class="material-symbols-rounded">arrow_drop_down</span>';

    expect(textOfTarget(header)).toBe('Usernames');
  });

  it('falls back to the title of icon buttons', () => {
    const icon = document.createElement('span');
    icon.className = 'material-symbols-rounded';
    icon.setAttribute('title', 'Incoming messages');
    icon.textContent = 'input';

    expect(textOfTarget(icon)).toBe('Incoming messages');
  });
});
