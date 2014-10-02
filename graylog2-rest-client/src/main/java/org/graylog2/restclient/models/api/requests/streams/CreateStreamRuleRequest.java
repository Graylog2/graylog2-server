package org.graylog2.restclient.models.api.requests.streams;

import org.graylog2.restclient.models.api.requests.ApiRequest;
import play.data.validation.Constraints;

/**
 * @author Dennis Oelkers <dennis@torch.sh>
 */
public class CreateStreamRuleRequest extends ApiRequest {
    public String field;
    @Constraints.Required
    public Integer type;
    public String value;
    public Boolean inverted;

    public String getField() {
        return field;
    }

    public void setField(String field) {
        this.field = field;
    }

    public Integer getType() {
        return type;
    }

    public void setType(Integer type) {
        this.type = type;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }

    public Boolean getInverted() {
        return inverted;
    }

    public void setInverted(Boolean inverted) {
        this.inverted = inverted;
    }
}
