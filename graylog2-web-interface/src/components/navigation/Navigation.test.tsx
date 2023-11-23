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
import { render, screen } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import type { Location } from 'history';
import { defaultUser } from 'defaultMockValues';

import mockComponent from 'helpers/mocking/MockComponent';
import { asMock } from 'helpers/mocking';
import Navigation from 'components/navigation/Navigation';
import useCurrentUser from 'hooks/useCurrentUser';
import PerspectivesBindings from 'components/perspectives/bindings';
import PerspectivesProvider from 'components/perspectives/contexts/PerspectivesProvider';
import useLocation from 'routing/useLocation';
import HotkeysProvider from 'contexts/HotkeysProvider';

jest.mock('./ScratchpadToggle', () => mockComponent('ScratchpadToggle'));
jest.mock('hooks/useCurrentUser');
jest.mock('./DevelopmentHeaderBadge', () => () => <span />);
jest.mock('routing/withLocation', () => (x) => x);
jest.mock('hooks/useFeature', () => (featureFlag: string) => featureFlag === 'frontend_hotkeys');
jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '' })));

describe('Navigation', () => {
  const SUT = () => <HotkeysProvider><PerspectivesProvider><Navigation /></PerspectivesProvider></HotkeysProvider>;

  beforeAll(() => {
    PluginStore.register(new PluginManifest({}, PerspectivesBindings));
  });

  beforeEach(() => {
    asMock(useCurrentUser).mockReturnValue(defaultUser);
    asMock(useLocation).mockReturnValue({ pathname: '/' } as Location);
  });

  it('has common elements', async () => {
    render(<SUT />);

    await screen.findByRole('link', { name: /throughput/i });
    await screen.findByRole('button', { name: /help/i });
    await screen.findByRole('link', { name: /welcome/i });
    await screen.findByRole('button', { name: /user menu for administrator/i });
  });
});
