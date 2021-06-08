/*
 * Copyright (C) 2020 Graylog, Inc.
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the Server Side Public License, version 1,
 * as published by MongoDB, Inc.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * Server Side Public License for more details.
 *
 * You should have received a copy of the Server Side Public License
 * along with this program. If not, see
 * <http://www.mongodb.com/licensing/server-side-public-license>.
 */
import React, { useContext, useEffect } from 'react';
import styled, { css } from 'styled-components';

import { Button } from 'components/graylog';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

import InteractiveContext from '../contexts/InteractiveContext';

type Props = {
  toggleEdit: () => void,
  editing: boolean,
};

const Container = styled.div`
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: inherit;
`;

const SpacedHeading = styled.h2(({ theme }) => css`
  margin-bottom: ${theme.spacings.sm};
`);

const EmptyAggregationContent = ({ toggleEdit, editing = false }: Props) => {
  const onRenderComplete = useContext(RenderCompletionCallback);

  useEffect(() => {
    if (onRenderComplete) {
      onRenderComplete();
    }
  }, [onRenderComplete]);

  const interactive = useContext(InteractiveContext);
  const text = editing
    ? (
      <p>You are now editing the widget.<br />
        To see results, add at least one metric. You can group data by adding rows/columns.<br />
        To finish, click &quot;Save&quot; to save, &quot;Cancel&quot; to abandon changes.
      </p>
    )
    : (<p>Please {interactive ? <Button bsStyle="info" onClick={toggleEdit}>Edit</Button> : 'edit'} the widget to see results here.</p>);

  return (
    <Container>
      <div>
        <SpacedHeading>Empty Aggregation</SpacedHeading>

        {text}
      </div>
    </Container>
  );
};

export default EmptyAggregationContent;
