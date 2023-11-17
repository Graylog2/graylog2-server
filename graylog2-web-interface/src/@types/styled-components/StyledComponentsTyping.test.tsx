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
