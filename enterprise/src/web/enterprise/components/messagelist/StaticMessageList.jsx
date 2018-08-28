import React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { MenuItem } from 'react-bootstrap';

import style from 'pages/ShowDashboardPage.css';
import connect from 'stores/connect';

import WidgetHeader from '../widgets/WidgetHeader';
import WidgetActionDropdown from '../widgets/WidgetActionDropdown';
import EditWidgetFrame from '../widgets/EditWidgetFrame';
import MessageList from '../widgets/MessageList';
import MeasureDimensions from '../widgets/MeasureDimensions';
import { FieldTypesStore } from '../../stores/FieldTypesStore';
import FieldTypeMapping from '../../logic/fieldtypes/FieldTypeMapping';
import widgetStyles from '../widgets/Widget.css';

import staticMessageListStyle from './StaticMessageList.css';
import EditMessageList from '../widgets/EditMessageList';
import EmptyResultWidget from '../widgets/EmptyResultWidget';

const StaticMessageList = createReactClass({
  propTypes: {
    fieldTypes: PropTypes.shape({
      all: ImmutablePropTypes.listOf(PropTypes.instanceOf(FieldTypeMapping)),
    }).isRequired,
    messages: PropTypes.shape({
      total: PropTypes.number.isRequired,
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    }).isRequired,
    onToggleMessages: PropTypes.func.isRequired,
    showMessages: PropTypes.bool.isRequired,
  },

  getInitialState() {
    return {
      editing: false,
    };
  },

  renderEditWidget() {
    if (!this.state.editing) {
      return undefined;
    }

    const widgetActionDropdownCaret = <i className={`fa fa-chevron-down ${widgetStyles.widgetActionDropdownCaret} ${widgetStyles.tonedDown}`} />;
    let container;
    return (
      <EditWidgetFrame>
        <span ref={(node) => { container = node; }}>
          <span className="pull-right">
            <WidgetActionDropdown element={widgetActionDropdownCaret} container={() => container}>
              <MenuItem onSelect={() => { this.setState({ editing: !this.state.editing }); }}>Finish Editing</MenuItem>
            </WidgetActionDropdown>
          </span>
          <MeasureDimensions>
            <WidgetHeader hideDragHandle title="All Messages" />
            <EditMessageList fields={this.props.fieldTypes.all}>
              <MessageList data={this.props.messages}
                           fields={this.props.fieldTypes.all}
                           pageSize={100} />
            </EditMessageList>
          </MeasureDimensions>
        </span>
      </EditWidgetFrame>
    );
  },

  renderWidget() {
    const widgetActionDropdownCaret = <i className={`fa fa-chevron-down ${widgetStyles.widgetActionDropdownCaret} ${widgetStyles.tonedDown}`} />;
    const messageList = this.props.messages.total > 0 ? (
      <MessageList data={this.props.messages}
                   fields={this.props.fieldTypes.all}
                   pageSize={100} />
    ) : <EmptyResultWidget />;
    return (
      <div className={style.widgetContainer}>
        <div className="widget">
          <span role="button" tabIndex={0} style={{ fontSize: 10 }} onClick={this.props.onToggleMessages}>
            <i className="fa fa-bars pull-right" />
          </span>
          <span className={`pull-right ${staticMessageListStyle.carret}`}>
            <WidgetActionDropdown element={widgetActionDropdownCaret}>
              <MenuItem onSelect={() => { this.setState({ editing: !this.state.editing }); }}>Edit</MenuItem>
            </WidgetActionDropdown>
          </span>
          {this.props.showMessages ? <WidgetHeader hideDragHandle title="All Messages" /> : <span style={{ fontSize: 12 }}>Messages</span>}
          {this.props.showMessages && messageList}
        </div>
      </div>
    );
  },

  render() {
    return (
      <span>
        {this.renderWidget()}
        {this.renderEditWidget()}
      </span>
    );
  },
});

export default connect(StaticMessageList, { fieldTypes: FieldTypesStore });
