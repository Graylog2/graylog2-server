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
import { act, render, screen, waitFor } from 'wrappedTestingLibrary';
import selectEvent from 'react-select-event';
import userEvent from '@testing-library/user-event';

import { indexSets } from 'fixtures/indexSets';
import { stream } from 'fixtures/streams';

import StreamModal from './StreamModal';

const exampleStream = {
  ...stream,
  title: 'Stream Title',
  description: 'Stream Description',
  index_set_id: indexSets[0].id,
};

const SUT = (props: Partial<React.ComponentProps<typeof StreamModal>>) => (
  <StreamModal onSubmit={() => Promise.resolve()}
               onClose={() => {}}
               indexSets={indexSets}
               submitButtonText="Submit"
               submitLoadingText="Submitting..."
               title="Bach"
               {...props} />
);

describe('StreamModal', () => {
  it('should render without provided stream', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    const onClose = jest.fn();
    render(<SUT onSubmit={onSubmit} onClose={onClose} />);

    await screen.findByRole('textbox', {
      name: /title/i,
      hidden: true,
    });

    await screen.findByRole('textbox', {
      name: /description/i,
      hidden: true,
    });
  });

  it('should update stream', async () => {
    const onSubmit = jest.fn(() => Promise.resolve());
    render(<SUT initialValues={exampleStream} onSubmit={onSubmit} />);

    const title = await screen.findByRole('textbox', {
      name: /title/i,
      hidden: true,
    });

    const description = await screen.findByRole('textbox', {
      name: /description/i,
      hidden: true,
    });

    const indexSetSelect = await screen.findByLabelText('Index Set');

    expect(title).toHaveValue(exampleStream.title);
    expect(description).toHaveValue(exampleStream.description);

    await userEvent.type(title, ' and further title');
    await userEvent.type(description, ' and further description');

    await act(async () => {
      await selectEvent.openMenu(indexSetSelect);
    });

    await act(async () => {
      await selectEvent.select(indexSetSelect, 'Example Index Set');
    });

    await screen.findByText('Example Index Set');

    const submitButton = await screen.findByRole('button', {
      name: /submit/i,
      hidden: true,
    });

    await waitFor(() => {
      expect(submitButton).not.toBeDisabled();
    });

    // eslint-disable-next-line testing-library/no-unnecessary-act
    await act(async () => {
      await userEvent.click(submitButton);
    });

    await waitFor(() => expect(onSubmit).toHaveBeenCalledWith({
      description: 'Stream Description and further description',
      index_set_id: 'index-set-id-2',
      remove_matches_from_default_stream: false,
      title: 'Stream Title and further title',
    }));
  });
});
