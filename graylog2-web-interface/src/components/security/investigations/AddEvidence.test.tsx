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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import type { EvidenceTypes } from './types';
import AddEvidence from './AddEvidence';

type SecEvidenceType = {
  invId: string,
  payload: {
    dashboards?: string[],
    searches?: string[],
    events?: string[],
    logs?: { id: string, index: string }[],
  },
};

const mockSecAddEvidence = jest.fn((_payload: SecEvidenceType) => {});
const mockUseAddEvidence = jest.fn(() => ({
  addEvidence: mockSecAddEvidence,
  addingEvidence: false,
}));

const mockUseInvestigationDrawer = jest.fn(() => ({
  selectedInvestigationId: 'investigation-id',
}));

type SecAddEvidenceProps = {
  id: string,
  type: string,
  children: React.ReactNode,
  index?: string,
};

const MockSecAddEvidence = ({ id, index, type, children }: SecAddEvidenceProps) => {
  const { addEvidence } = mockUseAddEvidence();
  const { selectedInvestigationId } = mockUseInvestigationDrawer();

  const submitEvidence = (e: React.BaseSyntheticEvent) => {
    e.preventDefault();
    e.stopPropagation();

    switch (type) {
      case 'logs':
        addEvidence({
          invId: selectedInvestigationId,
          payload: { logs: [{ id, index }] },
        });

        break;
      case 'dashboards':
      case 'searches':
      case 'events':
        addEvidence({
          invId: selectedInvestigationId,
          payload: { [type]: [id] },
        });

        break;
      default:
        break;
    }
  };

  const childrenWithProps = React.Children.map(children, (child: React.ReactElement) => (
    React.cloneElement(child, { onClick: submitEvidence })
  ) as React.ReactElement);

  // eslint-disable-next-line react/jsx-no-useless-fragment
  return <>{childrenWithProps}</>;
};

MockSecAddEvidence.defaultProps = {
  index: undefined,
};

const mockExports = jest.fn((_entityKey?: string) => ([{
  components: {
    AddEvidence: MockSecAddEvidence,
  },
  hooks: {
    useInvestigationDrawer: mockUseInvestigationDrawer,
  },
}]));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => mockExports()) },
}));

const renderAddEvidence = (id: string, type: EvidenceTypes, index: string = undefined) => {
  render(
    <AddEvidence id={id}
                 type={type}
                 index={index}
                 child={({ investigationSelected }) => <button type="button" disabled={!investigationSelected}>Add to investigation</button>} />,
  );
};

describe('AddEvidence', () => {
  it('should render button', () => {
    renderAddEvidence('dashboard-id', 'dashboards');

    expect(screen.getByText('Add to investigation')).toBeInTheDocument();
  });

  it('should not fail and render null if no plugin is registered', () => {
    mockExports.mockReturnValueOnce(undefined);
    renderAddEvidence('dashboard-id', 'dashboards');

    expect(screen.queryByText('Add to investigation')).not.toBeInTheDocument();
  });

  it('should disabled button if no investigation is selected', async () => {
    mockUseInvestigationDrawer.mockReturnValueOnce({ selectedInvestigationId: undefined });
    renderAddEvidence('dashboard-id', 'dashboards');

    await waitFor(() => expect(screen.getByText('Add to investigation')).toBeDisabled());
  });

  it('should add a dashboard to investigation', () => {
    renderAddEvidence('dashboard-id', 'dashboards');
    const button = screen.getByText('Add to investigation');

    userEvent.click(button);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'investigation-id',
      payload: { dashboards: ['dashboard-id'] },
    });
  });

  it('should add a search to investigation', () => {
    renderAddEvidence('search-id', 'searches');
    const button = screen.getByText('Add to investigation');

    userEvent.click(button);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'investigation-id',
      payload: { searches: ['search-id'] },
    });
  });

  it('should add a event to investigation', () => {
    renderAddEvidence('event-id', 'events');
    const button = screen.getByText('Add to investigation');

    userEvent.click(button);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'investigation-id',
      payload: { events: ['event-id'] },
    });
  });

  it('should add a message to investigation', () => {
    renderAddEvidence('message-id', 'logs', 'test-index');
    const button = screen.getByText('Add to investigation');

    userEvent.click(button);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'investigation-id',
      payload: { logs: [{ id: 'message-id', index: 'test-index' }] },
    });
  });
});
