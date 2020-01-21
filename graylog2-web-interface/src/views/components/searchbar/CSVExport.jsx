// @flow strict
import React, { useState } from 'react';
import PropTypes from 'prop-types';
import URI from 'urijs';
import connect from 'stores/connect';

import { StreamsStore } from 'views/stores/StreamsStore';
import { ViewStore } from 'views/stores/ViewStore';
import URLUtils from 'util/URLUtils';
import { Modal, Button } from 'components/graylog';
import { Icon } from 'components/common';
import ApiRoutes from 'routing/ApiRoutes';
import StoreProvider from 'injection/StoreProvider';
import View from 'views/logic/views/View';
import Select from 'views/components/Select';

const SessionStore = StoreProvider.getStore('Session');

type StreamEntry = {
  label: String,
  value: String,
};

type Props = {
  availableStreams: Array<StreamEntry>,
  closeModal: () => void,
};

const getURLForExportAsCSV = (selectedStream: ?String) => {
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
          ['timestamp', 'message', 'source'],
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

const CSVExport = ({ closeModal, availableStreams }: Props) => {
  const [selectedStream, setSelectedStream] = useState();

  const infoText = (URLUtils.areCredentialsInURLSupported()
    ? 'Please right click the download link below and choose "Save Link As..." to download the CSV file.'
    : 'Please click the download link below. Your browser may ask for your username and password to '
    + 'download the CSV file.');
  return (
    <Modal show>
      <Modal.Header>
        <Modal.Title>Export search results as CVS</Modal.Title>
      </Modal.Header>
      <Modal.Body>
        <p>{infoText}</p>
        <div>
          <span>Add stream to filter message:</span>
          <Select placeholder="None: click to add stream"
                  onChange={selection => setSelectedStream(selection)}
                  options={availableStreams}
                  value={selectedStream} />
        </div>
        <p>
          {/* eslint-disable-next-line react/jsx-no-target-blank */}
          <a href={getURLForExportAsCSV((selectedStream || {}).value)} target="_blank">
            <Icon name="cloud-download" />&nbsp;
            Download
          </a>
        </p>
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
};

CSVExport.defaultProps = {
  closeModal: () => {},
  availableStreams: [],
};

export default connect(
  CSVExport,
  {
    availableStreams: StreamsStore,
  },
  ({ availableStreams: { streams }, ...rest }) => ({
    ...rest,
    availableStreams: streams.map(stream => ({ label: stream.title, value: stream.id })),
  }),
);
