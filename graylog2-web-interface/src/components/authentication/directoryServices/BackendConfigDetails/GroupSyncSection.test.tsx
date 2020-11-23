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
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { ldapBackend as exampleAuthBackend } from 'fixtures/authenticationBackends';

import { DirectoryServiceBackend } from 'logic/authentication/directoryServices/types';

import GroupSyncSection from './GroupSyncSection';

describe('<GroupSyncSection />', () => {
  it('should display info if enterprise plugin does not exist', () => {
    render(<GroupSyncSection authenticationBackend={exampleAuthBackend as DirectoryServiceBackend} roles={Immutable.List()} />);

    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
    expect(screen.getByText('Group Synchronization')).toBeInTheDocument();
  });

  it('should display enterprise group sync section', () => {
    PluginStore.register(new PluginManifest({}, {
      'authentication.enterprise.directoryServices.groupSync': {
        components: {
          GroupSyncSection: () => <>GroupSyncSection</>,
        },
      },
    }));

    render(<GroupSyncSection authenticationBackend={exampleAuthBackend as DirectoryServiceBackend} roles={Immutable.List()} />);

    expect(screen.getByText('GroupSyncSection')).toBeInTheDocument();
  });
});
