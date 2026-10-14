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
import { screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

const changeConfigurationField = async (label: string, value: string) => {
  await screen.findByRole('spinbutton', { name: new RegExp(`^${label}`) });
  const isDisk = ['Stop allocating shards at', 'Move shards away at', 'Protect indexes at'].includes(label);
  const match = value.match(/^(-?\d+(?:\.\d+)?)(%|[kmgtp]?b|[gmk])$/);
  const amount = match?.[1] ?? value;
  const unit = match?.[2];
  const unitButton = screen.queryByRole('button', { name: `${label} unit` });

  if (unitButton) {
    let unitLabel = unit?.toUpperCase();
    if (unit === '%') unitLabel = isDisk ? '% disk used' : '% of heap';
    if (unit === 'g') unitLabel = 'GB';
    if (isDisk && !unit) unitLabel = 'Ratio (0–1)';
    if (unit && unit !== '%' && isDisk) unitLabel += ' free';
    if (unit && label === 'Recovery bandwidth limit') unitLabel += '/s';

    if (unitLabel && !unitButton.textContent?.includes(unitLabel)) {
      await userEvent.click(unitButton);
      await userEvent.click(await screen.findByRole('menuitem', { name: unitLabel }));
    }
  }

  const input = await screen.findByRole('spinbutton', { name: new RegExp(`^${label}`) });
  await userEvent.clear(input);
  await userEvent.type(input, amount);

  return input;
};

export default changeConfigurationField;
