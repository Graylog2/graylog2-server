import * as React from 'react';
import { render } from 'wrappedTestingLibrary';
import { fireEvent } from '@testing-library/dom';

import history from 'util/History';
import { Button } from 'components/graylog';

import { LinkContainer } from './router';

jest.mock('util/History');

describe('LinkContainer', () => {
  it('should use component passed in children', async () => {
    const { findByRole } = render((
      <LinkContainer to="/">
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByRole('button', { name: 'All Alerts' });

    fireEvent.click(button);

    expect(history.push).toHaveBeenCalledWith('/');
  });

  it('should call onClick', async () => {
    const onClick = jest.fn();
    const { findByRole } = render((
      <LinkContainer to="/" onClick={onClick}>
        <Button bsStyle="info">All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByRole('button', { name: 'All Alerts' });

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });

  it('should call onClick of children', async () => {
    const onClick = jest.fn();
    const { findByRole } = render((
      <LinkContainer to="/">
        <Button bsStyle="info" onClick={onClick}>All Alerts</Button>
      </LinkContainer>
    ));

    const button = await findByRole('button', { name: 'All Alerts' });

    fireEvent.click(button);

    expect(onClick).toHaveBeenCalled();
  });
});
