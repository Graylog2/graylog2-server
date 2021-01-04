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
import React, { useState } from 'react';
import { List } from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';

import connect from 'stores/connect';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import { Icon, Spinner } from 'components/common';
import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CSVExportSettings from 'views/components/searchbar/csvexport/CSVExportSettings';
import CSVExportWidgetSelection from 'views/components/searchbar/csvexport/CSVExportWidgetSelection';
import CustomPropTypes from 'views/components/CustomPropTypes';

import ExportStrategy from './ExportStrategy';
import startDownload from './startDownload';

const DEFAULT_FIELDS = ['timestamp', 'source', 'message'];

const Content = styled.div`
  margin-left: 15px;
  margin-right: 15px;
`;

export type Props = {
  closeModal: () => void,
  directExportWidgetId?: string,
  executionState: SearchExecutionState,
  fields: List<FieldTypeMapping>,
  view: View,
};

const _getInitialWidgetFields = (selectedWidget) => {
  if (selectedWidget.config.showMessageRow) {
    return [...new Set([...selectedWidget.config.fields, 'message'])];
  }

  return selectedWidget.config.fields;
};

const _getInitialFields = (selectedWidget) => {
  let initialFields = DEFAULT_FIELDS;

  if (selectedWidget) {
    initialFields = _getInitialWidgetFields(selectedWidget);
  }

  return initialFields.map((field) => ({ field }));
};

const _onSelectWidget = ({ value: newWidget }, setSelectedWidget, setSelectedFields) => {
  setSelectedWidget(newWidget);
  setSelectedFields(_getInitialFields(newWidget));
};

const _onFieldSelect = (newFields, setSelectedFields) => {
  setSelectedFields(newFields.map((field) => ({ field: field.value })));
};

const _onStartDownload = (downloadFile, view, executionState, selectedWidget, selectedFields, limit, setLoading, closeModal) => {
  setLoading(true);
  startDownload(downloadFile, view, executionState, selectedWidget, selectedFields, limit).then(closeModal);
};

const CSVExportModal = ({ closeModal, fields, view, directExportWidgetId, executionState }: Props) => {
  const { state: viewStates } = view;
  const { shouldEnableDownload, title, initialWidget, shouldShowWidgetSelection, shouldAllowWidgetSelection, downloadFile } = ExportStrategy.createExportStrategy(view.type);
  const messagesWidgets = viewStates.map((state) => state.widgets.filter((widget) => widget.type === MessagesWidget.type).toList()).toList().flatten(true) as List<Widget>;

  const [loading, setLoading] = useState(false);
  const [selectedWidget, setSelectedWidget] = useState<Widget | undefined>(initialWidget(messagesWidgets, directExportWidgetId));
  const [selectedFields, setSelectedFields] = useState<{ field: string }[]>(_getInitialFields(selectedWidget));
  const [limit, setLimit] = useState<number | undefined>();

  const singleWidgetDownload = !!directExportWidgetId;
  const showWidgetSelection = shouldShowWidgetSelection(singleWidgetDownload, selectedWidget, messagesWidgets);
  const allowWidgetSelection = shouldAllowWidgetSelection(singleWidgetDownload, showWidgetSelection, messagesWidgets);
  const enableDownload = shouldEnableDownload(showWidgetSelection, selectedWidget, selectedFields, loading);
  const _startDownload = () => _onStartDownload(downloadFile, view, executionState, selectedWidget, selectedFields, limit, setLoading, closeModal);

  return (
    <BootstrapModalWrapper showModal onHide={closeModal}>
      <Modal.Header>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Content>
          {showWidgetSelection && (
            <CSVExportWidgetSelection selectWidget={(selection) => _onSelectWidget(selection, setSelectedWidget, setSelectedFields)}
                                      view={view}
                                      widgets={messagesWidgets.toList()} />
          )}
          {!showWidgetSelection && (
            <CSVExportSettings fields={fields}
                               selectedFields={selectedFields}
                               selectField={(newFields) => _onFieldSelect(newFields, setSelectedFields)}
                               selectedWidget={selectedWidget}
                               setLimit={setLimit}
                               limit={limit}
                               view={view} />
          )}
        </Content>
      </Modal.Body>
      <Modal.Footer>
        {allowWidgetSelection && <Button bsStyle="link" onClick={() => setSelectedWidget(null)} className="pull-left">Select different message table</Button>}
        <Button type="button" onClick={closeModal}>Close</Button>
        <Button type="button" onClick={_startDownload} disabled={!enableDownload} bsStyle="primary" data-testid="csv-download-button">
          {loading
            ? <Spinner text="Downloading..." delay={0} />
            : <><Icon name="cloud-download-alt" />&nbsp;Start Download</>}
        </Button>
      </Modal.Footer>
    </BootstrapModalWrapper>
  );
};

CSVExportModal.propTypes = {
  closeModal: PropTypes.func,
  directExportWidgetId: PropTypes.string,
  fields: CustomPropTypes.FieldListType.isRequired,
};

CSVExportModal.defaultProps = {
  closeModal: () => {},
  directExportWidgetId: null,
};

export default connect(
  CSVExportModal,
  {
    fields: FieldTypesStore,
    executionState: SearchExecutionStateStore,
  },
  ({ fields: { all }, executionState, ...rest }) => ({
    ...rest,
    executionState,
    fields: all,
  }),
);
