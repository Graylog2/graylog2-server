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
import { RuleBuilderTypes } from './types';

const block = buildRuleBlock({
  outputvariable: 'output_5',
});

const mockDelete = jest.fn();
const mockEdit = jest.fn();
const mockNegate = jest.fn();

describe('RuleBlockDisplay', () => {
  afterEach(() => {
    jest.resetAllMocks();
  });

  it('uses the step_title', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    expect(screen.getByText(/to_long "foo"/i)).toBeInTheDocument();
  });

  it('shows the outputvariable and its return type', async () => {
    render(<RuleBlockDisplay block={block} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} returnType={RuleBuilderTypes.Number} />);

    expect(screen.getByText(/\$output_5/i)).toBeInTheDocument();

    expect(screen.getByText(/number/i)).toBeInTheDocument();
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

  it('displays errors when existing', async () => {
    render(<RuleBlockDisplay block={{ ...block, errors: ['wrong 1', 'not right 2'] }} onDelete={mockDelete} onEdit={mockEdit} onNegate={mockNegate} />);

    expect(screen.getByText('wrong 1')).toBeInTheDocument();
    expect(screen.getByText('not right 2')).toBeInTheDocument();
  });
});
