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
import PropTypes from 'prop-types';
import styled from 'styled-components';
import type { StyledComponent } from 'styled-components';

import { Icon } from 'components/common';
import ClipboardButton from 'components/common/ClipboardButton';

import type { WidgetProps } from './Widget';

const Container: StyledComponent<{}, {}, HTMLDivElement> = styled.div`
  height: 100%;
  display: flex;
  justify-content: center;
  align-items: center;
`;

const IconContainer = styled.div`
  margin: 3px 15px 0 0;
`;

const Description = styled.div`
  max-width: 700px;
`;

const Row = styled.div`
  margin-bottom: 5px;

  &:last-child {
    margin-bottom: 0;
  }
`;

const OrderedList = styled.ol`
  padding: 0;
  list-style: decimal inside none;
`;

const UnknownWidget = ({ config, type }: WidgetProps) => (
  <Container>
    <IconContainer>
      <Icon name="question" size="3x" />
    </IconContainer>
    <Description>
      <Row>
        <strong>Unknown Widget: {type}</strong>
      </Row>
      <Row>
        Unfortunately we are not able to render this widget, because we do not know how to handle widgets of
        type <strong>{type}</strong>. This might be caused by one of these situations:
      </Row>

      <Row>
        <OrderedList>
          <li>You created this widget using a plugin that is now missing.</li>
          <li>This widget was part of a legacy dashboard and created by a plugin that is not available anymore.</li>
        </OrderedList>
      </Row>

      <Row>
        What can you do about it? You can load the plugin again, contact the original plugin author for a plugin that
        works with Graylog 3.2+, or remove the widget if you do not need it anymore.
      </Row>
      <Row>
        Either way, you can copy the widget&rsquo;s config to the
        clipboard: <ClipboardButton title={<Icon name="copy" bsSize="sm" />} text={JSON.stringify(config, null, 2)} bsSize="xsmall" />
      </Row>
    </Description>
  </Container>
);

UnknownWidget.propTypes = {
  config: PropTypes.object.isRequired,
  type: PropTypes.string.isRequired,
};

export default UnknownWidget;
