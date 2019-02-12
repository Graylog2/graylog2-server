import React from 'react';
import { mount } from 'enzyme';

import AppErrorBoundary from './AppErrorBoundary';

jest.mock('react-router', () => ({ withRouter: x => x }));
jest.mock('!style/useable!css!./NotFoundPage.css', () => ({ use: jest.fn(), unuse: jest.fn() }));

const suppressConsole = (fn) => {
  /* eslint-disable no-console */
  const originalConsoleError = console.error;
  console.error = () => {
  };

  fn();

  console.error = originalConsoleError;
  /* eslint-enable no-console */
};

const ErroneusComponent = () => {
  // eslint-disable-next-line no-throw-literal
  throw {
    message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
    stack: 'This the stack trace.',
  };
};

const WorkingComponent = () => <div>Hello World!</div>;

describe('AppErrorBoundary', () => {
  it('registers to router upon mount', () => {
    const router = {
      listen: jest.fn(),
    };

    mount((
      <AppErrorBoundary router={router}>
        <WorkingComponent />
      </AppErrorBoundary>
    ));

    expect(router.listen).toHaveBeenCalled();
  });

  it('unregisters from router upon unmount', () => {
    const unlisten = jest.fn();
    const router = {
      listen: () => unlisten,
    };

    const wrapper = mount((
      <AppErrorBoundary router={router}>
        <WorkingComponent />
      </AppErrorBoundary>
    ));
    wrapper.unmount();

    expect(unlisten).toHaveBeenCalled();
  });

  it('displays child component if there is no error', () => {
    const router = {
      listen: jest.fn(),
    };
    const wrapper = mount((
      <AppErrorBoundary router={router}>
        <WorkingComponent />
      </AppErrorBoundary>
    ));

    expect(wrapper).toMatchSnapshot();
    expect(wrapper.find(WorkingComponent)).toHaveLength(1);
  });

  it('displays error after catching', () => {
    const router = {
      listen: jest.fn(),
    };

    suppressConsole(() => {
      const wrapper = mount((
        <AppErrorBoundary router={router}>
          <ErroneusComponent />
        </AppErrorBoundary>
      ));

      const errorPage = wrapper.find('ErrorPage');
      expect(errorPage).toExist();
      expect(errorPage).toHaveProp('error', {
        message: 'Oh no, a banana peel fell on the party gorilla\'s head!',
        stack: 'This the stack trace.',
      });
      expect(errorPage).toHaveProp('info', { componentStack: '\n    in ErroneusComponent\n    in AppErrorBoundary (created by WrapperComponent)\n    in WrapperComponent' });
    });
  });

  it('resets error when navigation changes', () => {
    const router = {
      listen: jest.fn(),
    };

    suppressConsole(() => {
      const wrapper = mount((
        <AppErrorBoundary router={router}>
          <ErroneusComponent />
        </AppErrorBoundary>
      ));

      expect(wrapper).toIncludeText('banana peel');

      wrapper.setProps({ children: <WorkingComponent /> });
      const listenCallback = router.listen.mock.calls[0][0];
      listenCallback();
      // force update of component to reflect state changes
      wrapper.update();

      expect(wrapper.find(WorkingComponent)).toHaveLength(1);
      expect(wrapper).toIncludeText('Hello World');
    });
  });
});
