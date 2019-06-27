import React from 'react';
import PropTypes from 'prop-types';
import { Button, OverlayTrigger, Popover } from 'react-bootstrap';

import connect from 'stores/connect';

import QueryInput from '../searchbar/AsyncQueryInput';
import { SearchStore } from '../../stores/SearchStore';

import style from './WidgetFilterMenu.css';

class WidgetFilterMenu extends React.Component {
  static propTypes = {
    children: PropTypes.node.isRequired,
    onChange: PropTypes.func.isRequired,
    value: PropTypes.string,
  };

  static defaultProps = {
    value: undefined,
  };

  constructor(props, context) {
    super(props, context);
    this.state = {
      filter: props.value,
    };
  }

  componentWillReceiveProps(nextProps) {
    if (this.props.value !== nextProps.value) {
      this.setState({ filter: nextProps.value });
    }
  }

  _onUpdate = () => {
    this.props.onChange(this.state.filter);
    this.overlayTrigger.hide();
  };

  _onClose = () => this.setState({ filter: this.props.value });

  render() {
    const popoverBottom = (
      <Popover id="popover-positioned-bottom" title="Widget Filter">
        <div className={style.flavorText}>
          You can limit the results used by this widget by adding a custom filter here.
        </div>
        <div className={style.filterInput}>
          <QueryInput onChange={value => this.setState({ filter: value })}
                      onExecute={this._onUpdate}
                      placeholder="Add new widget filter"
                      maxLines={10}
                      tabSize={0}
                      wrapEnabled
                      value={this.state.filter} />
        </div>
        <div className="pull-right" style={{ marginBottom: '10px', marginTop: '10px' }}>
          <Button bsStyle="success" onClick={this._onUpdate}>Done</Button>
        </div>
      </Popover>
    );
    return (
      <OverlayTrigger ref={(elem) => { this.overlayTrigger = elem; }}
                      trigger="click"
                      placement="bottom"
                      overlay={popoverBottom}
                      onExited={this._onClose}
                      rootClose>
        {this.props.children}
      </OverlayTrigger>
    );
  }
}

export default connect(WidgetFilterMenu, { search: SearchStore });
