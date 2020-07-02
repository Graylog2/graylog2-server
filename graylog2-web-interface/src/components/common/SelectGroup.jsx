// @flow strict
import * as React from 'react';
import styled, { type StyledComponent } from 'styled-components';

import { type ThemeInterface } from 'theme';

type Props = {
  children: React.Node,
  className?: string,
};

const Conainter: StyledComponent<{}, ThemeInterface, HTMLDivElement> = styled.div`
  display: flex;

  > div:first-child > div {
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
  }

  > div:last-child > div {
    border-top-left-radius: 0;
    border-bottom-left-radius: 0;
  }

  > div:not(:first-child) > div {
    border-left: 0;
  }

  > div:not(:first-child):not(:last-child) > div {
    border-radius: 0;
  }
`;

const SelectGroup = ({ children, className }: Props) => <Conainter className={className}>{children}</Conainter>;

SelectGroup.defaultProps = {
  className: undefined,
};

export default SelectGroup;
