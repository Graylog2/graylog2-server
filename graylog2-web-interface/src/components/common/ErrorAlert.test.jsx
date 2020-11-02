// @flow strict
import React from 'react';
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import ErrorAlert from './ErrorAlert';

describe('ErrorAlert', () => {
  it('should display an Error', () => {
    render(<ErrorAlert>Franz</ErrorAlert>);

    expect(screen.queryByText('Franz')).not.toBeNull();
    expect(screen.queryByText('Runtime Error')).toBeNull();
  });

  it('should display an Runtime Error', () => {
    render(<ErrorAlert runtimeError>Franz</ErrorAlert>);

    expect(screen.queryByText('Franz')).not.toBeNull();
    expect(screen.queryByText('Runtime Error')).not.toBeNull();
  });

  it('should display nothing without children', () => {
    render(<ErrorAlert />);

    expect(screen.queryByText('Franz')).toBeNull();
    expect(screen.queryByText('Runtime Error')).toBeNull();
  });

  it('should call onClose handler', () => {
    const onClose = jest.fn();
    render(<ErrorAlert onClose={onClose}>Franz</ErrorAlert>);

    const closeBtn = screen.getByRole('button');

    fireEvent.click(closeBtn);

    expect(onClose).toHaveBeenCalled();
  });
});
