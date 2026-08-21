import styled, { css } from 'styled-components';

const SectionHeader = styled.div(
  ({ theme }) => css`
    display: flex;
    align-items: center;
    justify-content: space-between;
    margin: ${theme.spacings.lg} 0 ${theme.spacings.xs};
  `,
);

export default SectionHeader;
