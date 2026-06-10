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
import { render, screen } from 'wrappedTestingLibrary';

import { asMock } from 'helpers/mocking';
import useInputStateSummary from 'hooks/useInputStateSummary';

import InputsDotBadge from './InputsDotBadge';

jest.mock('hooks/useInputStateSummary');

const TEXT = 'Inputs';

describe('<InputsDotBadge />', () => {
  beforeEach(() => {
    jest.clearAllMocks();
  });

  it('renders text while loading', () => {
    asMock(useInputStateSummary).mockReturnValue({
      hasProblematicInputs: false,
      isLoading: true,
    });

    render(<InputsDotBadge text={TEXT} />);

    expect(screen.getByText(TEXT)).toBeInTheDocument();
  });

  it('renders plain text when there are no problematic inputs', () => {
    asMock(useInputStateSummary).mockReturnValue({
      hasProblematicInputs: false,
      isLoading: false,
    });

    render(<InputsDotBadge text={TEXT} />);

    const textEl = screen.getByText(TEXT);
    expect(textEl).toBeInTheDocument();
    expect(textEl).not.toHaveAttribute('title');
  });

  it('shows badge when there are problematic inputs', () => {
    asMock(useInputStateSummary).mockReturnValue({
      hasProblematicInputs: true,
      isLoading: false,
    });

    render(<InputsDotBadge text={TEXT} />);

    const badge = screen.getByTitle(/Some inputs are in failed state or in setup mode\./i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent(TEXT);
  });

  it('shows badge when hasExternalIssues is true and no problematic inputs', () => {
    asMock(useInputStateSummary).mockReturnValue({
      hasProblematicInputs: false,
      isLoading: false,
    });

    render(<InputsDotBadge text={TEXT} hasExternalIssues externalIssuesTitle="Forwarder inputs have issues." />);

    const badge = screen.getByTitle(/Forwarder inputs have issues\./i);
    expect(badge).toBeInTheDocument();
    expect(badge).toHaveTextContent(TEXT);
  });

  it('shows failed inputs title when both problematic inputs and external issues exist', () => {
    asMock(useInputStateSummary).mockReturnValue({
      hasProblematicInputs: true,
      isLoading: false,
    });

    render(<InputsDotBadge text={TEXT} hasExternalIssues externalIssuesTitle="Forwarder inputs have issues." />);

    const badge = screen.getByTitle(/Some inputs are in failed state or in setup mode\./i);
    expect(badge).toBeInTheDocument();
  });
});
