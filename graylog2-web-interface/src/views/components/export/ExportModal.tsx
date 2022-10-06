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
import type { List } from 'immutable';
import { OrderedSet } from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Field, Formik, Form } from 'formik';

import ModalSubmit from 'components/common/ModalSubmit';
import connect from 'stores/connect';
import type SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import type View from 'views/logic/views/View';
import type Widget from 'views/logic/widgets/Widget';
import { Modal, Button } from 'components/bootstrap';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import ExportWidgetSelection from 'views/components/export/ExportWidgetSelection';
import { MESSAGE_FIELD, SOURCE_FIELD, TIMESTAMP_FIELD } from 'views/Constants';
import type { ExportSettings as ExportSettingsType } from 'views/components/ExportSettingsContext';
import FieldTypesContext from 'views/components/contexts/FieldTypesContext';

import ExportSettings from './ExportSettings';
import ExportStrategy from './ExportStrategy';
import startDownload from './startDownload';

const DEFAULT_FIELDS = OrderedSet([TIMESTAMP_FIELD, SOURCE_FIELD, MESSAGE_FIELD]);

const Content = styled.div`
  margin-left: 15px;
  margin-right: 15px;
`;

export type Props = {
  closeModal: () => void,
  directExportWidgetId?: string,
  executionState: SearchExecutionState,
  view: View,
};

const _getInitialWidgetFields = (selectedWidget: Widget): OrderedSet<string> => {
  if (selectedWidget.config.showMessageRow) {
    return OrderedSet<string>(selectedWidget.config.fields).add(MESSAGE_FIELD).toOrderedSet();
  }

  return OrderedSet(selectedWidget.config.fields);
};

const _getInitialFields = (selectedWidget) => {
  const initialFields = selectedWidget ? _getInitialWidgetFields(selectedWidget) : DEFAULT_FIELDS;

  return initialFields.map((field) => ({ field })).toArray();
};

type FormState = {
  selectedWidget: Widget | undefined,
  limit: number,
  selectedFields: Array<{ field: string }>,
  customSettings: ExportSettingsType,
  format: string,
};

const ExportModal = ({ closeModal, view, directExportWidgetId, executionState }: Props) => {
  const { state: viewStates } = view;
  const { shouldEnableDownload, title, initialWidget, shouldShowWidgetSelection, shouldAllowWidgetSelection, downloadFile } = ExportStrategy.createExportStrategy(view.type);
  const exportableWidgets = viewStates.map((state) => state.widgets.filter((widget) => widget.isExportable).toList()).toList().flatten(true) as List<Widget>;

  const [loading, setLoading] = useState(false);
  const initialSelectedWidget = initialWidget(exportableWidgets, directExportWidgetId);
  const initialSelectedFields = _getInitialFields(initialSelectedWidget);

  const { all: fields } = useContext(FieldTypesContext);

  const singleWidgetDownload = !!directExportWidgetId;

  const _startDownload = ({ selectedWidget, selectedFields, limit, customSettings, format }: FormState) => {
    setLoading(true);

    return startDownload(format, downloadFile, view, executionState, selectedWidget, selectedFields, limit, customSettings)
      .then(closeModal)
      .finally(() => setLoading(false));
  };

  const initialValues: FormState = {
    selectedWidget: initialSelectedWidget,
    selectedFields: initialSelectedFields,
    limit: undefined,
    customSettings: {},
    format: 'csv',
  };

  return (
    <Formik<FormState> onSubmit={_startDownload}
                       initialValues={initialValues}>
      {({ values: { selectedWidget, selectedFields }, setFieldValue }) => {
        const showWidgetSelection = shouldShowWidgetSelection(singleWidgetDownload, selectedWidget, exportableWidgets);
        const allowWidgetSelection = shouldAllowWidgetSelection(singleWidgetDownload, showWidgetSelection, exportableWidgets);
        const enableDownload = shouldEnableDownload(showWidgetSelection, selectedWidget, selectedFields, loading);
        const resetSelectedWidget = () => setFieldValue('selectedWidget', undefined);
        const setSelectedFields = (newFields) => setFieldValue('selectedFields', newFields);

        return (
          <BootstrapModalWrapper showModal onHide={closeModal}>
            <Form>
              <Modal.Header>
                <Modal.Title>{title}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Content>
                  {showWidgetSelection && (
                    <Field name="selectedWidget">
                      {({ field: { name, onChange } }) => {
                        const onChangeSelectWidget = (widget) => {
                          setSelectedFields(_getInitialFields(widget));

                          return onChange({ target: { name, value: widget } });
                        };

                        return (
                          <ExportWidgetSelection selectWidget={onChangeSelectWidget}
                                                 view={view}
                                                 widgets={exportableWidgets.toList()} />
                        );
                      }}
                    </Field>
                  )}
                  {!showWidgetSelection && (
                    <ExportSettings fields={fields}
                                    selectedWidget={initialSelectedWidget}
                                    view={view} />
                  )}
                </Content>
              </Modal.Body>
              <Modal.Footer>
                <ModalSubmit leftCol={
                              allowWidgetSelection && (
                                <Button bsStyle="link" onClick={resetSelectedWidget} className="pull-left">
                                  Select different message table
                                </Button>
                              )
                             }
                             onCancel={closeModal}
                             disabledSubmit={!enableDownload}
                             isSubmitting={loading}
                             isAsyncSubmit
                             submitLoadingText="Downloading..."
                             submitIcon="cloud-download-alt"
                             submitButtonText="Start Download" />
              </Modal.Footer>
            </Form>
          </BootstrapModalWrapper>
        );
      }}
    </Formik>
  );
};

ExportModal.propTypes = {
  closeModal: PropTypes.func,
  directExportWidgetId: PropTypes.string,
};

ExportModal.defaultProps = {
  closeModal: () => {},
  directExportWidgetId: null,
};

export default connect(
  ExportModal,
  {
    executionState: SearchExecutionStateStore,
  },
  ({ executionState, ...rest }) => ({
    ...rest,
    executionState,
  }),
);
