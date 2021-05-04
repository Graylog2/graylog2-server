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
import PropTypes from 'prop-types';
import styled from 'styled-components';

import { Icon } from 'components/common';

import { WidgetErrorsList } from './WidgetPropTypes';
import styles from './MessageWidgets.css';

type WidgetError = {
  description: string,
};

type Props = {
  errors: Array<WidgetError>,
  title?: string,
};

const Description = styled.div`
  max-width: 700px;
`;

const ErrorList = styled.ul`
  padding: 0;
`;

const Row = styled.div`
  margin-bottom: 5px;

  :last-child {
    margin-bottom: 0;
  }
`;

const ErrorWidget = ({ errors, title }: Props) => (
  <div className={styles.spinnerContainer}>
    <Icon name="exclamation-triangle" size="3x" className={styles.iconMargin} />
    <Description>
      <Row>
        <strong>{title}</strong>
      </Row>
      <ErrorList>
        {errors.map((e) => <Row as="li" key={e.description}>{e.description}</Row>)}
      </ErrorList>
    </Description>
  </div>
);

ErrorWidget.propTypes = {
  errors: WidgetErrorsList.isRequired,
  title: PropTypes.string,
};

ErrorWidget.defaultProps = {
  title: 'While retrieving data for this widget, the following error(s) occurred:',
};

export default ErrorWidget;
