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

import type { PipelineType, StageType } from 'components/pipelines/types';
import { DEFAULT_PIPELINE } from 'components/pipelines/types';
import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';
import { useRules } from 'components/rules/hooks/useRules';

import StageForm from './StageForm';

jest.mock('hooks/usePermissions');
jest.mock('components/rules/hooks/useRules', () => ({
  ...jest.requireActual('components/rules/hooks/useRules'),
  useRules: jest.fn(),
}));

const stage: StageType = { stage: 0, match: 'EITHER', rules: ['rule-a'] };
const pipeline: PipelineType = { ...DEFAULT_PIPELINE, id: 'pipeline-id', title: 'Pipeline', stages: [stage] };

const openForm = async () => userEvent.click(await screen.findByRole('button', { name: /^edit$/i }));

describe('StageForm', () => {
  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true, isAdmin: false });
    asMock(useRules).mockReturnValue({ data: [], isLoading: false } as unknown as ReturnType<typeof useRules>);
  });

  it('discards cancelled edits', async () => {
    const save = jest.fn();
    render(<StageForm pipeline={pipeline} stage={stage} save={save} />);

    await openForm();
    await userEvent.click(await screen.findByLabelText(/all rules on this stage match/i));
    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    await openForm();
    await userEvent.click(await screen.findByRole('button', { name: /update stage/i }));

    expect(save).toHaveBeenCalledWith(expect.objectContaining({ match: 'EITHER' }), expect.any(Function));
  });

  it('starts from the current stage when reopened', async () => {
    const save = jest.fn();
    const updatedStage: StageType = { ...stage, rules: ['rule-a', 'rule-b'] };
    const { rerender } = render(<StageForm pipeline={pipeline} stage={stage} save={save} />);

    rerender(<StageForm pipeline={{ ...pipeline, stages: [updatedStage] }} stage={updatedStage} save={save} />);

    await openForm();
    await userEvent.click(await screen.findByRole('button', { name: /update stage/i }));

    expect(save).toHaveBeenCalledWith(expect.objectContaining({ rules: ['rule-a', 'rule-b'] }), expect.any(Function));
  });
});
