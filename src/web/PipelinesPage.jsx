import React, {PropTypes} from 'react';
import Reflux from 'reflux';

import { Row, Col } from 'react-bootstrap';
import PageHeader from 'components/common/PageHeader';
import Spinner from 'components/common/Spinner';

import CurrentUserStore from 'stores/users/CurrentUserStore';

import PipelinesActions from 'PipelinesActions';
import PipelinesStore from 'PipelinesStore';
import PipelinesComponent from 'PipelinesComponent';

import RulesActions from 'RulesActions';
import RulesStore from 'RulesStore';
import RulesComponent from 'RulesComponent';


const PipelinesPage = React.createClass({
  mixins: [Reflux.connect(CurrentUserStore), Reflux.connect(PipelinesStore), Reflux.connect(RulesStore)],

  getInitialState() {
    return {
      pipelines: undefined,
      rules: undefined,
    }
  },

  componentDidMount() {
    PipelinesActions.list();
    RulesActions.list();
  },

  loadData() {
    PipelinesActions.list();
  },

  render() {
    let content;
    if (!this.state.pipelines || !this.state.rules) {
      content = <Spinner />;
    } else {
      content = [
        <PipelinesComponent key="pipelines" pipelines={this.state.pipelines} />,
        <RulesComponent key="rules" rules={this.state.rules} />
      ];
    }
    return (
      <span>
        <PageHeader title="Processing pipelines">
          <span>Processing pipelines</span>
        </PageHeader>
        {content}
      </span>);
  },
});

export default PipelinesPage;
