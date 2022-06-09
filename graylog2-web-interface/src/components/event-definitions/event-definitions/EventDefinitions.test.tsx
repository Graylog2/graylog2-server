import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import EventDefinitions from './EventDefinitions';

const SUT = (props: Partial<React.ComponentProps<typeof EventDefinitions>>) => (
  <EventDefinitions onDisable={jest.fn()}
                    onDelete={jest.fn()}
                    onCopy={jest.fn()}
                    onEnable={jest.fn()}
                    onPageChange={jest.fn()}
                    query="*"
                    onQueryChange={jest.fn()}
                    pagination={{
                      grandTotal: 0,
                      page: 1,
                      pageSize: 10,
                      total: 0,
                    }}
                    eventDefinitions={[]}
                    {...props} />
);

describe('EventDefinitions', () => {
  it('renders special dialog when no event definitions are present', async () => {
    render(<SUT />);

    await screen.findByText('Looks like there is nothing here, yet!');
  });
});
