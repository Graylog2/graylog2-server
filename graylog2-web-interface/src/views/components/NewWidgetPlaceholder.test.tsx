import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';
import userEvent from '@testing-library/user-event';

import WidgetPosition from 'views/logic/widgets/WidgetPosition';

import NewWidgetPlaceholder from './NewWidgetPlaceholder';

describe('NewWidgetPlaceholder', () => {
  const widgetPosition = WidgetPosition.builder()
    .col(3)
    .row(3)
    .height(4)
    .width(8)
    .build();

  it('shows helpful text when rendered', async () => {
    render(<NewWidgetPlaceholder position={widgetPosition} component={() => <></>} />);
    await screen.findByText('Create a new widget here');
  });

  it('renders custom component when clicked', async () => {
    const component = () => <>Hey there!</>;
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    await screen.findByText('Hey there!');
  });

  it('passes position to custom component', async () => {
    const component = jest.fn(() => <>Hey there!</>);
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    await screen.findByText('Hey there!');

    expect(component).toHaveBeenCalledWith(expect.objectContaining({ position: widgetPosition }), expect.anything());
  });

  it('unmounts custom component after calling `onCancel`', async () => {
    const component = ({ onCancel }) => <button onClick={onCancel}>Close</button>;
    render(<NewWidgetPlaceholder position={widgetPosition} component={component} />);
    const text = await screen.findByText('Create a new widget here');
    userEvent.click(text);

    const close = await screen.findByRole('button', { name: 'Close' });
    userEvent.click(close);

    expect(screen.queryByRole('button', { name: 'Close' })).not.toBeInTheDocument();
  });
});
