import React from 'react';
import Reflux from 'reflux';

import StoreProvider from 'injection/StoreProvider';
const CurrentUserStore = StoreProvider.getStore('CurrentUser');
const InputStatesStore = StoreProvider.getStore('InputStates');

import { PageHeader } from 'components/common';
import { InputsList } from 'components/inputs';

const InputsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
  componentDidMount() {
    this.interval = setInterval(InputStatesStore.list, 2000);
  },
  componentWillUnmount() {
    clearInterval(this.interval);
  },
  render() {
    return (
      <div>
        <PageHeader title="Inputs">
          <span>Graylog nodes accept data via inputs. Launch or terminate as many inputs as you want here.</span>
        </PageHeader>
        <InputsList permissions={this.state.currentUser.permissions}/>
      </div>
    );
  },
});

export default InputsPage;
