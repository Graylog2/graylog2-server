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
import { useState } from 'react';
import { List, Set } from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import { Field, Formik, Form } from 'formik';

import connect from 'stores/connect';
import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';
import { Icon, Spinner } from 'components/common';
import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CSVExportSettings from 'views/components/searchbar/csvexport/CSVExportSettings';
import CSVExportWidgetSelection from 'views/components/searchbar/csvexport/CSVExportWidgetSelection';
import CustomPropTypes from 'views/components/CustomPropTypes';
import { MESSAGE_FIELD, SOURCE_FIELD, TIMESTAMP_FIELD } from 'views/Constants';
import { ExportSettings } from 'views/components/ExportSettingsContext';

import ExportStrategy from './ExportStrategy';
import startDownload from './startDownload';

const DEFAULT_FIELDS = Set([TIMESTAMP_FIELD, SOURCE_FIELD, MESSAGE_FIELD]);

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

const _getInitialWidgetFields = (selectedWidget: Widget): Set<string> => {
  if (selectedWidget.config.showMessageRow) {
    return Set<string>(selectedWidget.config.fields).add(MESSAGE_FIELD).toSet();
  }

  return Set(selectedWidget.config.fields);
};

const _getInitialFields = (selectedWidget) => {
  const initialFields = selectedWidget ? _getInitialWidgetFields(selectedWidget) : DEFAULT_FIELDS;

  return initialFields.map((field) => ({ field })).toArray();
};

type FormState = {
  selectedWidget: Widget | undefined,
  limit: number,
  selectedFields: Array<{ field: string }>,
  customSettings: ExportSettings,
  format: string,
};

const CSVExportModal = ({ closeModal, fields, view, directExportWidgetId, executionState }: Props) => {
  const { state: viewStates } = view;
  const { shouldEnableDownload, title, initialWidget, shouldShowWidgetSelection, shouldAllowWidgetSelection, downloadFile } = ExportStrategy.createExportStrategy(view.type);
  const exportableWidgets = viewStates.map((state) => state.widgets.filter((widget) => widget.isExportable).toList()).toList().flatten(true) as List<Widget>;

  const [loading, setLoading] = useState(false);
  const initialSelectedWidget = initialWidget(exportableWidgets, directExportWidgetId);
  const initialSelectedFields = _getInitialFields(initialSelectedWidget);

  const singleWidgetDownload = !!directExportWidgetId;

  const _startDownload = ({ selectedWidget, selectedFields, limit, customSettings }: FormState) => {
    setLoading(true);

    return startDownload(downloadFile, view, executionState, selectedWidget, selectedFields, limit, customSettings)
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
      {({ submitForm, values: { selectedWidget, selectedFields }, setFieldValue }) => {
        const showWidgetSelection = shouldShowWidgetSelection(singleWidgetDownload, selectedWidget, exportableWidgets);
        const allowWidgetSelection = shouldAllowWidgetSelection(singleWidgetDownload, showWidgetSelection, exportableWidgets);
        const enableDownload = shouldEnableDownload(showWidgetSelection, selectedWidget, selectedFields, loading);
        const resetSelectedWidget = () => setFieldValue('selectedWidget', undefined);
        const setSelectedFields = (newFields) => setFieldValue('selectedFields', newFields);

        return (
          <Form>
            <BootstrapModalWrapper showModal onHide={closeModal}>
              <Modal.Header>
                <Modal.Title>{title}</Modal.Title>
              </Modal.Header>
              <Modal.Body>
                <Content>
                  {showWidgetSelection && (
                  <Field name="selectedWidget">
                    {({ field: { name, onChange } }) => {
                      const onChangeSelectWidget = ({ value }) => {
                        setSelectedFields(_getInitialFields(value));

                        return onChange({ target: { name, value } });
                      };

                      return (
                        <CSVExportWidgetSelection selectWidget={onChangeSelectWidget}
                                                  view={view}
                                                  widgets={exportableWidgets.toList()} />
                      );
                    }}
                  </Field>
                  )}
                  {!showWidgetSelection && (
                  <CSVExportSettings fields={fields}
                                     selectedWidget={initialSelectedWidget}
                                     view={view} />
                  )}
                </Content>
              </Modal.Body>
              <Modal.Footer>
                {allowWidgetSelection && <Button bsStyle="link" onClick={resetSelectedWidget} className="pull-left">Select different message table</Button>}
                <Button type="button" onClick={closeModal}>Close</Button>
                <Button type="submit" onClick={submitForm} disabled={!enableDownload} bsStyle="primary" data-testid="csv-download-button">
                  {loading
                    ? <Spinner text="Downloading..." delay={0} />
                    : <><Icon name="cloud-download-alt" />&nbsp;Start Download</>}
                </Button>
              </Modal.Footer>
            </BootstrapModalWrapper>
          </Form>
        );
      }}
    </Formik>
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
