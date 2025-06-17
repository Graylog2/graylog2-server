import * as React from 'react';
import * as Immutable from 'immutable';
import { render, screen } from 'wrappedTestingLibrary';

import TextWidgetConfig from 'views/logic/widgets/TextWidgetConfig';

import OriginalTextVisualization from './TextVisualization';

const TextVisualization = ({ text }: { text: string }) => (
  <OriginalTextVisualization
    config={new TextWidgetConfig(text)}
    height={100}
    width={100}
    data={{}}
    editing={false}
    fields={Immutable.List()}
    queryId=""
    setLoadingState={() => {}}
    id=""
  />
);

describe('TextVisualization', () => {
  it('renders basic markdown', async () => {
    render(<TextVisualization text="# Hey there!" />);

    await screen.findByRole('heading', { name: 'Hey there!' });
  });

  it('renders link to open in new window', async () => {
    render(<TextVisualization text="[A link](https://www.graylog.org/)" />);

    const link = await screen.findByRole('link', { name: 'A link' });

    expect(link).toHaveAttribute('href', 'https://www.graylog.org/');
    expect(link).toHaveAttribute('target', '_blank');
    expect(link).toHaveAttribute('rel', 'noopener noreferrer');
  });
});
