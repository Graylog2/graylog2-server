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
// @flow strict
import React, { useContext, useEffect } from 'react';

import { Jumbotron, Button } from 'components/graylog';
import RenderCompletionCallback from 'views/components/widgets/RenderCompletionCallback';

import InteractiveContext from '../contexts/InteractiveContext';

type Props = {
  toggleEdit: () => void,
  editing: boolean,
};

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
    <Jumbotron>
      <h2>Empty Aggregation</h2>
      <br />
      {text}
    </Jumbotron>
  );
};

export default EmptyAggregationContent;
