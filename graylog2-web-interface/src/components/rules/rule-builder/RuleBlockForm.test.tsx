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
import { fireEvent, render, screen, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';

import RuleBlockForm from './RuleBlockForm';
import { buildRuleBlock, actionsBlockDict } from './fixtures';

const block = buildRuleBlock({
  functionName: 'substring',
  step_title: 'substring "wingardium leviosa" start "1" end "2"',
  params:
{
  value: 'wingardium leviosa',
  start: 1,
  indexEnd: 2,
},
});

const options = actionsBlockDict.map(({ name }) => ({ label: name, value: name }));

const mockAdd = jest.fn();
const mockCancel = jest.fn();
const mockSelect = jest.fn();
const mockUpdate = jest.fn();

const comp = ({
  existingBlock = undefined,
  selectedBlockDict = undefined,
} = {}) => (
  <RuleBlockForm onAdd={mockAdd}
                 onCancel={mockCancel}
                 onSelect={mockSelect}
                 onUpdate={mockUpdate}
                 options={options}
                 selectedBlockDict={selectedBlockDict}
                 existingBlock={existingBlock}
                 order={1}
                 type="action" />
);

describe('RuleBlockForm', () => {
  afterEach(() => {
    jest.clearAllMocks();
  });

  it('renders a select with all functions as options', async () => {
    render(comp());

    const select = await screen.findByRole('combobox');

    await selectEvent.openMenu(select);

    options.forEach((option) => expect(screen.getByText(option.label)).toBeInTheDocument());
  });

  it('calls onSelect handler when selecting an option', async () => {
    render(comp());

    const select = await screen.findByRole('combobox');

    await selectEvent.openMenu(select);
    await selectEvent.select(select, 'set_field');

    expect(mockSelect).toHaveBeenCalledWith('set_field');
  });

  it('shows function form and adds a block when an option is selected', async () => {
    render(comp({ selectedBlockDict: actionsBlockDict[0] }));

    expect(screen.getByRole('heading', { name: /has field/i })).toBeInTheDocument();

    const submitButton = await screen.findByRole('button', { name: /add/i });

    const fieldInput = screen.getByLabelText('field');
    fireEvent.change(fieldInput, { target: { value: 'bar' } });

    fireEvent.click(submitButton);

    await waitFor(() => expect(mockAdd).toHaveBeenCalledWith({ field: 'bar', message: undefined }));
  });

  it('continues to show function form and does not add the block when a required field is missing', async () => {
    render(comp({ selectedBlockDict: actionsBlockDict[0] }));

    expect(screen.getByRole('heading', { name: /has field/i })).toBeInTheDocument();

    const submitButton = await screen.findByRole('button', { name: /add/i });

    const requiredField = screen.getByLabelText('field');

    fireEvent.click(submitButton);

    expect(mockAdd).not.toHaveBeenCalled();

    expect(requiredField).toBeInTheDocument();
  });

  it('marks optional fields', async () => {
    render(comp({ selectedBlockDict: actionsBlockDict[4] }));

    expect(screen.getByRole('heading', { name: /get substring/i })).toBeInTheDocument();

    const optionalField = screen.getByText('indexEnd');

    expect(optionalField).toBeInTheDocument();
    expect(screen.getByText(/\(Opt\.\)/i)).toBeInTheDocument();
  });

  it('shows a help icon with function syntax help', async () => {
    render(comp({ selectedBlockDict: actionsBlockDict[4] }));

    expect(screen.getByRole('heading', { name: /get substring/i })).toBeInTheDocument();

    const helpIcon = screen.getByTestId('funcSyntaxHelpIcon');

    fireEvent.click(helpIcon);

    await screen.findByText('Function Syntax Help');
  });

  it('does not add a new block and calls onCancel handler when clicking cancel button', async () => {
    render(comp({ selectedBlockDict: actionsBlockDict[0] }));

    const cancelButton = await screen.findByRole('button', { name: /cancel/i });

    const fieldInput = screen.getByLabelText('field');
    fireEvent.change(fieldInput, { target: { value: 'bar' } });

    fireEvent.click(cancelButton);

    await waitFor(() => expect(mockCancel).toHaveBeenCalled());

    expect(mockAdd).not.toHaveBeenCalled();
  });

  it('shows and updates function form when a block is present', async () => {
    render(comp({ existingBlock: block, selectedBlockDict: actionsBlockDict[4] }));

    await screen.findByRole('heading', { name: /substring "wingardium leviosa" start "1" end "2"/i });

    const updateButton = await screen.findByRole('button', { name: /update/i });

    const fieldInput = screen.getByLabelText('value');
    fireEvent.change(fieldInput, { target: { value: 'Lumos' } });

    fireEvent.click(updateButton);

    await waitFor(() => expect(mockUpdate).toHaveBeenCalledWith({ indexEnd: 2, start: 1, value: 'Lumos' }, 'substring'));
  });

  it('displays errors when existing', async () => {
    render(comp({ existingBlock: { ...block, errors: ['wrong 1', 'not right 2'] }, selectedBlockDict: actionsBlockDict[4] }));

    expect(screen.getByText('wrong 1')).toBeInTheDocument();
    expect(screen.getByText('not right 2')).toBeInTheDocument();
  });
});
