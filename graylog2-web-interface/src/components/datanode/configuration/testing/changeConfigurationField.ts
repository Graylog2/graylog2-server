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
import { screen, within } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';
import escapeRegExp from 'lodash/escapeRegExp';

const changeConfigurationField = async (label: string, amount: string, unit?: string) => {
  const inputName = new RegExp(`^${escapeRegExp(label)}`);
  await screen.findByRole('spinbutton', { name: inputName });

  if (unit) {
    const unitButton = screen.getByRole('button', { name: `${label} unit` });

    if (!within(unitButton).queryByText(unit)) {
      await userEvent.click(unitButton);
      await userEvent.click(await screen.findByRole('menuitem', { name: unit }));
    }
  }

  const input = screen.getByRole('spinbutton', { name: inputName });
  await userEvent.clear(input);
  await userEvent.type(input, amount);

  return input;
};

export default changeConfigurationField;
