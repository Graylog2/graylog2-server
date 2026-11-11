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
import { render, screen, waitFor } from 'wrappedTestingLibrary';
import mermaid from 'mermaid';

import { usePluginExports } from 'views/test/testPlugins';
import asMock from 'helpers/mocking/AsMock';
import MarkdownRenderTracker from 'components/common/markdown/MarkdownRenderTracker';

import Markdown from './Markdown';

jest.mock('mermaid', () => ({
  __esModule: true,
  default: { initialize: jest.fn(), render: jest.fn() },
}));

describe('Markdown', () => {
  // We are skipping this test, until we can implement a headless theme provider from @mantine/core
  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('renders `undefined`', () => {
    const { container } = render(<Markdown text={undefined} />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  // We are skipping this test, until we can implement a headless theme provider from @mantine/core
  // eslint-disable-next-line jest/no-disabled-tests
  it.skip('renders empty string', () => {
    const { container } = render(<Markdown text="" />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  it('renders simple markdown', async () => {
    render(<Markdown text="# Title" />);

    await screen.findByRole('heading', { name: /title/i });
  });

  describe('supports extended syntax', () => {
    usePluginExports({
      'markdown.augment.components': [
        {
          id: 'test',
          component: ({ value }) => <span data-testid="test-component">{value}</span>,
        },
      ],
    });

    it('replaces custom #test# syntax', async () => {
      render(<Markdown text="This is a #test#Hello world!#test# component." augment />);

      const testComponent = await screen.findByTestId('test-component');

      expect(testComponent).toHaveTextContent('Hello world!');
    });

    it('replaces custom syntax and renders links inside tables', async () => {
      render(
        <Markdown
          text={'| Host | Seen |\n| --- | --- |\n| [fw-1](https://example.org/fw-1) | #test#yesterday#test# |'}
          augment
        />,
      );

      await screen.findByRole('table');

      expect(screen.getByRole('columnheader', { name: 'Host' })).toBeInTheDocument();
      expect(screen.getByRole('link', { name: /fw-1/ })).toHaveAttribute('href', 'https://example.org/fw-1');
      expect(screen.getByTestId('test-component')).toHaveTextContent('yesterday');
    });

    it('replaces custom syntax in link texts', async () => {
      render(<Markdown text="[Seen #test#yesterday#test# on fw-1](https://example.org/fw-1)" augment />);

      const link = await screen.findByRole('link', { name: /Seen yesterday on fw-1/ });

      expect(link).toHaveAttribute('href', 'https://example.org/fw-1');
      expect(screen.getByTestId('test-component')).toHaveTextContent('yesterday');
    });

    it('does not replace custom #test# syntax by default', async () => {
      render(<Markdown text="This is a #test#Hello world!#test# component." augment={false} />);

      await screen.findByText('This is a #test#Hello world!#test# component.');
    });

    it('does not replace custom #test# syntax when `augment` prop is false', async () => {
      render(<Markdown text="This is a #test#Hello world!#test# component." augment={false} />);

      await screen.findByText('This is a #test#Hello world!#test# component.');
    });
  });

  describe('mermaid diagrams', () => {
    const diagram = ['```mermaid', 'graph LR', '  A[Start] -->|"ok & done"| B{Is it > 1?}', '  B <--> C', '```'].join('\n');

    beforeEach(() => {
      asMock(mermaid.render).mockReset();
      asMock(mermaid.render).mockResolvedValue({
        svg: '<svg><g><text>Start</text></g><script>alert(1)</script></svg>',
        diagramType: 'flowchart',
      });
    });

    it('shows code blocks in the language "mermaid" as diagrams', async () => {
      render(<Markdown text={diagram} />);

      const svg = await screen.findByRole('img', { name: 'Diagram' });

      expect(mermaid.render).toHaveBeenCalledWith(
        expect.any(String),
        'graph LR\n  A[Start] -->|"ok & done"| B{Is it > 1?}\n  B <--> C\n',
      );
      expect(screen.getByText('Start')).toBeInTheDocument();
      expect(svg).not.toContainHTML('<script>');
    });

    it('shows the source with the error when a diagram is invalid, and removes what mermaid left behind', async () => {
      asMock(mermaid.render).mockImplementation((id: string) => {
        const leftover = document.createElement('div');
        leftover.id = `d${id}`;
        leftover.textContent = 'Syntax error in text';
        document.body.appendChild(leftover);

        return Promise.reject(new Error('Parse error on line 2'));
      });

      render(<Markdown text={diagram} />);

      await screen.findByText(/could not be rendered: parse error on line 2/i);
      expect(screen.getByText(/graph LR/)).toBeInTheDocument();
      expect(screen.queryByText('Syntax error in text')).not.toBeInTheDocument();
    });

    it('keeps other code blocks', async () => {
      render(<Markdown text={['```json', '{"a": 1}', '```'].join('\n')} />);

      await screen.findByText('{"a": 1}');
      expect(mermaid.render).not.toHaveBeenCalled();
    });

    it('keeps a tracker waiting until the diagram is rendered', async () => {
      let finishDiagram: (result: { svg: string; diagramType: string }) => void;
      asMock(mermaid.render).mockReturnValue(
        new Promise((resolve) => {
          finishDiagram = resolve;
        }),
      );
      let pending = 0;
      const track = jest.fn(() => {
        pending += 1;

        return () => {
          pending -= 1;
        };
      });

      render(
        <MarkdownRenderTracker.Provider value={track}>
          <Markdown text={diagram} />
        </MarkdownRenderTracker.Provider>,
      );

      await waitFor(() => expect(mermaid.render).toHaveBeenCalled());
      expect(pending).toBe(1);

      finishDiagram({ svg: '<svg><text>Start</text></svg>', diagramType: 'flowchart' });

      await screen.findByRole('img', { name: 'Diagram' });
      expect(pending).toBe(0);
      expect(track).toHaveBeenCalled();
    });
  });
});
