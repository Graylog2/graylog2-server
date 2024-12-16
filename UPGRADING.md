Upgrading to Graylog 6.2.x
==========================

## Breaking Changes

### Plugins

Adjustment of `enterpriseWidgets` web interface plugin. The `editComponent` attribute now no longer has a `onSubmit` prop.
Before this change the prop had to be called to close the widget edit mode. Now it is enough to call `applyAllWidgetChanges` from the `WidgetEditApplyAllChangesContext`.
Alternatively the `SaveOrCancelButtons` component can be used in the edit component for custom widgets. It renders a cancel and submit button and calls `applyAllWidgetChanges` on submit.

## Configuration File Changes

| Option        | Action     | Description                                    |
|---------------|------------|------------------------------------------------|
| `tbd`         | **added**  |                                                |

## Default Configuration Changes

- tbd

## Java API Changes

The following Java Code API changes have been made.

| File/method                                    | Description |
|------------------------------------------------|-------------|
| `org.graylog.scheduler.JobSchedule#toDBUpdate` | removed     |

## REST API Endpoint Changes

The following REST API changes have been made.

| Endpoint                                         | Description                                                                                                                     |
|--------------------------------------------------|---------------------------------------------------------------------------------------------------------------------------------|
| `GET /tbd`                                       | tbd                                                                                                                             |
