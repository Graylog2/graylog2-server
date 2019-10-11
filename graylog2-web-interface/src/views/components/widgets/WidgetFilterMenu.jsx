import React from 'react';
import PropTypes from 'prop-types';

import { OverlayTrigger, Popover, Button } from 'components/graylog';
import QueryInput from '../searchbar/AsyncQueryInput';

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
    const { value } = this.props;
    if (value !== nextProps.value) {
      this.setState({ filter: nextProps.value });
    }
  }

  _onUpdate = () => {
    const { onChange } = this.props;
    const { filter } = this.state;
    onChange(filter);
    this.overlayTrigger.hide();
  };

  // eslint-disable-next-line react/destructuring-assignment
  _onClose = () => this.setState({ filter: this.props.value });

  render() {
    const { children } = this.props;
    const { filter } = this.state;
    const popoverBottom = (
      <Popover id="popover-positioned-bottom" title="Widget Filter">
        <div className={style.flavorText}>
          You can limit the results used by this widget by adding a custom filter here.
        </div>
        <div className={style.filterInput}>
          <QueryInput onChange={value => new Promise(resolve => this.setState({ filter: value }, resolve))}
                      onExecute={this._onUpdate}
                      placeholder="Add new widget filter"
                      maxLines={10}
                      tabSize={0}
                      wrapEnabled
                      value={filter} />
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
        {children}
      </OverlayTrigger>
    );
  }
}

export default WidgetFilterMenu;
