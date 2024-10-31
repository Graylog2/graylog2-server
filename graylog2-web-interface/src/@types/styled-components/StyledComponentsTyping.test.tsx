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
import styled from 'styled-components';
import { render, screen } from 'wrappedTestingLibrary';

const Foo = ({ name }: { name: string }) => <span>{name}</span>;
const StyledFoo = styled(Foo)`
  text-decoration: underline;
`;

describe('StyledComponentsTyping', () => {
  it('Make sure that component wrapping works in general', async () => {
    render(<StyledFoo name="Ruth Lichterman" />);

    await screen.findByText('Ruth Lichterman');
  });

  /*
    This test is utilizing @ts-expect-error to make sure that typing for styled-components works.
    Therefore, we are testing if missing props, wrong types or additional props are raising errors.
   */
  it('Make sure that typing works', async () => {
    // @ts-expect-error
    render(<StyledFoo />);
    // @ts-expect-error
    render(<StyledFoo name={23} />);
    // @ts-expect-error
    render(<StyledFoo name="Ruth Lichterman" missingAttribute={23} />);
    await screen.findByText('Ruth Lichterman');
  });
});
