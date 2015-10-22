import React, {PropTypes} from 'react';
import AddExtractor from 'components/extractors/AddExtractor';

const ExtractorsPage = React.createClass({
  propTypes: {
    params: PropTypes.object.isRequired,
  },
  render() {
    return (
      <div>
        <div>Ohai, there will be something else here</div>
        <AddExtractor inputId={this.props.params.inputId}/>
      </div>
    );
  },
});

export default ExtractorsPage;
