$(document).ready(function() {
    $("#streampermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params) {
            var editSelect = $("#streameditpermissions");
            editSelect.find("option[value=" + params.deselected +"]").attr('selected', false);
            editSelect.chosen().trigger("chosen:updated");
        });

    $("#streameditpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params){
            if (params.selected) {
                var readSelect = $("#streampermissions");
                readSelect.find("option[value=" + params.selected +"]").attr('selected', true);
                readSelect.chosen().trigger("chosen:updated");
            }
        });

    $("#dashboardpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params) {
            var editSelect = $("#dashboardeditpermissions");
            editSelect.find("option[value=" + params.deselected +"]").attr('selected', false);
            editSelect.chosen().trigger("chosen:updated");
        });

    $("#dashboardeditpermissions")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(function(event, params){
            if (params.selected) {
                var readSelect = $("#dashboardpermissions");
                readSelect.find("option[value=" + params.selected +"]").attr('selected', true);
                readSelect.chosen().trigger("chosen:updated");
            }
        });

    var rolesChangeHandler = function(event, param) {
        // do not allow to unselect everything, but default to reader role
        var $rolesSelect = $(event.target);
        var adminSelected = $rolesSelect.find("option[value=Admin]:selected").length > 0;
        var readerSelected = $rolesSelect.find("option[value=Reader]:selected").length > 0;

        // Don't allow reader and admin at the same time, deselect admin if choosing reader
        if (param['selected'] === 'Reader' && adminSelected) {
            $rolesSelect.find("option[value=Admin]").attr("selected", false);
        }
        // always at least enable "Reader" role
        if (param['deselected'] === 'Reader' && !adminSelected) {
            $rolesSelect.find("option[value=Reader]").attr("selected", true);
        }

        // choosing Admin deselects reader
        if (param['selected'] === 'Admin' && readerSelected) {
            $rolesSelect.find("option[value=Reader]").attr("selected", false);
        }
        // fall back to reader role if admin is deselected
        if (param['deselected'] === 'Admin' && !readerSelected) {
            $rolesSelect.find("option[value=Reader]").attr("selected", true);
        }
        $rolesSelect.chosen().trigger("chosen:updated");
    };

    $("#edituserroles")
        .chosen({search_contains:true, width:"250px", inherit_select_classes:true})
        .change(rolesChangeHandler);

    $(".roles-select")
        .chosen({search_contains:true, inherit_select_classes:true, allow_single_deselect:false})
        .change(rolesChangeHandler);

});
