import * as React from 'react';
import { Grid } from '@mantine/core';

const Row = ({ children, ...props }: React.ComponentProps<typeof Grid>) => (
  <Grid {...props}>
    {children}
  </Grid>
);

export default Row;
