import * as React from 'react';
import { useCallback, useState } from 'react';
import styled, { css } from 'styled-components';

import IconButton from 'components/common/IconButton';

const Container = styled.div(
  ({ theme }) => css`
    margin: 5px 0;
    padding: 5px;
    background-color: ${theme.colors.background.secondaryNav};
  `,
);
const HeadlineContainer = styled.div`
  display: flex;
  flex-direction: row;
  justify-content: space-between;
`;
type Props = React.PropsWithChildren<{}>;
const EventsMetrics = ({ children = undefined }: Props) => {
  const [expanded, setExpanded] = useState(true);
  const expandTitle = `${expanded ? 'Collapse' : 'Expand'} Metrics`;
  const expandIcon = expanded ? 'unfold_less' : 'unfold_more';
  const onClick = useCallback(() => setExpanded((_expanded) => !_expanded), []);

  return (
    <Container>
      <HeadlineContainer>
        <h2>Metrics</h2>
        <IconButton title={expandTitle} name={expandIcon} onClick={onClick} />
      </HeadlineContainer>
      {expanded ? children : null}
    </Container>
  );
};
export default EventsMetrics;
