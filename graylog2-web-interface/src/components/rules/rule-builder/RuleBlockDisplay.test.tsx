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
import { fireEvent, render, screen } from 'wrappedTestingLibrary';

import RuleBlockDisplay from './RuleBlockDisplay';
import { buildRuleBlock } from './fixtures';

const block = buildRuleBlock({
  params:
{
  spell: 'wingardium leviosa',
  wand_required: true,
  cast_time: 20,
  message: '$output_action_1',
},
});

const mockDelete = jest.fn();
const mockEdit = jest.fn();
const mockNegate = jest.fn();

describe('RuleBuilderBlock', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('uses the step_title', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    expect(screen.getByText(/to_long "foo"/i)).toBeInTheDocument();
  });

  it('displays params properly', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    expect(screen.getByText((content, _) => content.endsWith('wingardium leviosa')).textContent)
      .toEqual('spell: wingardium leviosa');

    expect(screen.getByText((content, _) => content.endsWith('true')).textContent)
      .toEqual('wand_required: true');

    expect(screen.getByText((content, _) => content.endsWith('20')).textContent)
      .toEqual('cast_time: 20');

    expect(screen.getByText((content, _) => content.endsWith('Output of the previous step')).textContent)
      .toEqual('message: Output of the previous step');
  });

  it('calls deleteHandler when clicking the delete button', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    const deleteButton = await screen.findByRole('button', { name: 'Delete' });

    fireEvent.click(deleteButton);

    expect(mockDelete).toHaveBeenCalled();
  });

  it('calls editHandler when clicking the edit button', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    const editButton = await screen.findByRole('button', { name: 'Edit' });

    fireEvent.click(editButton);

    expect(mockEdit).toHaveBeenCalled();
  });

  it('calls negateHandler when clicking the negate button', async () => {
    render(<RuleBlockDisplay block={block} negatable onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    const negationButton = await screen.findByRole('button', { name: 'Not' });

    fireEvent.click(negationButton);

    expect(mockNegate).toHaveBeenCalled();
  });
});
