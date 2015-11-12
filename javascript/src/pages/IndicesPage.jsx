import React from 'react';
import Reflux from 'reflux';

import { DeflectorStore, IndexRangesStore, IndicesStore } from 'stores/indices';
import { DeflectorActions, IndexRangesActions, IndicesActions } from 'actions/indices';

import DocsHelper from 'util/DocsHelper';
import { PageHeader, Spinner } from 'components/common';
import { DocumentationLink } from 'components/support';
import { IndicesMaintenanceDropdown, IndicesOverview } from 'components/indices';

const IndicesPage = React.createClass({
  componentDidMount() {
    const timerId = setInterval(() => {
      DeflectorActions.config();
      DeflectorActions.list();

      IndexRangesActions.list();

      IndicesActions.list();
    }, this.REFRESH_INTERVAL);
    this.setState({timerId: timerId});
  },
  componentWillUnmount() {
    clearInterval(this.state.timerId);
  },
  mixins: [Reflux.connect(IndicesStore), Reflux.connect(DeflectorStore), Reflux.connect(IndexRangesStore)],
  REFRESH_INTERVAL: 2000,
  render() {
    if (!this.state.indices || !this.state.indexRanges || !this.state.deflector) {
      return <Spinner />;
    }
    return (
      <span>
        <PageHeader title="Indices">
          <span>
            This is an overview of all indices (message stores) Graylog is currently taking in account
            for searches and analysis. You can learn more about the index model in the{' '}
            <DocumentationLink page={DocsHelper.PAGES.INDEX_MODEL} text="documentation" />.
            Closed indices can be re-opened at any time.
          </span>

          <span>
          </span>

          <IndicesMaintenanceDropdown />
        </PageHeader>

        <IndicesOverview indices={this.state.indices} closedIndices={this.state.closedIndices}
                         deflector={this.state.deflector} indexRanges={this.state.indexRanges} />
      </span>
    );
  },
});

export default IndicesPage;
