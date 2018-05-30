import React from 'react';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';

import style from 'pages/ShowDashboardPage.css';
import connect from 'stores/connect';
import WidgetHeader from '../widgets/WidgetHeader';
import MessageList from '../widgets/MessageList';
import { FieldTypesStore } from '../../stores/FieldTypesStore';
import FieldTypeMapping from '../../logic/fieldtypes/FieldTypeMapping';

const StaticMessageList = ({ showMessages, onToggleMessages, fieldTypes, messages }) => (
  <div className={style.widgetContainer}>
    <div className="widget">
      <span style={{ fontSize: 10 }} onClick={onToggleMessages}><i className="fa fa-bars pull-right" /></span>
      {showMessages ? <WidgetHeader title="Messages" /> :
        <span style={{ fontSize: 12 }}>Messages</span>}
      {showMessages && <MessageList data={messages} fields={fieldTypes.all} pageSize={100} />}
    </div>
  </div>
);

StaticMessageList.propTypes = {
  fieldTypes: PropTypes.shape({
    all: ImmutablePropTypes.listOf(PropTypes.instanceOf(FieldTypeMapping)),
  }).isRequired,
  messages: PropTypes.shape({
    messages: PropTypes.arrayOf(PropTypes.object).isRequired,
  }).isRequired,
  onToggleMessages: PropTypes.func.isRequired,
  showMessages: PropTypes.bool.isRequired,
};

export default connect(StaticMessageList, { fieldTypes: FieldTypesStore });
