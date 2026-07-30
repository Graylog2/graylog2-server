import React from 'react';
import { styled, css } from 'styled-components';

import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

const StyledWrapper = styled.div<{ $align?: 'right' }>(
  ({ $align }) => css`
    display: flex;
    cursor: pointer;
    height: 100%;
    width: 100%;
    align-items: center;
    justify-content: ${$align === 'right' ? 'flex-end' : 'flex-start'};
  `,
);

type Props = React.PropsWithChildren<{
  id: string;
  align?: 'right';
  section: string;
}>;

const ExpandedSectionToggleWrapper = ({ id, section, align = undefined, children = undefined }: Props) => {
  const { toggleSection } = useExpandedSections();
  const _toggleSection = () => toggleSection(id, section);

  return (
    <StyledWrapper title="Show details" onClick={_toggleSection} $align={align}>
      {children}
    </StyledWrapper>
  );
};

export default ExpandedSectionToggleWrapper;
