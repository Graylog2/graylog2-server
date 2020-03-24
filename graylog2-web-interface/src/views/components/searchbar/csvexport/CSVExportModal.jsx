// @flow strict
import React, { useState } from 'react';
import { List } from 'immutable';
import PropTypes from 'prop-types';
import styled from 'styled-components';
import connect from 'stores/connect';

import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { defaultSort } from 'views/logic/widgets/MessagesWidgetConfig';
import FieldTypeMapping from 'views/logic/fieldtypes/FieldTypeMapping';
import MessagesWidget from 'views/logic/widgets/MessagesWidget';
import SortConfig from 'views/logic/aggregationbuilder/SortConfig';
import View from 'views/logic/views/View';
import Widget from 'views/logic/widgets/Widget';

import { Icon } from 'components/common';
import { Modal, Button } from 'components/graylog';
import BootstrapModalWrapper from 'components/bootstrap/BootstrapModalWrapper';
import CSVExportSettings from 'views/components/searchbar/csvexport/CSVExportSettings';
import CSVExportWidgetSelection from 'views/components/searchbar/csvexport/CSVExportWidgetSelection';
import CustomPropTypes from 'views/components/CustomPropTypes';

import ExportStategy from './ExportStrategy';
import startDownload from './startDownload';

const Content = styled.div`
  margin-left: 15px;
  margin-right: 15px;
`;

type Props = {
  closeModal: () => void,
  directExportWidgetId?: string,
  fields: List<FieldTypeMapping>,
  view: View
};

const _onSelectWidget = ({ value: newWidget }, setSelectedWidget, setSelectedFields, setSelectedSort) => {
  setSelectedWidget(newWidget);
  setSelectedFields(newWidget.config.fields.map(fieldName => ({ field: fieldName })));
  setSelectedSort(newWidget.config.sort);
};

const _onFieldSelect = (newFields, setSelectedFields) => {
  setSelectedFields(newFields.map(field => ({ field: field.value })));
};

const _wrapFieldOption = field => ({ field });
const _defaultFields = ['timestamp', 'source', 'message'];
const _defaultFieldOptions = _defaultFields.map(_wrapFieldOption);

const CSVExportModal = ({ closeModal, fields, view, directExportWidgetId }: Props) => {
  const { state: viewStates } = view;
  const { shouldEnableDownload, title, initialWidget, shouldShowWidgetSelection, shouldAllowWidgetSelection } = ExportStategy.createExportStrategy(view.type);
  const messagesWidgets = viewStates.map(state => state.widgets.filter(widget => widget.type === MessagesWidget.type)).flatten(true);

  const [selectedWidget, setSelectedWidget] = useState<?Widget>(initialWidget(messagesWidgets, directExportWidgetId));
  const [selectedFields, setSelectedFields] = useState<{ field: string }[]>(selectedWidget ? selectedWidget.config.fields.map(_wrapFieldOption) : _defaultFieldOptions);
  const [selectedSort, setSelectedSort] = useState<SortConfig[]>(selectedWidget ? selectedWidget.config.sort : defaultSort);
  const [selectedSortDirection] = selectedSort.map(s => s.direction);

  const singleWidgetDownload = !!directExportWidgetId;
  const widgetTitles = viewStates.flatMap(state => state.titles.get('widget'));
  const showWidgetSelection = shouldShowWidgetSelection(singleWidgetDownload, selectedWidget, messagesWidgets);
  const allowWidgetSelection = shouldAllowWidgetSelection(singleWidgetDownload, showWidgetSelection, messagesWidgets);
  const enableDownload = shouldEnableDownload(showWidgetSelection, selectedWidget, selectedFields);

  return (
    <BootstrapModalWrapper showModal onHide={closeModal}>
      <Modal.Header>
        <Modal.Title>{title}</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Content>
          {showWidgetSelection && (
            <CSVExportWidgetSelection selectWidget={selection => _onSelectWidget(selection, setSelectedWidget, setSelectedFields, setSelectedSort)}
                                      viewStates={viewStates}
                                      widgetTitles={widgetTitles}
                                      widgets={messagesWidgets} />
          )}
          {!showWidgetSelection && (
            <CSVExportSettings fields={fields}
                               selectedFields={selectedFields}
                               selectedSort={selectedSort}
                               selectedSortDirection={selectedSortDirection}
                               selectField={newFields => _onFieldSelect(newFields, setSelectedFields)}
                               setSelectedSort={setSelectedSort}
                               selectedWidget={selectedWidget}
                               widgetTitles={widgetTitles} />
          )}
        </Content>
      </Modal.Body>
      <Modal.Footer>
        {allowWidgetSelection && <Button bsStyle="link" onClick={() => setSelectedWidget(null)} className="pull-left">Select different message table</Button>}
        <Button type="button" onClick={closeModal}>Close</Button>
        <Button type="button" onClick={() => startDownload(view, selectedWidget, selectedFields, selectedSort)} disabled={!enableDownload} bsStyle="primary" data-testid="csv-download-button"><Icon name="cloud-download" /> Start Download</Button>
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
  },
  ({ fields: { all }, ...rest }) => ({
    ...rest,
    fields: all,
  }),
);
