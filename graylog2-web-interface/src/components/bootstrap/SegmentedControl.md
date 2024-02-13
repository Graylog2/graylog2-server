#### Default

```tsx
<SegmentedControl data={['Aloho', 'Mora', 'Lumos']}/>
```

#### Disabled

```tsx
<SegmentedControl data={['Aloho', 'Mora', 'Lumos']} disabled/>
```

#### Single option disabled

```tsx
<SegmentedControl data={[
  {
    value: 'aloho',
    label: 'Aloho',
  },
  {
    value: 'mora',
    label: 'Mora',
    disabled: true
  },
  {
    value: 'lumos',
    label: 'Lumos',
  }
]}/>
```

