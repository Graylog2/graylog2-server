import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/dom';

import history from 'util/History';
import { Button } from 'components/graylog';

import { LinkContainer } from './router';

jest.mock('util/History');

describe('LinkContainer', () => {
  it('should use component passed in children', async () => {
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(history.push).toHaveBeenCalledWith('/');
  });

  it('should call onClick', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/" onClick={onClick}>
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });

  it('should call onClick of children', async () => {
    const onClick = jest.fn();
    const { findByText } = render((
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('All Alerts');

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });

  it('should add target URL as href to children', async () => {
    const { findByText } = render((
      <LinkContainer to="/alerts">
        <Button bsStyle="info" onClick={jest.fn()}>Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByText('Alerts');

    expect(button.href).toEqual('http://localhost/alerts');
  });
});
