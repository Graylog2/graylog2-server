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

import { usePluginExports } from 'views/test/testPlugins';

import SigmaEventDefinitionPage from './SigmaEventDefinitionPage';

jest.mock('routing/useLocation', () => jest.fn(() => ({ pathname: '/alerts/definitions/sigma/git-import' })));
jest.mock('components/events/EventsPageNavigation', () => () => null);

const GitImportForm = () => <span>Git import form</span>;

describe('SigmaEventDefinitionPage', () => {
  describe('without an import plugin', () => {
    it('renders as not found', async () => {
      render(<SigmaEventDefinitionPage />);

      expect(await screen.findByText('Page not found')).toBeInTheDocument();
    });
  });

  describe('with an import plugin whose condition is met', () => {
    usePluginExports({
      'eventDefinitions.components.sigmaGitImport': [
        { component: GitImportForm, key: 'gitImport', useCondition: () => true },
      ],
    });

    it('renders the import form', async () => {
      render(<SigmaEventDefinitionPage />);

      expect(await screen.findByText('Git import form')).toBeInTheDocument();
    });
  });

  describe('with an import plugin whose condition is not met', () => {
    usePluginExports({
      'eventDefinitions.components.sigmaGitImport': [
        { component: GitImportForm, key: 'gitImport', useCondition: () => false },
      ],
    });

    it('renders as not found', async () => {
      render(<SigmaEventDefinitionPage />);

      expect(await screen.findByText('Page not found')).toBeInTheDocument();
      expect(screen.queryByText('Git import form')).not.toBeInTheDocument();
    });
  });
});
