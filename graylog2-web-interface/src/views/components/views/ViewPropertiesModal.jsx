import React from 'react';
import PropTypes from 'prop-types';
import { isEqual } from 'lodash';

import { Modal, Button } from 'components/graylog';
import FormsUtils from 'util/FormsUtils';
import Input from 'components/bootstrap/Input';

export default class ViewPropertiesModal extends React.Component {
  static propTypes = {
    view: PropTypes.object.isRequired,
    show: PropTypes.bool,
    title: PropTypes.string.isRequired,
    onSave: PropTypes.func.isRequired,
    onClose: PropTypes.func,
  };

  static defaultProps = {
    onClose: () => {},
    show: false,
  };

  constructor(props) {
    super(props);
    this.state = {
      view: props.view,
      show: props.show,
      title: props.title,
    };
  }

  componentWillReceiveProps(nextProps) {
    const { show, title } = this.props;
    const { view } = this.state;
    if (show !== nextProps.show || title !== nextProps.title || !isEqual(view, nextProps.view)) {
      this.setState({ view: nextProps.view, title: nextProps.title, show: nextProps.show });
    }
  }

  // eslint-disable-next-line consistent-return
  _onChange = (event) => {
    const { name } = event.target;
    const value = FormsUtils.getValueFromInput(event.target);

    switch (name) {
      case 'title': return this.setState(state => ({ view: state.view.toBuilder().title(value).build() }));
      case 'summary': return this.setState(state => ({ view: state.view.toBuilder().summary(value).build() }));
      case 'description': return this.setState(state => ({ view: state.view.toBuilder().description(value).build() }));
      default: break;
    }
  };

  _onClose = () => {
    const { onClose } = this.props;
    onClose();
  };

  _onSave = () => {
    const { onSave, onClose } = this.props;
    const { view } = this.state;
    onSave(view);
    onClose();
  };

  render() {
    const { show, view, title } = this.state;
    return (
      <Modal show={show} bsSize="large" onHide={this.handleClose}>
        <Modal.Header closeButton>
          <Modal.Title>{title}</Modal.Title>
        </Modal.Header>
        <Modal.Body>
          <Input id="title"
                 type="text"
                 name="title"
                 label="Title"
                 help="The title of the dashboard."
                 onChange={this._onChange}
                 value={view.title} />
          <Input id="summary"
                 type="text"
                 name="summary"
                 label="Summary"
                 help="A helpful summary of the dashboard."
                 onChange={this._onChange}
                 value={view.summary} />
          <Input id="description"
                 type="textarea"
                 name="description"
                 label="Description"
                 help="A longer, helpful description of the dashboard and its functionality."
                 onChange={this._onChange}
                 value={view.description} />
        </Modal.Body>
        <Modal.Footer>
          <Button onClick={this._onSave} bsStyle="success">Save</Button>
          <Button onClick={this._onClose}>Cancel</Button>
        </Modal.Footer>
      </Modal>
    );
  }
}
