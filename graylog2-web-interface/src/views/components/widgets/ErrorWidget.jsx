// @flow strict
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
        {errors.map(e => <Row as="li" key={e.description}>{e.description}</Row>)}
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
