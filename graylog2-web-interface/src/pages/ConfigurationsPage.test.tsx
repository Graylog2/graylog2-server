import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import asMock from 'helpers/mocking/AsMock';
import suppressConsole from 'helpers/suppressConsole';

import ConfigurationsPage from 'pages/ConfigurationsPage';
import usePluginEntities from 'views/logic/usePluginEntities';
import SidecarConfig from 'components/configurations/SidecarConfig';

jest.mock('views/logic/usePluginEntities');
jest.mock('components/configurations/SearchesConfig', () => () => <span>Search Configuration</span>);
jest.mock('components/configurations/MessageProcessorsConfig', () => () => <span>Message Processors Configuration</span>);
jest.mock('components/configurations/SidecarConfig');

const ComponentThrowingError = () => {
  throw Error('Boom!');
};

const ComponentWorkingFine = () => (
  <span>It is all good!</span>
);

describe('ConfigurationsPage', () => {
  it('wrapps core configuration elements with error boundary', async () => {
    asMock(usePluginEntities).mockReturnValue([]);

    asMock(SidecarConfig).mockImplementation(() => {
      throw Error('Boom!');
    });

    suppressConsole(() => {
      render(<ConfigurationsPage />);
    });

    await screen.findByText('Message Processors Configuration');
    await screen.findByText('Boom!');
  });

  it('wraps plugin configuration elements with error boundary', async () => {
    asMock(usePluginEntities).mockReturnValue([
      { configType: 'foo', component: ComponentThrowingError },
      { configType: 'bar', component: ComponentWorkingFine },
    ]);

    suppressConsole(() => {
      render(<ConfigurationsPage />);
    });

    await screen.findByText('It is all good!');
    await screen.findByText('Boom!');
  });
});
