module MessagesHelper
  def get_quickfilter_selected filters, filter, convert_to = :nothing
    return nil if filters.blank? or filters[filter].blank?

    begin
      case convert_to
        when :integer then return filters[filter].to_i
        when :bson_id then return BSON::ObjectId(filters[filter])
        else return filters[filter] # No converting
      end
    rescue => e
      return filters[filter]
    end

  end

  def format_additional_field_value(key, value)
    return "".html_safe if key.blank? or value.blank?
    res = CGI::escapeHTML(value.to_s)
    res = "<pre>#{res}</pre>" if res.include?("\n")
    res.html_safe
  end

  def additional_field_link_target(key, value, stream_id)
    res = {
      :controller => :messages,
      "filters[additional][keys][]" => key,
      "filters[additional][values][]" => value
    }

    unless stream_id.blank?
      res[:stream_id] = stream_id
    end

    res
  end

end
