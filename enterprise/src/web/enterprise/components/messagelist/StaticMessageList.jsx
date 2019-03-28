// @flow strict
import * as React from 'react';
import createReactClass from 'create-react-class';
import PropTypes from 'prop-types';
import ImmutablePropTypes from 'react-immutable-proptypes';
import { MenuItem } from 'react-bootstrap';

import style from 'pages/ShowDashboardPage.css';
// $FlowFixMe: imports from core need to be fixed in flow
import connect from 'stores/connect';

import { FieldTypesStore } from 'enterprise/stores/FieldTypesStore';
import FieldTypeMapping from 'enterprise/logic/fieldtypes/FieldTypeMapping';
import { SelectedFieldsActions, SelectedFieldsStore } from 'enterprise/stores/SelectedFieldsStore';
import MessagesWidgetConfig from 'enterprise/logic/widgets/MessagesWidgetConfig';

import WidgetHeader from '../widgets/WidgetHeader';
import WidgetActionDropdown from '../widgets/WidgetActionDropdown';
import EditWidgetFrame from '../widgets/EditWidgetFrame';
import MessageList from '../widgets/MessageList';
import MeasureDimensions from '../widgets/MeasureDimensions';
import widgetStyles from '../widgets/Widget.css';
import EditMessageList from '../widgets/EditMessageList';

import EmptyResultWidget from '../widgets/EmptyResultWidget';
import SaveOrCancelButtons from '../widgets/SaveOrCancelButtons';

import staticMessageListStyle from './StaticMessageList.css';

const StaticMessageList = createReactClass({
  propTypes: {
    fieldTypes: PropTypes.shape({
      all: ImmutablePropTypes.listOf(PropTypes.instanceOf(FieldTypeMapping)),
    }).isRequired,
    messages: PropTypes.shape({
      total: PropTypes.number.isRequired,
      messages: PropTypes.arrayOf(PropTypes.object).isRequired,
    }),
    onToggleMessages: PropTypes.func.isRequired,
    selectedFields: ImmutablePropTypes.setOf(PropTypes.string).isRequired,
    showMessages: PropTypes.bool.isRequired,
  },

  getDefaultProps() {
    return {
      messages: {
        total: 0,
        messages: [],
      },
    };
  },

  getInitialState() {
    return {
      editing: false,
      oldFields: undefined,
    };
  },

  _onEdit() {
    this.setState({
      editing: true,
      oldFields: this.props.selectedFields.toJS(),
    });
  },

  _onSave() {
    this.setState({ editing: false, oldFields: undefined });
  },

  _onCancel() {
    const { oldFields } = this.state;
    this.setState({ editing: false, oldFields: undefined }, () => SelectedFieldsActions.set(oldFields));
  },

  _onChangeFields(newConfig) {
    const { fields } = newConfig;
    SelectedFieldsActions.set(fields);
  },

  renderEditWidget() {
    if (!this.state.editing) {
      return undefined;
    }

    const config = MessagesWidgetConfig.builder().fields(this.props.selectedFields.toJS()).build();

    return (
      <EditWidgetFrame>
        <MeasureDimensions>
          <WidgetHeader hideDragHandle title="All Messages" />
          <EditMessageList fields={this.props.fieldTypes.all} containerHeight={100} config={config} onChange={this._onChangeFields}>
            <MessageList data={this.props.messages}
                         fields={this.props.fieldTypes.all}
                         pageSize={100} />
          </EditMessageList>
        </MeasureDimensions>
        <SaveOrCancelButtons onFinish={this._onSave} onCancel={this._onCancel} />
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
              <MenuItem onSelect={this._onEdit}>Edit</MenuItem>
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

export default connect(StaticMessageList, { fieldTypes: FieldTypesStore, selectedFields: SelectedFieldsStore });
