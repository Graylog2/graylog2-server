import React from 'react';
import Reflux from 'reflux';
import PageHeader from 'components/common/PageHeader';
import InputsList from 'components/inputs/InputsList';
import CurrentUserStore from 'stores/users/CurrentUserStore';

const InputsPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore)],
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
