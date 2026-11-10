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

import { asMock } from 'helpers/mocking';
import usePluginEntities from 'hooks/usePluginEntities';

import TacticsTechniquesDetailRow from './TacticsTechniquesDetailRow';

jest.mock('hooks/usePluginEntities');

const entity = { tactics_techniques: ['attack.t1059'] };

describe('TacticsTechniquesDetailRow', () => {
  it('renders nothing when no plugin is registered', () => {
    asMock(usePluginEntities).mockReturnValue([]);

    render(<TacticsTechniquesDetailRow entity={entity} />);

    expect(screen.queryByText(/Tactics\/Techniques Row/)).not.toBeInTheDocument();
  });

  it('renders nothing when the plugin useCondition returns false', () => {
    asMock(usePluginEntities).mockReturnValue([
      { component: () => <div>Tactics/Techniques Row</div>, useCondition: () => false },
    ]);

    render(<TacticsTechniquesDetailRow entity={entity} />);

    expect(screen.queryByText(/Tactics\/Techniques Row/)).not.toBeInTheDocument();
  });

  it('renders the plugin component with the given entity when enabled', async () => {
    const PluginComponent = ({ entity: pluginEntity }: { entity: typeof entity }) => (
      <div>Techniques: {pluginEntity.tactics_techniques.join(', ')}</div>
    );

    asMock(usePluginEntities).mockReturnValue([{ component: PluginComponent, useCondition: () => true }]);

    render(<TacticsTechniquesDetailRow entity={entity} />);

    await screen.findByText('Techniques: attack.t1059');
  });
});
