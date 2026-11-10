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
import type { Stream } from 'logic/streams/types';
import asMock from 'helpers/mocking/AsMock';
import usePermissions from 'hooks/usePermissions';

import PipelineConnectionsForm from './PipelineConnectionsForm';

jest.mock('hooks/usePermissions');

const pipeline: PipelineType = { ...DEFAULT_PIPELINE, id: 'pipeline-id', title: 'Pipeline' };
const streams = [
  { id: 'stream-a', title: 'Stream A' },
  { id: 'stream-b', title: 'Stream B' },
] as Stream[];
const connection = (streamId: string) => ({ id: `connection-${streamId}`, stream_id: streamId, pipeline_ids: [pipeline.id] });

const openForm = async () => userEvent.click(await screen.findByRole('button', { name: /edit connections/i }));
const submit = async () => userEvent.click(await screen.findByRole('button', { name: /update connections/i }));

describe('PipelineConnectionsForm', () => {
  beforeEach(() => {
    asMock(usePermissions).mockReturnValue({ isPermitted: () => true, isAnyPermitted: () => true });
  });

  it('discards cancelled edits', async () => {
    const save = jest.fn();
    render(
      <PipelineConnectionsForm
        pipeline={pipeline}
        connections={[connection('stream-a')]}
        streams={streams}
        save={save}
        disableEdit={false}
      />,
    );

    await openForm();
    await userEvent.click(await screen.findByRole('button', { name: /remove/i }));
    await userEvent.click(await screen.findByRole('button', { name: /cancel/i }));

    await openForm();
    await submit();

    expect(save).toHaveBeenCalledWith({ pipeline: pipeline.id, streams: ['stream-a'] }, expect.any(Function));
  });

  it('starts from the current connections when reopened', async () => {
    const save = jest.fn();
    const props = { pipeline, streams, save, disableEdit: false };
    const { rerender } = render(<PipelineConnectionsForm {...props} connections={[connection('stream-a')]} />);

    rerender(<PipelineConnectionsForm {...props} connections={[connection('stream-a'), connection('stream-b')]} />);

    await openForm();
    await submit();

    expect(save).toHaveBeenCalledWith(
      { pipeline: pipeline.id, streams: ['stream-a', 'stream-b'] },
      expect.any(Function),
    );
  });
});
