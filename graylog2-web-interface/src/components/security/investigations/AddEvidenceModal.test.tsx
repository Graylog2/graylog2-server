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
import userEvent from '@testing-library/user-event';

import type { EvidenceTypes } from './types';
import AddEvidenceModal from './AddEvidenceModal';

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

const mockInvestigations = [
  { id: 'inve-1' },
  { id: 'inve-2' },
  { id: 'inve-3' },
];

type Props = {
  id: string,
  type: string,
  index?: string,
};

const MockSecAddEvidenceModal = React.forwardRef(({ id, type, index }: Props, ref: React.MutableRefObject<{ toggle: () => void }>) => {
  const [showModal, setShowModal] = React.useState(false);
  const { addEvidence } = mockUseAddEvidence();

  React.useImperativeHandle(ref, () => ({
    toggle: () => setShowModal(true),
  }));

  const submitEvidence = (e: React.BaseSyntheticEvent) => {
    e.preventDefault();

    mockInvestigations.forEach((inv: { id: string }) => {
      switch (type) {
        case 'logs':
          addEvidence({
            invId: inv.id,
            payload: { logs: [{ id, index }] },
          });

          break;
        case 'dashboards':
        case 'searches':
        case 'events':
          addEvidence({
            invId: inv.id,
            payload: { [type]: [id] },
          });

          break;
        default:
          break;
      }
    });
  };

  return showModal && (
    <div>
      <span>Mock Sec Add Evidence Modal</span>
      <button type="button" onClick={submitEvidence}>Submit</button>
    </div>
  );
});

MockSecAddEvidenceModal.defaultProps = {
  index: undefined,
};

const mockExports = jest.fn((_entityKey?: string) => ([{
  components: {
    AddEvidenceModal: MockSecAddEvidenceModal,
  },
}]));

jest.mock('graylog-web-plugin/plugin', () => ({
  PluginStore: { exports: jest.fn(() => mockExports()) },
}));

const renderModal = (type: EvidenceTypes, id: string, index: string = undefined) => {
  const modalRef = React.createRef<{ toggle:() => void }>();

  render(
    <>
      <button type="button" onClick={() => modalRef.current?.toggle()}>Open Modal</button>
      <AddEvidenceModal type={type} id={id} index={index} ref={modalRef} />
    </>,
  );
};

describe('AddEvidenceModal', () => {
  it('should render modal', () => {
    renderModal('dashboards', 'dash-1');

    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));

    expect(screen.getByText('Mock Sec Add Evidence Modal')).toBeInTheDocument();
  });

  it('should not fail and render null if no plugin is registered', () => {
    mockExports.mockReturnValueOnce(undefined);
    renderModal('dashboards', 'dash-1');

    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));

    expect(screen.queryByText('Mock Sec Add Evidence Modal')).not.toBeInTheDocument();
  });

  it('should add a dashboard to the selected investigations', () => {
    renderModal('dashboards', 'dash-1');
    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));
    userEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(mockSecAddEvidence).toHaveBeenCalledTimes(3);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'inve-3',
      payload: { dashboards: ['dash-1'] },
    });
  });

  it('should add a search to the selected investigations', () => {
    renderModal('searches', 'search-1');
    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));
    userEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(mockSecAddEvidence).toHaveBeenCalledTimes(6);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'inve-3',
      payload: { searches: ['search-1'] },
    });
  });

  it('should add a event to the selected investigations', () => {
    renderModal('events', 'event-1');
    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));
    userEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(mockSecAddEvidence).toHaveBeenCalledTimes(9);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'inve-3',
      payload: { events: ['event-1'] },
    });
  });

  it('should add a message to the selected investigations', () => {
    renderModal('logs', 'message-1', 'index-1');
    userEvent.click(screen.getByRole('button', { name: 'Open Modal' }));
    userEvent.click(screen.getByRole('button', { name: 'Submit' }));

    expect(mockSecAddEvidence).toHaveBeenCalledTimes(12);

    expect(mockSecAddEvidence).toHaveBeenLastCalledWith({
      invId: 'inve-3',
      payload: { logs: [{ id: 'message-1', index: 'index-1' }] },
    });
  });
});
