import React from 'react';
import PropTypes from 'prop-types';
import { isEqual } from 'lodash';
import FormsUtils from 'util/FormsUtils';

import BootstrapModalForm from 'components/bootstrap/BootstrapModalForm';
import Input from 'components/bootstrap/Input';

export default class ViewPropertiesModal extends React.Component {
  modal = React.createRef();

  static propTypes = {
    onClose: PropTypes.func.isRequired,
    onSave: PropTypes.func.isRequired,
    show: PropTypes.bool.isRequired,
    title: PropTypes.string.isRequired,
    view: PropTypes.object.isRequired,
  };

  constructor(props) {
    super(props);
    this.state = {
      view: props.view,
      title: props.title,
    };
  }

  componentWillReceiveProps(nextProps) {
    const { title } = this.props;
    const { view } = this.state;
    if (title !== nextProps.title || !isEqual(view, nextProps.view)) {
      this.setState({ view: nextProps.view, title: nextProps.title });
    }
  }

  // eslint-disable-next-line consistent-return
  _onChange = (event) => {
    const { name } = event.target;
    let value = FormsUtils.getValueFromInput(event.target);
    const trimmedValue = value.trim();
    if (trimmedValue === '') {
      value = trimmedValue;
    }

    switch (name) {
      case 'title': return this.setState(state => ({ view: state.view.toBuilder().title(value).build() }));
      case 'summary': return this.setState(state => ({ view: state.view.toBuilder().summary(value).build() }));
      case 'description': return this.setState(state => ({ view: state.view.toBuilder().description(value).build() }));
      default: break;
    }
  };

  _cleanState = () => {
    const { view, title } = this.props;
    this.setState({ view, title });
  }

  _onSave = () => {
    const { onClose, onSave } = this.props;
    const { view } = this.state;
    onSave(view);
    onClose();
  };

  render() {
    const { view: { title = '', summary = '', description = '' }, title: modalTitle } = this.state;
    const { onClose, show } = this.props;
    return (
      <BootstrapModalForm show={show}
                          title={modalTitle}
                          onCancel={onClose}
                          onModalClose={this._cleanState}
                          onSubmitForm={this._onSave}
                          submitButtonText="Save"
                          bsSize="large">
        <Input id="title"
               type="text"
               name="title"
               label="Title"
               help="The title of the dashboard."
               required
               onChange={this._onChange}
               value={title} />
        <Input id="summary"
               type="text"
               name="summary"
               label="Summary"
               help="A helpful summary of the dashboard."
               onChange={this._onChange}
               value={summary} />
        <Input id="description"
               type="textarea"
               name="description"
               label="Description"
               help="A longer, helpful description of the dashboard and its functionality."
               onChange={this._onChange}
               value={description} />
      </BootstrapModalForm>
    );
  }
}
