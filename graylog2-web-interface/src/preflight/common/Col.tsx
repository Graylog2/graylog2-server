import * as React from 'react';
import { Grid } from '@mantine/core';

const Col = ({ children, ...props }: React.ComponentProps<typeof Grid.Col>) => {
  return (
    <Grid.Col {...props} style={{ ...props.style }}>
      {children}
    </Grid.Col>
  );
};

export default Col;
