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
import React from 'react';
import styled, { css } from 'styled-components';

import { Spinner, Icon, OverlayTrigger } from 'components/common';
import EditableTitle, { Title } from 'views/components/common/EditableTitle';
import { Input } from 'components/bootstrap';
import IconButton from 'components/common/IconButton';
import { widgetDragHandleClass } from 'views/components/widgets/Constants';

const LoadingSpinner = styled(Spinner)`
  margin-left: 10px;
`;

const Container = styled.div(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
    text-overflow: ellipsis;
    margin-bottom: 5px;
    display: grid;
    grid-template-columns: minmax(35px, 1fr) max-content;
    align-items: center;

    .widget-title {
      width: 100%;
      max-width: 400px;
    }
  `,
);

const Col = styled.div`
  display: flex;
  align-items: center;
  gap: 10px;
`;

const DragHandleContainer = styled.div`
  display: flex;
  align-items: center;
  justify-content: center;
`;

const WidgetDragHandle = styled(Icon)`
  cursor: move;
  opacity: 0.3;
  margin-right: 5px;
`;

const WidgetActionDropdown = styled.span`
  position: relative;
`;

const TitleInputWrapper = styled.div`
  max-width: 400px;
  width: 100%;

  .form-group {
    margin-bottom: 5px;
    width: 100%;
  }
`;

const TitleInput = styled(Input)(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
    width: 100%;
  `,
);

type WidgetTitleProps = {
  onChange?: (newTitle: string) => void;
  editing: boolean;
  title: string;
  titleIcon?: React.ReactNode;
};

const WidgetTitle = ({ onChange = undefined, editing, title, titleIcon = undefined }: WidgetTitleProps) => {
  if (typeof onChange !== 'function') {
    return (
      <>
        <Title>{title}</Title>
        {titleIcon}
      </>
    );
  }

  return editing ? (
    <TitleInputWrapper>
      <TitleInput
        type="text"
        id="widget-title"
        onChange={(e) => onChange(e.target.value)}
        defaultValue={title}
        required
      />
    </TitleInputWrapper>
  ) : (
    <>
      <EditableTitle key={title} disabled={!onChange} value={title} onChange={onChange} />
      {titleIcon}
    </>
  );
};

type WidgetDescriptionProps = {
  onChange?: (newDescription: string) => void;
  editing: boolean;
  description: string;
};

const DescriptionInputWrapper = styled.div`
  flex-grow: 1;
  width: 100%;

  .form-group {
    margin-bottom: 5px;
    width: 100%;
  }
`;

const DescriptionInput = styled(Input)(
  ({ theme }) => css`
    font-size: ${theme.fonts.size.large};
    width: 100%;
  `,
);

const DescriptionPopover = ({ description }: { description: string }) => (
  <OverlayTrigger
    trigger="click"
    rootClose
    placement="bottom"
    overlay={description ?? <i>No widget description provided</i>}>
    <IconButton title="Show description for widget" name="help" />
  </OverlayTrigger>
);

const WidgetDescription = ({ onChange = undefined, editing, description }: WidgetDescriptionProps) => {
  if (typeof onChange !== 'function') {
    return <Title>{description}</Title>;
  }
  if (!editing) {
    return description ? <DescriptionPopover description={description} /> : null;
  }

  return (
    <DescriptionInputWrapper>
      <DescriptionInput
        type="text"
        id="widget-description"
        placeholder="Please add a helpful description to the widget"
        onChange={(e) => onChange(e.target.value)}
        defaultValue={description}
        required
      />
    </DescriptionInputWrapper>
  );
};

type Props = {
  children?: React.ReactNode;
  onRename?: (newTitle: string) => unknown;
  onUpdateDescription?: (newDescription: string) => unknown;
  hideDragHandle?: boolean;
  title: string;
  description: string;
  loading?: boolean;
  editing: boolean;
  titleIcon?: React.ReactNode;
};

const WidgetHeader = ({
  title,
  description,
  editing,
  hideDragHandle = false,
  loading = false,
  children = undefined,
  titleIcon = undefined,
  onRename = undefined,
  onUpdateDescription = undefined,
}: Props) => (
  <Container>
    <Col>
      {hideDragHandle || (
        <DragHandleContainer className={widgetDragHandleClass} title={`Drag handle for ${title}`}>
          <WidgetDragHandle name="drag_indicator" />
        </DragHandleContainer>
      )}
      <WidgetTitle editing={editing} title={title} titleIcon={titleIcon} onChange={onRename} />
      <WidgetDescription editing={editing} description={description} onChange={onUpdateDescription} />
      {loading && <LoadingSpinner text="" delay={0} />}
    </Col>
    <WidgetActionDropdown>{children}</WidgetActionDropdown>
  </Container>
);

export default WidgetHeader;
