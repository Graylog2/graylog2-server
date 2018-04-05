import React from 'react';
import PropTypes from 'prop-types';
import { Button, Modal } from 'react-bootstrap';

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
    if (this.props.show !== nextProps.show) {
      this.setState({ show: nextProps.show });
    }
  };

  _onChange = (event) => {
    const name = event.target.name;
    const value = FormsUtils.getValueFromInput(event.target);
    this.setState(state => ({ view: state.view.set(name, value) }));
  };

  _onClose = () => this.props.onClose();
  _onSave = () => {
    this.props.onSave(this.state.view);
    this.props.onClose();
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
                 help="The title of the view."
                 onChange={this._onChange}
                 value={view.title} />
          <Input id="summary"
                 type="text"
                 name="summary"
                 label="Summary"
                 help="A helpful summary of the view."
                 onChange={this._onChange}
                 value={view.summary} />
          <Input id="description"
                 type="textarea"
                 name="description"
                 label="Description"
                 help="A longer, helpful description of the view and its functionality."
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
};
