// @flow strict
import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import { alice as exampleUser } from 'fixtures/users';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';

import TeamsSection from './TeamsSection';

describe('<TeamsSection />', () => {
  it('should display info if license is not present', async () => {
    render(<TeamsSection user={exampleUser} />);

    expect(screen.getByText('Enterprise Feature')).toBeInTheDocument();
  });

  it('should display enterprise user teams assignment plugin', async () => {
    PluginStore.register(new PluginManifest({}, {
      teams: {
        UserTeamsAssignment: () => <>UserTeamsAssignment</>,
      },
    }));

    render(<TeamsSection user={exampleUser} />);

    expect(screen.getByText('UserTeamsAssignment')).toBeInTheDocument();
  });
});
