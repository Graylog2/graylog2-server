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

import ExpandedOutputsSection from 'components/streams/StreamsOverview/ExpandedOutputsSection';
import { stream } from 'fixtures/streams';
import { asMock } from 'helpers/mocking';
import useStreamOutputs from 'hooks/useStreamOutputs';

jest.mock('hooks/useStreamOutputs');

const buildOutput = (id: string, title: string, type = 'org.graylog2.outputs.GelfOutput') => ({
  id,
  title,
  type,
  configuration: {},
});

const mockStreamOutputs = (outputs: ReturnType<typeof buildOutput>[]) => {
  asMock(useStreamOutputs).mockReturnValue({
    data: { outputs, total: outputs.length },
    refetch: () => {},
    isInitialLoading: false,
    isError: false,
  });
};

describe('ExpandedOutputsSection', () => {
  it('links each output to the destinations segment with edit_output for that output', async () => {
    mockStreamOutputs([buildOutput('out-1', 'Graylog GELF output'), buildOutput('out-2', 'Syslog forwarder')]);

    render(<ExpandedOutputsSection stream={stream} />);

    const gelfLink = await screen.findByRole('link', { name: /graylog gelf output/i });
    const syslogLink = await screen.findByRole('link', { name: /syslog forwarder/i });

    expect(gelfLink).toHaveAttribute(
      'href',
      `/streams/${stream.id}/view?segment=destinations&edit_output=out-1`,
    );
    expect(syslogLink).toHaveAttribute(
      'href',
      `/streams/${stream.id}/view?segment=destinations&edit_output=out-2`,
    );
  });

  it('renders the output type alongside the title', async () => {
    mockStreamOutputs([buildOutput('out-1', 'GELF', 'org.graylog2.outputs.GelfOutput')]);

    render(<ExpandedOutputsSection stream={stream} />);

    await screen.findByText(/\(org\.graylog2\.outputs\.GelfOutput\)/);
  });

  it('renders a summary line with the count of connected outputs', async () => {
    mockStreamOutputs([buildOutput('out-1', 'A'), buildOutput('out-2', 'B'), buildOutput('out-3', 'C')]);

    render(<ExpandedOutputsSection stream={stream} />);

    await screen.findByText(/3 connected outputs\./i);
  });

  it('uses the singular form when only one output is connected', async () => {
    mockStreamOutputs([buildOutput('out-1', 'Only output')]);

    render(<ExpandedOutputsSection stream={stream} />);

    await screen.findByText(/1 connected output\./i);
  });

  it('renders outputs sorted by title', async () => {
    mockStreamOutputs([buildOutput('out-1', 'Zeta'), buildOutput('out-2', 'alpha'), buildOutput('out-3', 'Mu')]);

    render(<ExpandedOutputsSection stream={stream} />);

    const links = await screen.findAllByRole('link');

    expect(links.map((link) => link.textContent)).toEqual(['alpha', 'Mu', 'Zeta']);
  });

  it('shows a spinner while loading', () => {
    asMock(useStreamOutputs).mockReturnValue({
      data: undefined,
      refetch: () => {},
      isInitialLoading: true,
      isError: false,
    });

    render(<ExpandedOutputsSection stream={stream} />);

    expect(screen.getByText(/loading/i)).toBeInTheDocument();
  });
});
