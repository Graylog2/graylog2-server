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
import userEvent from '@testing-library/user-event';
import { render, screen } from 'wrappedTestingLibrary';

import type { PipelineType } from 'components/pipelines/types';
import { DEFAULT_PIPELINE } from 'components/pipelines/types';
import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';

import PipelineForm from './PipelineForm';

jest.mock('hooks/usePermissions');

const pipeline: PipelineType = {
  ...DEFAULT_PIPELINE,
  id: 'pipeline-id',
  title: 'Original title',
  description: 'Original description',
  stages: [{ stage: 0, match: 'EITHER', rules: ['rule-a'] }],
};

const pipelineWithNewStage: PipelineType = {
  ...pipeline,
  stages: [...pipeline.stages, { stage: 1, match: 'ALL', rules: ['rule-b', 'rule-c'] }],
};

const openForm = async () => userEvent.click(await screen.findByRole('button', { name: /edit pipeline details/i }));

const replaceValue = async (label: RegExp, value: string) => {
  const input = await screen.findByLabelText(label);

  await userEvent.clear(input);
  await userEvent.type(input, value);
};

describe('PipelineForm', () => {
  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
  });

  it('keeps stages added after the form was first rendered', async () => {
    const save = jest.fn();
    const { rerender } = render(<PipelineForm pipeline={pipeline} save={save} />);

    rerender(<PipelineForm pipeline={pipelineWithNewStage} save={save} />);

    await openForm();
    await replaceValue(/^title$/i, 'New title');
    await userEvent.click(await screen.findByRole('button', { name: /update pipeline/i }));

    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'New title', stages: pipelineWithNewStage.stages }),
      expect.any(Function),
    );
  });

  it('discards cancelled edits', async () => {
    const save = jest.fn();
    render(<PipelineForm pipeline={pipeline} save={save} />);

    await openForm();
    await replaceValue(/^title$/i, 'Cancelled title');
    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    await openForm();

    expect(await screen.findByLabelText(/^title$/i)).toHaveValue('Original title');

    await replaceValue(/^description$/i, 'New description');
    await userEvent.click(await screen.findByRole('button', { name: /update pipeline/i }));

    expect(save).toHaveBeenCalledWith(
      expect.objectContaining({ title: 'Original title', description: 'New description' }),
      expect.any(Function),
    );
  });
});
