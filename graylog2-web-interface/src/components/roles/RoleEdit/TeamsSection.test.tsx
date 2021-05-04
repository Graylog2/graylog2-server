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
import { manager as exampleRole } from 'fixtures/roles';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TeamsSection from './TeamsSection';

describe('<TeamsSection />', () => {
  it('should display info if license is not present', () => {
    render(<TeamsSection role={exampleRole} />);

    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
  });

  it('should display enterprise role teams assignment plugin', () => {
    PluginStore.register(new PluginManifest({}, {
      teams: {
        RoleTeamsAssignment: () => <>RoleTeamsAssignment</>,
      },
    }));

    render(<TeamsSection role={exampleRole} />);

    expect(screen.getByText('RoleTeamsAssignment')).toBeInTheDocument();
  });
});
