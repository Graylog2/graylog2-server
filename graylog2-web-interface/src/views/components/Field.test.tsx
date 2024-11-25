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
import { render, screen, fireEvent } from 'wrappedTestingLibrary';

import FieldType from 'views/logic/fieldtypes/FieldType';
import TestStoreProvider from 'views/test/TestStoreProvider';
import useViewsPlugin from 'views/test/testViewsPlugin';

import OriginalField from './Field';
import InteractiveContext from './contexts/InteractiveContext';

type FieldProps = { interactive: boolean } & React.ComponentProps<typeof OriginalField>;

const Field = ({ children, interactive, ...props }: FieldProps) => (
  <InteractiveContext.Provider value={interactive}>
    <TestStoreProvider>
      <OriginalField {...props}>
        {children}
      </OriginalField>
    </TestStoreProvider>
  </InteractiveContext.Provider>
);

describe('Field', () => {
  useViewsPlugin();

  describe('handles value action menu depending on interactive context', () => {
    it('does not show value actions if interactive context is `false`', async () => {
      render((
        <Field name="foo"
               interactive={false}
               queryId="someQueryId"
               type={FieldType.Unknown}>
          Foo
        </Field>
      ));

      const title = await screen.findByText('Foo');
      fireEvent.click(title);

      expect(screen.queryByText('Foo = unknown')).not.toBeInTheDocument();
    });

    it('shows value actions if interactive context is `true`', async () => {
      render((
        <Field name="foo"
               interactive
               queryId="someQueryId"
               type={FieldType.Unknown}>
          Foo
        </Field>
      ));

      const title = await screen.findByText('Foo');
      fireEvent.click(title);
      await screen.findByText('foo = unknown');
    });
  });
});
