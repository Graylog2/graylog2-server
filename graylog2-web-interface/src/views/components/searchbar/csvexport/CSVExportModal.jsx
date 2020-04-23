// @flow strict
import React, { useState } from 'react';
import { List } from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import connect from 'stores/connect';

import SearchExecutionState from 'views/logic/search/SearchExecutionState';
import { SearchExecutionStateStore } from 'views/stores/SearchExecutionStateStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { defaultSort } from 'views/logic/widgets/MessagesWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
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

const _onSelectWidget = ({ value: newWidget }, setSelectedWidget, setSelectedFields, setSelectedSort) => {
  setSelectedWidget(newWidget);
  setSelectedFields(newWidget.config.fields.map((fieldName) => ({ field: fieldName })));
  setSelectedSort(newWidget.config.sort);
};

const _onFieldSelect = (newFields, setSelectedFields) => {
  setSelectedFields(newFields.map((field) => ({ field: field.value })));
};

const _onStartDownload = (view, executionState, selectedWidget, selectedFields, selectedSort, limit, setLoading) => {
  setLoading(true);
  startDownload(view, executionState, selectedWidget, selectedFields, selectedSort, limit).then(() => setLoading(false));
};

const _getInitialFields = (selectedWidget) => {
  let initialFields = DEFAULT_FIELDS;
  if (selectedWidget) {
    // Because the message table always displays the message, we need to add it as an initial field.
    initialFields = [...new Set([...selectedWidget.config.fields, 'message'])];
  }
  return initialFields.map((field) => ({ field }));
};

const CSVExportModal = ({ closeModal, fields, view, directExportWidgetId, executionState }: Props) => {
  const { state: viewStates } = view;
  const { shouldEnableDownload, title, initialWidget, shouldShowWidgetSelection, shouldAllowWidgetSelection } = ExportStrategy.createExportStrategy(view.type);
  const messagesWidgets = viewStates.map((state) => state.widgets.filter((widget) => widget.type === MessagesWidget.type)).flatten(true);

  const [loading, setLoading] = useState(false);
  const [selectedWidget, setSelectedWidget] = useState<?Widget>(initialWidget(messagesWidgets, directExportWidgetId));
  const [selectedFields, setSelectedFields] = useState<{ field: string }[]>(_getInitialFields(selectedWidget));
  const [selectedSort, setSelectedSort] = useState<SortConfig[]>(selectedWidget ? selectedWidget.config.sort : defaultSort);
  const [selectedSortDirection] = selectedSort.map((s) => s.direction);
  const [limit, setLimit] = useState<?number>();

  const singleWidgetDownload = !!directExportWidgetId;
  const showWidgetSelection = shouldShowWidgetSelection(singleWidgetDownload, selectedWidget, messagesWidgets);
  const allowWidgetSelection = shouldAllowWidgetSelection(singleWidgetDownload, showWidgetSelection, messagesWidgets);
  const enableDownload = shouldEnableDownload(showWidgetSelection, selectedWidget, selectedFields, loading);

  return (
    <BootstrapModalWrapper showModal onHide={closeModal}>
      <Modal.Header>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Content>
          {showWidgetSelection && (
            <CSVExportWidgetSelection selectWidget={(selection) => _onSelectWidget(selection, setSelectedWidget, setSelectedFields, setSelectedSort)}
                                      view={view}
                                      widgets={messagesWidgets} />
          )}
          {!showWidgetSelection && (
            <CSVExportSettings fields={fields}
                               selectedFields={selectedFields}
                               selectedSort={selectedSort}
                               selectedSortDirection={selectedSortDirection}
                               selectField={(newFields) => _onFieldSelect(newFields, setSelectedFields)}
                               setSelectedSort={setSelectedSort}
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
        <Button type="button" onClick={() => _onStartDownload(view, executionState, selectedWidget, selectedFields, selectedSort, limit, setLoading)} disabled={!enableDownload} bsStyle="primary" data-testid="csv-download-button">
          {loading
            ? <Spinner text="Downloading..." delay={0} />
            : <><Icon name="cloud-download" />&nbsp;Start Download</>}
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
