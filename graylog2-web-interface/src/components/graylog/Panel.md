### Basic example

```js
const PanelClickExample = () => {
  function handleClick() {
    alert('You have clicked on me');
  }

  return (
    <Panel onClick={handleClick}>Click me Example</Panel>
  )
};

<PanelClickExample />
```

### Panel w/ Header

```js
<Panel header="Panel heading without a title">
  Panel content
</Panel>
```

### Panel w/ Footer

```js
<Panel footer="Panel footer">
  Panel content
</Panel>
```


### Panel w/ Variants

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
