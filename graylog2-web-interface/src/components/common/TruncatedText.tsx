import * as React from 'react';
import { Text} from '@mantine/core';

const TruncatedText = ({ children, expanded } : { children: React.ReactNode, expanded: boolean }) => (
  <Text truncate={expanded ? undefined : 'end'}>
    {children}
</Text>
)

export default TruncatedText;