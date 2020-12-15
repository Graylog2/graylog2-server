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
import { useContext, useEffect, useCallback } from 'react';
import PropTypes from 'prop-types';

import { Modal } from 'components/graylog';
import Spinner from 'components/common/Spinner';
import WidgetContext from 'views/components/contexts/WidgetContext';
import QueryEditModeContext from 'views/components/contexts/QueryEditModeContext';
import { createElasticsearchQueryString } from 'views/logic/queries/Query';
import Widget from 'views/logic/widgets/Widget';
import { WidgetActions } from 'views/stores/WidgetStore';
import { DEFAULT_TIMERANGE } from 'views/Constants';

import WidgetQueryControls from '../WidgetQueryControls';
import IfDashboard from '../dashboard/IfDashboard';
import HeaderElements from '../HeaderElements';
import WidgetOverrideElements from '../WidgetOverrideElements';
import SearchBarForm from '../searchbar/SearchBarForm';

// eslint-disable-next-line import/no-webpack-loader-syntax
import styles from '!style?insertAt=bottom!css!./EditWidgetFrame.css';
// eslint-disable-next-line import/no-webpack-loader-syntax
import globalStyles from '!style/useable!css!./EditWidgetFrame.global.css';

type DialogProps = {
  bsClass: string,
  className: string,
  children: React.ReactNode,
};

const EditWidgetDialog = ({ className, children, bsClass, ...rest }: DialogProps) => (
  <Modal.Dialog {...rest} dialogClassName={styles.editWidgetDialog}>
    {children}
  </Modal.Dialog>
);

EditWidgetDialog.propTypes = {
  className: PropTypes.string.isRequired,
  children: PropTypes.node.isRequired,
};

type Props = {
  children: Array<React.ReactNode>,
};

const onSubmit = (values, widget: Widget) => {
  const { timerange, streams, queryString } = values;
  const newWidget = widget.toBuilder()
    .timerange(timerange)
    .query(createElasticsearchQueryString(queryString))
    .streams(streams)
    .build();

  return WidgetActions.update(widget.id, newWidget);
};

const EditWidgetFrame = ({ children }: Props) => {
  useEffect(() => {
    globalStyles.use();

    return globalStyles.unuse;
  }, []);

  const widget = useContext(WidgetContext);

  if (!widget) {
    return <Spinner text="Loading widget ..." />;
  }

  const { streams } = widget;
  const timerange = widget.timerange ?? DEFAULT_TIMERANGE;
  const { query_string: queryString } = widget.query ?? createElasticsearchQueryString('');
  const _onSubmit = useCallback((values) => onSubmit(values, widget), [widget]);

  return (
    <Modal show
           animation={false}
           dialogComponentClass={EditWidgetDialog}
           enforceFocus={false}>
      <SearchBarForm initialValues={{ timerange, streams, queryString }}
                     onSubmit={_onSubmit}>
        <div className={styles.gridContainer}>
          <IfDashboard>
            <Modal.Header className={styles.QueryControls}>
              <QueryEditModeContext.Provider value="widget">
                <HeaderElements />
                <WidgetQueryControls />
              </QueryEditModeContext.Provider>
            </Modal.Header>
          </IfDashboard>
          <Modal.Body className={styles.Visualization}>
            <div role="presentation" style={{ height: '100%' }}>
              <WidgetOverrideElements>
                {children[0]}
              </WidgetOverrideElements>
            </div>
          </Modal.Body>
          <Modal.Footer className={styles.Footer}>
            {children[1]}
          </Modal.Footer>
        </div>
      </SearchBarForm>
    </Modal>
  );
};

EditWidgetFrame.propTypes = {
  children: PropTypes.node.isRequired,
};

export default EditWidgetFrame;
