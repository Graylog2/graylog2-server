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
import styled from 'styled-components';

import FieldTypesContext from 'views/components/contexts/FieldTypesContext';
import WidgetContext from 'views/components/contexts/WidgetContext';
import useViewsDispatch from 'views/stores/useViewsDispatch';
import { updateDescription, updateWidgetConfig } from 'views/logic/slices/widgetActions';
import type WidgetConfig from 'views/logic/widgets/WidgetConfig';
import type Widget from 'views/logic/widgets/Widget';
import WidgetColorContext from 'views/components/widgets/WidgetColorContext';
import WidgetHeader from 'views/components/widgets/WidgetHeader';
import WidgetEditWrapper from 'views/components/widgets/WidgetEditWrapper';

import StandaloneWidgetGroup, { WidgetFromGroup } from './StandaloneWidgetGroup';
import { WidgetVisualizationFromContext } from './StandaloneWidgetContent';

const Container = styled.div`
  display: flex;
  flex-direction: column;
  height: 100%;
  min-height: 0;
`;

type EditorProps = {
  initialTitle: string;
  onSave: (widget: Widget, title: string) => Promise<void>;
  onCancel: () => void;
};

const Editor = ({ initialTitle, onSave, onCancel }: EditorProps) => {
  const widget = useContext(WidgetContext);
  const fieldTypes = useContext(FieldTypesContext);
  const dispatch = useViewsDispatch();
  const [title, setTitle] = useState(initialTitle);
  const onWidgetConfigChange = (newConfig: WidgetConfig) => {
    dispatch(updateWidgetConfig(widget.id, newConfig));
  };

  return (
    <Container>
      <WidgetHeader
        title={title}
        description={widget.description}
        editing
        hideDragHandle
        onRename={setTitle}
        onUpdateDescription={(newDescription: string) => dispatch(updateDescription(widget, newDescription))}
      />
      <WidgetEditWrapper
        editing
        showQueryControls
        config={widget.config}
        fields={fieldTypes?.all}
        id={widget.id}
        type={widget.type}
        onCancelEdit={onCancel}
        onWidgetConfigChange={onWidgetConfigChange}
        onSubmit={(newWidget) => onSave(newWidget, title)}>
        <WidgetVisualizationFromContext title={title} editing />
      </WidgetEditWrapper>
    </Container>
  );
};

type Props = {
  widget: Widget;
  title: string;
  onSave: (widget: Widget, title: string) => Promise<void>;
  onCancel: () => void;
};

/**
 * The widget edit mode of the search page for a widget outside of a search or dashboard.
 * `onSave` receives the edited widget, including its query, time range and streams.
 */
const StandaloneWidgetEditor = ({ widget, title, onSave, onCancel }: Props) => (
  <StandaloneWidgetGroup widgets={[widget]} titles={{ [widget.id]: title }} interactionMode="interactive">
    <WidgetFromGroup widgetId={widget.id}>
      <WidgetColorContext id={widget.id}>
        <Editor initialTitle={title} onSave={onSave} onCancel={onCancel} />
      </WidgetColorContext>
    </WidgetFromGroup>
  </StandaloneWidgetGroup>
);

export default StandaloneWidgetEditor;
