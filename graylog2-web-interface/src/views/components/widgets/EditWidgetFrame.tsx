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
import * as React from 'react';
import { useContext } from 'react';
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';
import WidgetEditApplyAllChangesProvider from 'views/components/contexts/WidgetEditApplyAllChangesProvider';
import type Widget from 'views/logic/widgets/Widget';

import WidgetQueryControls from '../WidgetQueryControls';
import WidgetOverrideElements from '../WidgetOverrideElements';
import DisableSubmissionStateProvider from '../contexts/DisableSubmissionStateProvider';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  flex: 1;
  overflow: hidden;
`;

const QueryControls = styled.div`
  margin-bottom: 10px;
`;

const Visualization = styled.div`
  display: flex;
  flex: 1;
  overflow: hidden;
`;

type Props = {
  children: React.ReactNode,
  displaySubmitActions?: boolean,
  onCancel: () => void,
  showQueryControls?: boolean,
  onSubmit: (newWidget: Widget, hasChanges: boolean) => Promise<void>,
  containerComponent?: React.ComponentType<React.PropsWithChildren>
};

const EditWidgetFrame = ({ children, onCancel, onSubmit, displaySubmitActions = true, showQueryControls = true, containerComponent: ContainerComponent = WidgetOverrideElements }: Props) => {
  const widget = useContext(WidgetContext);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  return (
    <WidgetEditApplyAllChangesProvider widget={widget} onSubmit={onSubmit}>
      <DisableSubmissionStateProvider>
        <Container>
          {(showQueryControls && !widget.returnsAllRecords) && (
            <QueryControls>
              <QueryEditModeContext.Provider value="widget">
                <WidgetQueryControls />
              </QueryEditModeContext.Provider>
            </QueryControls>
          )}
          <Visualization role="presentation">
            <ContainerComponent>
              {children}
            </ContainerComponent>
          </Visualization>
          {displaySubmitActions && (
            <div>
              <SaveOrCancelButtons onCancel={onCancel} />
            </div>
          )}
        </Container>
      </DisableSubmissionStateProvider>
    </WidgetEditApplyAllChangesProvider>
  );
};

export default EditWidgetFrame;
