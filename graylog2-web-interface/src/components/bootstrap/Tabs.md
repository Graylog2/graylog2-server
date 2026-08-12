### Uncontrolled

```js
<Tabs defaultValue="tab1">
  <Tabs.List>
    <Tabs.Tab value="tab1">Tab 1</Tabs.Tab>
    <Tabs.Tab value="tab2">Tab 2</Tabs.Tab>
    <Tabs.Tab value="tab3" disabled>
      Tab 3
    </Tabs.Tab>
  </Tabs.List>
  <Tabs.Panel value="tab1">Tab 1 content</Tabs.Panel>
  <Tabs.Panel value="tab2">Tab 2 content</Tabs.Panel>
  <Tabs.Panel value="tab3">Tab 3 content</Tabs.Panel>
</Tabs>
```

### Controlled

```js
const TabExample = () => {
  const [activeTab, setActiveTab] = React.useState('tab1');

  return (
    <Tabs value={activeTab} onChange={setActiveTab}>
      <Tabs.List>
        <Tabs.Tab value="tab1">Tab 1</Tabs.Tab>
        <Tabs.Tab value="tab2">Tab 2</Tabs.Tab>
        <Tabs.Tab value="tab3">Tab 3</Tabs.Tab>
      </Tabs.List>
      <Tabs.Panel value="tab1">Tab 1 content</Tabs.Panel>
      <Tabs.Panel value="tab2">Tab 2 content</Tabs.Panel>
      <Tabs.Panel value="tab3">Tab 3 content</Tabs.Panel>
    </Tabs>
  );
};

<TabExample />;
```

### API

Tab labels (`Tabs.List` / `Tabs.Tab`) and tab panels (`Tabs.Panel`) are declared separately. Each `Tabs.Tab` and `Tabs.Panel` share a `value` string that links them together.

**Tabs props**

| Prop           | Type                                | Default     | Description                         |
| -------------- | ----------------------------------- | ----------- | ----------------------------------- |
| `defaultValue` | `string`                            | —           | Initially active tab (uncontrolled) |
| `value`        | `string \| null`                    | —           | Active tab (controlled)             |
| `onChange`     | `(value: string \| null) => void`   | —           | Called when the active tab changes  |
| `variant`      | `'default' \| 'outline' \| 'pills'` | `'outline'` | Visual style                        |

**Keyboard navigation**

Arrow keys move focus between tabs; `Home`/`End` jump to the first/last tab.
