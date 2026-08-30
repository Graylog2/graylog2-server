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
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import type { Permission } from 'graylog-web-plugin/plugin';

import { asMock } from 'helpers/mocking';
import useCurrentUser from 'hooks/useCurrentUser';
import { adminUser } from 'fixtures/users';

import TokenStep from './TokenStep';

import { useCollectorsMutations } from '../hooks';
import useSendCollectorsTelemetry from '../hooks/useSendCollectorsTelemetry';
import { mockCollectorsMutations } from '../testing/mockMutations';
import type { Fleet } from '../types';

jest.mock('hooks/useCurrentUser');
jest.mock('../hooks/useCollectorsMutations');
jest.mock('../hooks/useSendCollectorsTelemetry');

const userWith = (permissions: Array<string>) =>
  adminUser.toBuilder().permissions(Immutable.List(permissions as Array<Permission>)).build();

const fleet: Fleet = {
  id: 'fleet-1',
  name: 'Production',
  created_at: '2026-01-01T00:00:00Z',
  updated_at: '2026-01-01T00:00:00Z',
};

const PERMISSION_DENIED_TITLE = 'You do not have permission to create enrollment tokens for this fleet';

describe('TokenStep permissions', () => {
  beforeEach(() => {
    jest.clearAllMocks();
    asMock(useCollectorsMutations).mockReturnValue(mockCollectorsMutations());
    asMock(useSendCollectorsTelemetry).mockReturnValue(jest.fn());
  });

  // TokenStep renders exactly one <button> (the Generate button) while generatedToken is null,
  // so a plain role query is unambiguous. A name-filtered role query can't be used here: once the
  // `title` attribute is set, dom-accessibility-api's accessible-name computation reports the
  // title text instead of the button's visible label ("Generate token"), so
  // `getByRole('button', { name: /generate token/i })` spuriously fails to match — this is a
  // testing-library/jsdom accname quirk, not a product bug (verified against a minimal
  // reproduction outside this component).
  const findGenerateButton = async () => screen.findByRole('button');

  it('disables Generate token with an explanatory title when token create is not permitted on the fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_fleets:read:fleet-1']));

    render(<TokenStep fleet={fleet} generatedToken={null} onGenerated={jest.fn()} onChangeToken={jest.fn()} />);

    const button = await findGenerateButton();

    expect(button).toHaveTextContent('Generate token');
    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('title', PERMISSION_DENIED_TITLE);
  });

  it('enables Generate token without a title when token create is permitted on the fleet', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_enrollment_tokens:create:fleet-1']));

    render(<TokenStep fleet={fleet} generatedToken={null} onGenerated={jest.fn()} onChangeToken={jest.fn()} />);

    const button = await findGenerateButton();

    expect(button).toHaveTextContent('Generate token');
    expect(button).not.toBeDisabled();
    expect(button).not.toHaveAttribute('title');
  });

  it('does not permission-disable the button when a different fleet grants the permission', async () => {
    asMock(useCurrentUser).mockReturnValue(userWith(['collector_enrollment_tokens:create:fleet-2']));

    render(<TokenStep fleet={fleet} generatedToken={null} onGenerated={jest.fn()} onChangeToken={jest.fn()} />);

    const button = await findGenerateButton();

    expect(button).toBeDisabled();
    expect(button).toHaveAttribute('title', PERMISSION_DENIED_TITLE);
  });
});
