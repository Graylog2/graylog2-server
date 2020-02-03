```js
import { FormControl, FormGroup, InputGroup, Button, DropdownButton, MenuItem } from 'components/graylog';
import Icon from 'components/common/Icon';

class FormExample extends React.Component {
  constructor(props, context) {
    super(props, context);
    this.handleChange = this.handleChange.bind(this);
    this.state = {
      value: ''
    };
  }

  getValidationState() {
    const length = this.state.value.length;
    if (length > 10) return 'success';
    if (length > 5) return 'warning';
    if (length > 0) return 'error';
    return null;
  }

  handleChange(e) {
    this.setState({ value: e.target.value });
  }

  render() {
    return (
      <form>
        <FormGroup validationState={this.getValidationState()}>
          <InputGroup>
            <InputGroup.Addon>@</InputGroup.Addon>
            <FormControl type="text"
                         value={this.state.value}
                         placeholder="Example w/ Validation"
                         onChange={this.handleChange} />
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <FormControl type="text" />
            <InputGroup.Addon>.00</InputGroup.Addon>
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <InputGroup.Addon>$</InputGroup.Addon>
            <FormControl type="text" />
            <InputGroup.Addon>.00</InputGroup.Addon>
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <FormControl type="text" />
            <InputGroup.Addon>
              <Icon name="music" />
            </InputGroup.Addon>
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <InputGroup.Button>
              <Button>Before</Button>
            </InputGroup.Button>
            <FormControl type="text" />
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <FormControl type="text" />
            <DropdownButton
              componentClass={InputGroup.Button}
              id="input-dropdown-addon"
              title="Action"
            >
              <MenuItem key="1">Item</MenuItem>
            </DropdownButton>
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <InputGroup.Addon>
              <input type="radio" aria-label="..." />
            </InputGroup.Addon>
            <FormControl type="text" />
          </InputGroup>
        </FormGroup>
        <FormGroup>
          <InputGroup>
            <InputGroup.Addon>
              <input type="checkbox" aria-label="..." />
            </InputGroup.Addon>
            <FormControl type="text" />
          </InputGroup>
        </FormGroup>
      </form>
    );
  };
};

<FormExample />
```
