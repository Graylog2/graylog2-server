import React from 'react';
import { styled } from 'styled-components';

import useExpandedSections from 'components/common/EntityDataTable/hooks/useExpandedSections';

const StyledWrapper = styled.div`
  cursor: pointer;
`;

type Props = React.PropsWithChildren<{
  id: string;
}>;

const ExpandedSectionToggleWrapper = ({ id, children = undefined }: Props) => {
  const { toggleSection } = useExpandedSections();
  const _toggleSection = () => toggleSection(id, 'configuration');

  return (
    <StyledWrapper title="show details" onClick={_toggleSection}>
      {children}
    </StyledWrapper>
  );
};

export default ExpandedSectionToggleWrapper;
