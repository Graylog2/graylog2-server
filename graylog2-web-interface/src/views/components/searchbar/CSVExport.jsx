// @flow strict
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';
import connect from 'stores/connect';
/* $FlowFixMe: Need to add to flow typed */
import styled from 'styled-components';

import { StreamsStore } from 'views/stores/StreamsStore';
import { FieldTypesStore } from 'views/stores/FieldTypesStore';
import { ViewStore } from 'views/stores/ViewStore';
import URLUtils from 'util/URLUtils';
import { Modal, Button, Row } from 'components/graylog';
import { Icon } from 'components/common';
import ApiRoutes from 'routing/ApiRoutes';
import StoreProvider from 'injection/StoreProvider';
import View from 'views/logic/views/View';
import Select from 'views/components/Select';

const SessionStore = StoreProvider.getStore('Session');

type Option = {
  label: String,
  value: String,
};

type Props = {
  availableStreams: Array<Option>,
  availableFields: Array<Option>,
  closeModal: () => void,
};

const getURLForExportAsCSV = (selectedStream: ?string, selectedFields: Array<string> = []) => {
  const { view } = ViewStore.getInitialState() || {};
  if (view && view.type === View.Type.Search) {
    const { queries } = view.search;
    if (queries.size !== 1) {
      throw new Error('Searches must only have a single query!');
    }
    const firstQuery = queries.first();
    if (firstQuery) {
      const { query: { query_string: queryString }, timerange } = firstQuery;
      const query = !queryString ? '*' : queryString;
      const url = new URI(URLUtils.qualifyUrl(
        ApiRoutes.UniversalSearchApiController.export(
          timerange.type,
          query,
          timerange,
          selectedStream,
          0,
          0,
          selectedFields,
        ).url,
      ));
      if (URLUtils.areCredentialsInURLSupported()) {
        url
          .username(SessionStore.getSessionId())
          .password('session');
      }

      return url.toString();
    }
  }
  return new URI(URLUtils.qualifyUrl('/notfound'));
};

const wrapOption = o => ({ label: o, value: o });
const defaultFields = ['timestamp', 'source', 'message'];
const defaultFieldOptions = defaultFields.map(wrapOption);

const CSVExport = ({ closeModal, availableStreams, availableFields }: Props) => {
  const [selectedStream, setSelectedStream] = useState();
  const [selectedFields, setSelectedFields] = useState(defaultFieldOptions);

  const link = selectedFields.length > 0
    ? (
      /* eslint-disable-next-line react/jsx-no-target-blank */
      <a href={getURLForExportAsCSV((selectedStream || {}).value, selectedFields.map(f => f.value))} target="_blank">
        <Icon name="cloud-download" />&nbsp;
        Download
      </a>
    )
    : <p>Select at least on field to export messages as CSV.</p>;

  const infoText = (URLUtils.areCredentialsInURLSupported()
    ? 'Please right click the download link below and choose "Save Link As..." to download the CSV file.'
    : 'Please click the download link below. Your browser may ask for your username and password to '
    + 'download the CSV file.');

  const Content = styled.div`
    margin-left: 10px;
    margin-right: 10px;
  `;
  return (
    <Modal show>
      <Modal.Header>
        <Modal.Title>Export search results as CSV</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <Content>
          <Row>
            <p>{infoText}</p>
          </Row>
          <Row>
            <span>Add stream to filter messages:</span>
            <Select placeholder="None: click to add stream"
                    onChange={selection => setSelectedStream(selection)}
                    options={availableStreams}
                    value={selectedStream} />
          </Row>
          <Row>
            <span>Select fields to export:</span>
            <Select placeholder="None: click to add fields"
                    onChange={selection => setSelectedFields(selection)}
                    options={availableFields}
                    value={selectedFields}
                    isMulti />
          </Row>
          <Row>
            {link}
          </Row>
        </Content>
      </Modal.Body>
      <Modal.Footer>
        <Button onClick={closeModal}>Close</Button>
      </Modal.Footer>
    </Modal>
  );
};

CSVExport.propTypes = {
  closeModal: PropTypes.func,
  availableStreams: PropTypes.array,
  availableFields: PropTypes.array,
};

CSVExport.defaultProps = {
  closeModal: () => {},
  availableStreams: [],
  availableFields: [],
};

export default connect(
  CSVExport,
  {
    availableStreams: StreamsStore,
    availableFields: FieldTypesStore,
  },
  ({ availableStreams: { streams }, availableFields: { all }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map(stream => ({ label: stream.title, value: stream.id })),
    availableFields: all.map(field => ({ label: field.name, value: field.name })),
  }),
);
