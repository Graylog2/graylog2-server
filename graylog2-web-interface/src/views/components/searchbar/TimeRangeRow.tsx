import styled, { css } from 'styled-components';

const TimeRangeRow = styled.div(({ theme }) => css`
  display: flex;
  gap: 10px;
  align-items: flex-start;

  @media (max-width: ${theme.breakpoints.max.md}) {
    flex-direction: column;

    > div {
      width: 100%;
    }
  }
`);

export default TimeRangeRow;
