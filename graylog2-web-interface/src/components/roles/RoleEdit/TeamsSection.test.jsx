// @flow strict
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { manager as exampleRole } from 'fixtures/roles';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TeamsSection from './TeamsSection';

describe('<TeamsSection />', () => {
  it('should display info if license is not present', async () => {
    render(<TeamsSection role={exampleRole} />);

    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
  });

  it('should display enterprise role teams assignment plugin', async () => {
    PluginStore.register(new PluginManifest({}, {
      teams: {
        RoleTeamsAssignment: () => <>RoleTeamsAssignment</>,
      },
    }));

    render(<TeamsSection role={exampleRole} />);

    expect(screen.getByText('RoleTeamsAssignment')).toBeInTheDocument();
  });
});
