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
import { useContext, useState } from 'react';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import Spinner from 'components/common/Spinner';
import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';
import SaveOrCancelButtons from 'views/components/widgets/SaveOrCancelButtons';

import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';
import HeaderElements from '../HeaderElements';
import WidgetOverrideElements from '../WidgetOverrideElements';

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
  flex 1;
  overflow: hidden;
`;

type Props = {
  children: React.ReactNode,
  onCancel: () => void,
  onFinish: () => void,
};

type ValidationState = [boolean, (hasErrors: boolean) => void];
export const ValidationStateContext = React.createContext<ValidationState>([false, () => {}]);

const EditWidgetFrame = ({ children, onCancel, onFinish }: Props) => {
  const widget = useContext(WidgetContext);

  const [hasErrors, setHasErrors] = useState(false);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  return (
    <ValidationStateContext.Provider value={[hasErrors, setHasErrors]}>
      <Container>
        <IfDashboard>
          <QueryControls>
            <QueryEditModeContext.Provider value="widget">
              <HeaderElements />
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
          <SaveOrCancelButtons onFinish={onFinish} onCancel={onCancel} disableSave={hasErrors} />
        </div>
      </Container>
    </ValidationStateContext.Provider>
  );
};

EditWidgetFrame.propTypes = {
  children: PropTypes.node.isRequired,
};

export default EditWidgetFrame;
