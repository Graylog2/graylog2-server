import * as React from 'react';
import { render, screen } from 'wrappedTestingLibrary';

import WidgetConfigForm from 'views/components/aggregationwizard/WidgetConfigForm';
import AggregationWidgetConfig from 'views/logic/aggregationbuilder/AggregationWidgetConfig';

import SortConfiguration from './SortConfiguration';

describe('SortConfiguration', () => {
  it('should show empty field for new sorts', async () => {
    const onSubmit = jest.fn();
    const validate = jest.fn();
    const config = AggregationWidgetConfig.builder().build();

    render((
      <WidgetConfigForm initialValues={{ sort: [{ id: 'foobar' }] }}
                        onSubmit={onSubmit}
                        validate={validate}
                        config={config}>
        <SortConfiguration index={0} />
      </WidgetConfigForm>
    ));

    await screen.findByLabelText('Select field for sorting');

    expect(screen.queryByText('-1')).not.toBeInTheDocument();
  });
});
