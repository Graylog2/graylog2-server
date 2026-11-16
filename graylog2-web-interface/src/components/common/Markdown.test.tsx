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

const DANGEROUS_ELEMENTS = 'script, img, iframe, svg, object, embed, style:not([data-mantine-styles]), form, input, b';

/* eslint-disable testing-library/no-node-access -- asserting on elements that must not exist and have no accessible role */
const expectNoDangerousElements = (container: HTMLElement) => {
  expect(container.querySelector(DANGEROUS_ELEMENTS)).toBeNull();
  expect(container.querySelector('[onerror], [onload], [onclick]')).toBeNull();
  expect(container.querySelector('a[href^="javascript:" i]')).toBeNull();
};
/* eslint-enable testing-library/no-node-access */

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

    it('replaces custom syntax in link texts', async () => {
      render(<Markdown text="[Seen #test#yesterday#test# on fw-1](https://example.org/fw-1)" augment />);

      const link = await screen.findByRole('link', { name: /Seen yesterday on fw-1/ });

      expect(link).toHaveAttribute('href', 'https://example.org/fw-1');
      expect(screen.getByTestId('test-component')).toHaveTextContent('yesterday');
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

    it('does not replace custom #test# syntax by default', async () => {
      render(<Markdown text="This is a #test#Hello world!#test# component." augment={false} />);

      await screen.findByText('This is a #test#Hello world!#test# component.');
    });

    it('does not replace custom #test# syntax when `augment` prop is false', async () => {
      render(<Markdown text="This is a #test#Hello world!#test# component." augment={false} />);

      await screen.findByText('This is a #test#Hello world!#test# component.');
    });

    it('renders HTML inside custom syntax as text', async () => {
      const { container } = render(<Markdown text="Value: #test#<img src=x onerror=alert(1)>#test#" augment />);

      const testComponent = await screen.findByTestId('test-component');

      expect(testComponent).toHaveTextContent('<img src=x onerror=alert(1)>');
      expectNoDangerousElements(container);
    });
  });

  describe('HTML in text', () => {
    describe('shows HTML as written', () => {
      it('in inline code', async () => {
        const { container } = render(
          <Markdown text="had a script snippet (`<script>alert(document.cookie)</script>`) in the search query" />,
        );

        const code = await screen.findByText('<script>alert(document.cookie)</script>');

        expect(code.tagName).toBe('CODE');
        expectNoDangerousElements(container);
      });

      it('in fenced code blocks', async () => {
        const { container } = render(
          <Markdown
            text={
              '```\nGET /search?q=<script>alert(document.cookie)</script> HTTP/1.1 200\n<img src=x onerror=alert(1)>\n```'
            }
          />,
        );

        const code = await screen.findByText(
          /GET \/search\?q=<script>alert\(document\.cookie\)<\/script> HTTP\/1\.1 200/,
        );

        expect(code.tagName).toBe('CODE');
        expect(code).toHaveTextContent('<img src=x onerror=alert(1)>');
        expectNoDangerousElements(container);
      });

      it('in indented code blocks', async () => {
        const { container } = render(
          <Markdown text={'Request:\n\n    <iframe src="https://evil.example"></iframe>'} />,
        );

        const code = await screen.findByText('<iframe src="https://evil.example"></iframe>');

        expect(code.tagName).toBe('CODE');
        expectNoDangerousElements(container);
      });

      it('with entities in inline code', async () => {
        render(<Markdown text={'Encoded as `&lt;script&gt;` and `&amp;`'} />);

        await screen.findByText('&lt;script&gt;');
        await screen.findByText('&amp;');
      });

      it('in plain text', async () => {
        const { container } = render(<Markdown text="Use <b>bold</b> here" />);

        await screen.findByText('Use <b>bold</b> here');
        expectNoDangerousElements(container);
      });

      it('with comparison operators and ampersands', async () => {
        render(<Markdown text={'a < b > c && AT&T'} />);

        await screen.findByText('a < b > c && AT&T');
      });
    });

    describe('does not render XSS attempts', () => {
      it.each([
        ['script tag', '<script>alert(document.cookie)</script>'],
        ['img with onerror', '<img src=x onerror=alert(1)>'],
        ['svg with onload', '<svg onload=alert(1)>'],
        ['iframe', '<iframe src="javascript:alert(1)"></iframe>'],
        ['anchor with javascript URI', '<a href="javascript:alert(1)">click me</a>'],
        ['style tag', '<style>body { display: none; }</style>'],
        ['object tag', '<object data="javascript:alert(1)"></object>'],
        ['form with input', '<form action="https://evil.example"><input name="password"></form>'],
        ['div with event handler', '<div onclick="alert(1)">click me</div>'],
      ])('shows %s as text', async (_name, payload) => {
        const { container } = render(<Markdown text={`Payload: ${payload}`} />);

        await screen.findByText(`Payload: ${payload}`);
        expectNoDangerousElements(container);
      });

      it('shows multi-line HTML blocks as text', async () => {
        const payload = '<div onclick="alert(1)">\n<script>alert(1)</script>\n</div>';
        const { container } = render(<Markdown text={payload} />);

        await screen.findByText(/<div onclick="alert\(1\)">/);
        expect(container).toHaveTextContent('<script>alert(1)</script>');
        expectNoDangerousElements(container);
      });

      it('shows HTML inside markdown formatting as text', async () => {
        const { container } = render(<Markdown text="**<img src=x onerror=alert(1)>**" />);

        const strong = await screen.findByText('<img src=x onerror=alert(1)>');

        expect(strong.tagName).toBe('STRONG');
        expectNoDangerousElements(container);
      });

      it('removes javascript URIs from markdown links', async () => {
        const { container } = render(<Markdown text="[click me](javascript:alert(1))" />);

        await screen.findByText(/click me/);
        expectNoDangerousElements(container);
      });

      it('removes javascript URIs with mixed case and entities from markdown links', async () => {
        const { container } = render(<Markdown text="[a](JaVaScRiPt:alert(1)) [b](&#106;avascript:alert(1))" />);

        await screen.findByText(/a/);
        expectNoDangerousElements(container);
      });
    });

    describe('keeps markdown features', () => {
      it('renders emphasis, lists and inline code', async () => {
        render(<Markdown text={'**bold** and _italic_\n\n- first\n- second `code`'} />);

        expect((await screen.findByText('bold')).tagName).toBe('STRONG');
        expect((await screen.findByText('italic')).tagName).toBe('EM');
        expect(await screen.findAllByRole('listitem')).toHaveLength(2);
        expect((await screen.findByText('code')).tagName).toBe('CODE');
      });

      it('renders links with allowed schemes', async () => {
        render(<Markdown text="[Docs](https://docs.example.org)" />);

        const link = await screen.findByRole('link', { name: /docs/i });

        expect(link).toHaveAttribute('href', 'https://docs.example.org');
      });

      it('renders tables', async () => {
        render(<Markdown text={'| field | value |\n| --- | --- |\n| source | `<b>web</b>` |'} />);

        await screen.findByRole('table');
        expect((await screen.findByText('<b>web</b>')).tagName).toBe('CODE');
      });
    });
  });

  describe('mermaid diagrams', () => {
    const diagram = ['```mermaid', 'graph LR', '  A[Start] -->|"ok & done"| B{Is it > 1?}', '  B <--> C', '```'].join(
      '\n',
    );

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

    it('removes images from diagrams', async () => {
      asMock(mermaid.render).mockResolvedValue({
        svg: '<svg><image href="https://example.org/a.png"></image><foreignObject><div><img src="https://example.org/b.png"><span>Start</span></div></foreignObject></svg>',
        diagramType: 'flowchart',
      });

      render(<Markdown text={diagram} />);

      const svg = await screen.findByRole('img', { name: 'Diagram' });

      expect(screen.getByText('Start')).toBeInTheDocument();
      expect(svg.innerHTML).not.toMatch(/<(image|img)\b/);
    });

    it('keeps diagrams from loading external resources', async () => {
      asMock(mermaid.render).mockResolvedValue({
        svg: [
          '<svg>',
          '<style>@import "https://example.org/a.css"; .evil > * { background-image: url(https://example.org/b.png) !important; } .escaped { background: \\75rl(https://example.org/c.png); } .set { background: image-set("https://example.org/d.png" 1x); } path { marker-end: url(#arrowhead); }</style>',
          '<filter id="f"><feImage href="https://example.org/e.png"></feImage></filter>',
          '<linearGradient id="a" href="https://example.org/f.svg#shape"></linearGradient><linearGradient href="#a"></linearGradient>',
          '<rect fill="url(https://example.org/g.png)" style="fill: url(https://example.org/h.png); stroke: #333"></rect>',
          '<text>Start</text>',
          '</svg>',
        ].join(''),
        diagramType: 'flowchart',
      });

      render(<Markdown text={diagram} />);

      const svg = await screen.findByRole('img', { name: 'Diagram' });

      expect(screen.getByText('Start')).toBeInTheDocument();
      expect(svg.innerHTML).not.toMatch(/(?:url|image-set|href=)["(]*https|@import|feImage/i);
      expect(svg.innerHTML).toMatch(/marker-end: url\(#arrowhead\)/);
      expect(svg.innerHTML).toMatch(/href="#a"/);
      expect(svg.innerHTML).toMatch(/stroke: #333/);
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

    it('passes HTML and entities in diagrams on as written', async () => {
      render(<Markdown text={['```mermaid', 'graph LR', '  A["<b>&lt;tag&gt; &amp;</b>"] --> B', '```'].join('\n')} />);

      await screen.findByRole('img', { name: 'Diagram' });

      expect(mermaid.render).toHaveBeenCalledWith(
        expect.any(String),
        'graph LR\n  A["<b>&lt;tag&gt; &amp;</b>"] --> B\n',
      );
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
