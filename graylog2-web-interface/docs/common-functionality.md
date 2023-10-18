### Setting up keyboard shortcuts

If you want to add a keyboard shortcut, you need to:
- extend the `hotkeysCollections` in the `hotkeysProvider`.
- call `useHotkey` hook in the place which provides the functionality you want to execute on keypress.

### Handling date times

When receiving or sending dates to the backend they are always in UTC and expressed according to ISO 8601, for example `2010-07-30T16:03:25.000Z`.
This is also the preferred format to store date times in the state of, for example, UI components.   

When displaying date times in the UI they are always displayed in the user timezone.
The [`useUserDateTime`](#useuserdatetime-1) hook provides the related functionality, including the timezone of the current user.
If you just want to display a date time, you can render the `DateTime` component, which implements methods provided by the `useUserDateTime` hook.

For all other cases where you need to transform a date time you can use the `util/DateTime` utils.  
