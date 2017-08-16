import React from 'react';
import PropTypes from 'prop-types';

const EditableOnClickField = React.createClass({
  propTypes: {
    value: PropTypes.string.isRequired,
    onChange: PropTypes.func.isRequired,
    children: PropTypes.node.isRequired,
  },
  getInitialState() {
    return {
      editing: false,
      value: this.props.value,
    };
  },
  componentDidMount() {
    document.addEventListener('mousedown', this._clickHandler);
  },
  componentWillUnmount() {
    document.removeEventListener('mousedown', this._clickHandler);
  },
  _clickHandler(event) {
    if (this.ref && !this.ref.contains(event.target) && this.state.editing) {
      this.setState({editing: false});
      if (this.state.value !== this.props.value) {
        this.props.onChange(this.state.value);
      }
    }
  },
  render() {
    if (this.state.editing) {
      return (<input ref={(ref) => { this.ref = ref; }}
                     type="text"
                     value={this.state.value}
                     onChange={event => this.setState({value: event.target.value})}/>);
    }
    return (
      <span onClick={() => this.setState({editing: true})}>
        {this.props.children}
      </span>
    );
  },
});

export default EditableOnClickField;
