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
import React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import ExpandedOutputsActions from 'components/streams/StreamsOverview/ExpandedOutputsActions';
import { stream } from 'fixtures/streams';

describe('ExpandedOutputsActions', () => {
  it('links Manage Outputs to the Destinations segment of the stream details page', async () => {
    render(<ExpandedOutputsActions stream={stream} />);

    const manageOutputsLink = await screen.findByRole('link', { name: /manage outputs/i });

    expect(manageOutputsLink).toHaveAttribute('href', `/streams/${stream.id}/view?segment=destinations`);
  });

  it('disables the button when the stream is the default stream', async () => {
    render(<ExpandedOutputsActions stream={{ ...stream, is_default: true }} />);

    const manageOutputsButton = await screen.findByRole('button', { name: /manage outputs/i });

    expect(manageOutputsButton).toBeDisabled();
  });

  it('disables the button when the stream is not editable', async () => {
    render(<ExpandedOutputsActions stream={{ ...stream, is_editable: false }} />);

    const manageOutputsButton = await screen.findByRole('button', { name: /manage outputs/i });

    expect(manageOutputsButton).toBeDisabled();
  });
});
