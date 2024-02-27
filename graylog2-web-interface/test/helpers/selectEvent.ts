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

import userEvent from '@testing-library/user-event';

/*
 * This file contains helper methods, which replace the `react-select-event` methods.
 * They are useful when interacting with the `react-select` select component.
 */

const clearAll = (container: HTMLElement, selectClassName: string) => {
  const clearIcons = container.querySelectorAll(`.${selectClassName} svg[aria-hidden="true"]`);
  userEvent.click(clearIcons[clearIcons.length - 1]);
};

export default { clearAll };
