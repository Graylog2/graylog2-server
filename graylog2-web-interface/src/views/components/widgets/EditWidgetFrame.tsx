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

import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';
import WidgetOverrideElements from '../WidgetOverrideElements';
import WidgetEditApplyAllChangesContext from '../contexts/WidgetEditApplyAllChangesContext';
import ValidationStateProvider from '../contexts/ValidationStateProvider';
import ValidationStateContext from '../contexts/ValidationStateContext';

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
  onCancel: () => void,
  onFinish: () => void,
};

const EditWidgetFrame = ({ children, onCancel, onFinish }: Props) => {
  const widget = useContext(WidgetContext);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  return (
    <WidgetEditApplyAllChangesProvider widget={widget}>
      <ValidationStateProvider>
        <Container>
          <IfDashboard>
            <QueryControls>
              <QueryEditModeContext.Provider value="widget">
                <WidgetQueryControls />
              </QueryEditModeContext.Provider>
            </QueryControls>
          </IfDashboard>
          <Visualization role="presentation">
            <WidgetOverrideElements>
              {children}
            </WidgetOverrideElements>
          </Visualization>
          <div>
            <WidgetEditApplyAllChangesContext.Consumer>
              {({ isSubmitting }) => (
                <ValidationStateContext.Consumer>
                  {({ hasErrors }) => (
                    <SaveOrCancelButtons onFinish={onFinish} onCancel={onCancel} disableSave={hasErrors || isSubmitting} />
                  )}
                </ValidationStateContext.Consumer>
              )}
            </WidgetEditApplyAllChangesContext.Consumer>
          </div>
        </Container>
      </ValidationStateProvider>
    </WidgetEditApplyAllChangesProvider>
  );
};

export default EditWidgetFrame;
