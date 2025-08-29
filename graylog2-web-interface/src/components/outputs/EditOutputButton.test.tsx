import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import EditOutputButton from './EditOutputButton';

const mockOutput = {
  id: 'output-1',
  type: 'test-type',
  title: 'Test Output',
  configuration: { foo: 'bar' },
};

const mockTypeDefinition = {
  requested_configuration: [{ name: 'foo', type: 'string' }],
};

describe('EditOutputButton', () => {
  it('shows configuration form modal on click', async () => {
    const getTypeDefinition = jest.fn((_type, cb) => {
      cb(mockTypeDefinition);
    });

    render(<EditOutputButton output={mockOutput} getTypeDefinition={getTypeDefinition} />);

    userEvent.click(await screen.findByRole('button', { name: /edit/i }));

    expect(getTypeDefinition).toHaveBeenCalledWith('test-type', expect.any(Function));

    await screen.findByRole('heading', { name: /Editing Output Test Output/i });
    userEvent.click(await screen.findByRole('button', { name: /update output/i }));
  });

  it('calls onUpdate and closes modal form on submit', async () => {
    const getTypeDefinition = jest.fn((_type, cb) => {
      cb(mockTypeDefinition);
    });
    const onUpdate = jest.fn();
    render(<EditOutputButton output={mockOutput} getTypeDefinition={getTypeDefinition} onUpdate={onUpdate} />);

    userEvent.click(await screen.findByRole('button', { name: /edit/i }));
    userEvent.click(await screen.findByRole('button', { name: /update output/i }));

    expect(onUpdate).toHaveBeenCalledWith(mockOutput, expect.anything());

    expect(screen.queryByRole('heading', { name: /Editing Output Test Output/i })).not.toBeInTheDocument();
  });

  it('closes modal form on cancel', async () => {
    const getTypeDefinition = jest.fn((_type, cb) => {
      cb(mockTypeDefinition);
    });
    const onUpdate = jest.fn();
    render(<EditOutputButton output={mockOutput} getTypeDefinition={getTypeDefinition} onUpdate={onUpdate} />);

    userEvent.click(await screen.findByRole('button', { name: /edit/i }));

    await screen.findByRole('heading', { name: /Editing Output Test Output/i });
    userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    expect(onUpdate).not.toHaveBeenCalled();
    expect(screen.queryByRole('heading', { name: /Editing Output Test Output/i })).not.toBeInTheDocument();
  });
});
