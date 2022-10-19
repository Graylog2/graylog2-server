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

import { Col, Row } from 'components/bootstrap';
import { DocumentTitle, PageHeader, Spinner } from 'components/common';
import DocumentationLink from 'components/support/DocumentationLink';
import ProcessorSimulator from 'components/simulator/ProcessorSimulator';
import DocsHelper from 'util/DocsHelper';
import StreamsStore from 'stores/streams/StreamsStore';
import PipelinesPageNavigation from 'components/pipelines/PipelinesPageNavigation';

class SimulatorPage extends React.Component {
  constructor(props) {
    super(props);

    this.state = {
      streams: undefined,
    };
  }

  componentDidMount() {
    StreamsStore.listStreams().then((streams) => {
      const filteredStreams = streams.filter((s) => s.is_editable);

      this.setState({ streams: filteredStreams });
    });
  }

  _isLoading = () => {
    const { streams } = this.state;

    return !streams;
  };

  render() {
    const { streams } = this.state;

    const content = this._isLoading() ? <Spinner /> : <ProcessorSimulator streams={streams} />;

    return (
      <DocumentTitle title="Simulate processing">
        <PipelinesPageNavigation />
        <PageHeader title="Simulate processing">
          <span>
            Processing messages can be complex. Use this page to simulate the result of processing an incoming
            message using your current set of pipelines and rules.
          </span>
          <span>
            Read more about Graylog pipelines in the <DocumentationLink page={DocsHelper.PAGES.PIPELINES} text="documentation" />.
          </span>
        </PageHeader>

        <Row className="content">
          <Col md={12}>
            {content}
          </Col>
        </Row>
      </DocumentTitle>
    );
  }
}

export default SimulatorPage;
