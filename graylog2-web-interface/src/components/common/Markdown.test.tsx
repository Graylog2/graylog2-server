import * as React from 'react';
import { render } from 'wrappedTestingLibrary';

import Markdown from './Markdown';

describe('Markdown', () => {
  it('renders `undefined`', () => {
    const { container } = render(<Markdown text={undefined} />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  it('renders empty string', () => {
    const { container } = render(<Markdown text="" />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div />
      </div>
    `);
  });

  it('renders simple markdown', () => {
    const { container } = render(<Markdown text="# Title" />);

    expect(container).toMatchInlineSnapshot(`
      <div>
        <div>
          <h1>
            Title
          </h1>
          
      
        </div>
      </div>
    `);
  });
});
