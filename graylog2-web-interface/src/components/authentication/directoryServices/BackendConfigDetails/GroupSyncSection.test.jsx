// @flow strict
import * as React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';
import { PluginManifest, PluginStore } from 'graylog-web-plugin/plugin';
import { ldapBackend as exampleAuthBackend } from 'fixtures/authenticationBackends';

import GroupSyncSection from './GroupSyncSection';

describe('<GroupSyncSection />', () => {
  it('should display info if enterprise plugin does not exist', () => {
    // $FlowFixMe AuthenticationBackend has the correct format
    render(<GroupSyncSection authenticationBackend={exampleAuthBackend} roles={Immutable.List()} />);

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

    // $FlowFixMe AuthenticationBackend has the correct format
    render(<GroupSyncSection authenticationBackend={exampleAuthBackend} roles={Immutable.List()} />);

    expect(screen.getByText('GroupSyncSection')).toBeInTheDocument();
  });
});
