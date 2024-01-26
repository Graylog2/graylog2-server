import * as React from 'react';
import { Tooltip as MantineTooltip } from '@mantine/core';
import { useTheme } from 'styled-components';

type Props = React.ComponentProps<typeof MantineTooltip>;

const Tooltip = (props: Props) => {
  const theme = useTheme();
  const styles = () => ({
    tooltip: {
      backgroundColor: theme.colors.global.contentBackground,
      color: theme.colors.global.textDefault,
      fontWeight: 400,
      fontSize: theme.fonts.size.root,
    },
  });

  return <MantineTooltip styles={styles} {...props} />;
};

export default Tooltip;
