import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');

import { DocumentTitle, PageHeader } from 'components/common';
import OutputsComponent from 'components/outputs/OutputsComponent';

const SystemOutputsPage = React.createClass({
  propTypes: {
    currentUser: PropTypes.object.isRequired,
  },

  render() {
    return (
      <DocumentTitle title="Outputs">
        <span>
          <PageHeader title="Outputs in Cluster">
            <span>
              Graylog nodes can forward messages via outputs. Launch or terminate as many outputs as you want here{' '}
              <strong>and then assign them to streams to forward all messages of a stream in real-time.</strong>
            </span>

            <span>
              You can find output plugins in <a href="https://marketplace.graylog.org/" target="_blank">the Graylog Marketplace</a>.
            </span>
          </PageHeader>

          <OutputsComponent permissions={this.props.currentUser.permissions} />
        </span>
      </DocumentTitle>
    );
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(SystemOutputsPage));
