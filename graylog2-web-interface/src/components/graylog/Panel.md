### Basic example

```js
const PanelClickExample = () => {
  function handleClick() {
    alert('You have clicked on me');
  }

  return (
    <Panel onClick={handleClick}>
      <Panel.Body>
        Basic panel example
      </Panel.Body>
    </Panel>
  )
};

<PanelClickExample />
```

### Panel w/ Header

```js
<Panel>
  <Panel.Heading>Panel heading without a title</Panel.Heading>
  <Panel.Body>Panel content</Panel.Body>
</Panel>
```

### Panel w/ Header & Title

```js
<Panel>
  <Panel.Heading>
    <Panel.Title componentClass="h3">Panel heading with a title</Panel.Title>
  </Panel.Heading>
  <Panel.Body>Panel content</Panel.Body>
</Panel>
```

### Panel w/ Footer

```js
<Panel>
  <Panel.Body>Panel content</Panel.Body>
  <Panel.Footer>Panel footer</Panel.Footer>
</Panel>
```

### Panel w/ Variants

```js
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <Panel bsStyle={style.toLowerCase()}
           key={`button-${style}-${i}`}>
      <Panel.Heading>{`${style} Heading`}</Panel.Heading>
      <Panel.Body>Panel content</Panel.Body>
    </Panel>
  )
})
```

### Collapsible Panel

```js
import { Button } from 'components/graylog';

const PanelCollapseExample = () => {
  const [expanded, setExpanded] = React.useState(false);

  return (
    <div>
      <p>
        <Button onClick={() => setExpanded(!expanded)}>
          Click to {expanded ? 'Close' : 'Open'}
        </Button>
      </p>

      <Panel expanded={expanded}>
        <Panel.Collapse>
          <Panel.Body>
            Anim pariatur cliche reprehenderit, enim eiusmod high life
            accusamus terry richardson ad squid. Nihil anim keffiyeh
            helvetica, craft beer labore wes anderson cred nesciunt sapiente
            ea proident.
          </Panel.Body>
        </Panel.Collapse>
      </Panel>
     </div>
  );
};

<>
  <PanelCollapseExample />

  <p>You can also make the Panel heading toggle the collapse.</p>

  <Panel id="collapsible-panel-example-2" defaultExpanded>
    <Panel.Heading>
      <Panel.Title toggle>
        Title that functions as a collapse toggle
      </Panel.Title>
    </Panel.Heading>
    <Panel.Collapse>
      <Panel.Body>
        Anim pariatur cliche reprehenderit, enim eiusmod high life
        accusamus terry richardson ad squid. Nihil anim keffiyeh
        helvetica, craft beer labore wes anderson cred nesciunt sapiente
        ea proident.
      </Panel.Body>
    </Panel.Collapse>
  </Panel>

  <p>Or use a Panel.Toggle component to customize</p>

  <Panel id="collapsible-panel-example-3" defaultExpanded>
    <Panel.Heading>
      <Panel.Title>Title that functions as a collapse toggle</Panel.Title>
      <Panel.Toggle componentClass="a">My own toggle</Panel.Toggle>
    </Panel.Heading>
    <Panel.Collapse>
      <Panel.Body>
        Anim pariatur cliche reprehenderit, enim eiusmod high life
        accusamus terry richardson ad squid. Nihil anim keffiyeh
        helvetica, craft beer labore wes anderson cred nesciunt sapiente
        ea proident.
      </Panel.Body>
    </Panel.Collapse>
  </Panel>
</>
```

## Deprecated Examples

### Deprecated Basic example

```js
const DeprecatedPanelClickExample = () => {
  function handleClick() {
    alert('You have clicked on me');
  }

  return (
    <Panel onClick={handleClick}>Click me Example</Panel>
  )
};

<DeprecatedPanelClickExample />
```

### Deprecated Panel w/ Header

```js
<Panel header="Panel heading without a title">
  Panel content
</Panel>
```

### Deprecated Panel w/ Footer

```js
<Panel footer="Panel footer">
  Panel content
</Panel>
```


### Deprecated Panel w/ Variants

```js
const styles = ['Primary', 'Danger', 'Warning', 'Success', 'Info', 'Default'];

styles.map((style, i) => {
  return (
    <Panel bsStyle={style.toLowerCase()}
           key={`button-${style}-${i}`}
           header={`${style} Heading`}>
      Lorem ipsum dolor sit amet consectetur adipisicing elit.
    </Panel>
  )
})
```

### Deprecated Collapsible Panel

```js
import { Button } from 'components/graylog';

const DeprecatedPanelCollapseExample = () => {
   const [expanded, setExpanded] = React.useState(false);

  return (
    <div>
      <p>
        <Button onClick={() => setExpanded(!expanded)}>
          Click to Toggle
        </Button>
      </p>

      <Panel collapsible expanded={expanded}>
        Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid.
        Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
      </Panel>
    </div>
  )
};

<>
  <DeprecatedPanelCollapseExample />

  <Panel collapsible
         defaultExpanded
         header="Click to Toggle">
    Anim pariatur cliche reprehenderit, enim eiusmod high life accusamus terry richardson ad squid.
    Nihil anim keffiyeh helvetica, craft beer labore wes anderson cred nesciunt sapiente ea proident.
  </Panel>
</>
```
