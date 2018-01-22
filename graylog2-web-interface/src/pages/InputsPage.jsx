import React from 'react';
import PropTypes from 'prop-types';
import { inject, observer } from 'mobx-react';

import { DocumentTitle, PageHeader } from 'components/common';
import { InputsList } from 'components/inputs';

import StoreProvider from 'injection/StoreProvider';

const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const InputStatesStore = StoreProvider.getStore('InputStates');

const InputsPage = React.createClass({
  propTypes: {
    currentUser: PropTypes.object.isRequired,
  },

  componentDidMount() {
    this.interval = setInterval(InputStatesStore.list, 2000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  render() {
    return (
      <DocumentTitle title="Inputs">
        <div>
          <PageHeader title="Inputs">
            <span>Graylog nodes accept data via inputs. Launch or terminate as many inputs as you want here.</span>
          </PageHeader>
          <InputsList permissions={this.props.currentUser.permissions} />
        </div>
      </DocumentTitle>
    );
  },
});

export default inject(() => ({
  currentUser: CurrentUserStore.currentUser,
}))(observer(InputsPage));
