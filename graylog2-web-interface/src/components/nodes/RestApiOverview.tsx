import React from 'react';
import styled from 'styled-components';

import RelativeTime from 'components/common/RelativeTime';

const StyledDl = styled.dl`
  margin-top: 5px;
  margin-bottom: 0;

  dt {
    float: left;
  }

  dd {
    margin-left: 150px;
  }
`;

type RestApiOverviewProps = {
  node: any;
};

const RestApiOverview = ({
  node,
}: RestApiOverviewProps) => {
  const { transport_address, last_seen } = node;

  return (
    <StyledDl>
      <dt>Transport address:</dt>
      <dd>{transport_address}</dd>
      <dt>Last seen:</dt>
      <dd><RelativeTime dateTime={last_seen} /></dd>
    </StyledDl>
  );
};

export default RestApiOverview;
