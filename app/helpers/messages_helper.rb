module MessagesHelper
  def get_quickfilter_selected filters, filter, not_to_i = false
    return nil if filters.blank? or filters[filter].blank?
    if not_to_i
      return filters[filter]
    else
      return filters[filter].to_i
    end
  end
end
