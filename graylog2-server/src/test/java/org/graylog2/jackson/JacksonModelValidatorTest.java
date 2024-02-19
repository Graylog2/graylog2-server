package org.graylog2.jackson;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.module.SimpleModule;
import com.google.auto.value.AutoValue;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JacksonModelValidatorTest {
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new SimpleModule("Test")
                    .setSerializerModifier(JacksonModelValidator.getBeanSerializerModifier()));

    // Well configured subtypes don't throw an error.
    @Nested
    class WithExistingProperty {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = "employee", value = Employee.class),
                @JsonSubTypes.Type(name = "customer", value = Customer.class),
        })
        private interface Person {
            @JsonProperty("type")
            String type();
        }

        record Employee(@JsonProperty("type") String type,
                        @JsonProperty("employee_number") String employeeNumber) implements Person {
        }

        record Customer(@JsonProperty("type") String type,
                        @JsonProperty("customer_number") String customerNumber) implements Person {
        }

        @Test
        void serialize() throws Exception {
            assertThat(objectMapper.readValue(objectMapper.writeValueAsString(new Employee("employee", "emp-123")), Person.class))
                    .isInstanceOf(Employee.class);
            assertThat(objectMapper.readValue(objectMapper.writeValueAsString(new Customer("customer", "cus-123")), Person.class))
                    .isInstanceOf(Customer.class);
        }

        @Test
        void check() {
            assertThatNoException().isThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Employee.class);
            });
            assertThatNoException().isThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Customer.class);
            });
        }
    }

    // JsonTypeInfo.As.EXISTING_PROPERTY but without the existing field.
    @Nested
    class WithExistingPropertyWithoutPropertyDefined {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.EXISTING_PROPERTY, property = "type", visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = "employee", value = Employee.class),
                @JsonSubTypes.Type(name = "customer", value = Customer.class),
        })
        private interface Person {
        }

        record Employee(@JsonProperty("employee_number") String employeeNumber) implements Person {
        }

        record Customer(@JsonProperty("customer_number") String customerNumber) implements Person {
        }

        @Test
        void serialize() throws Exception {
            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(new Employee("emp-123")), Person.class);
            }).hasMessageContaining("doesn't exist as property: type");

            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(new Customer("cus-123")), Person.class);
            }).hasMessageContaining("doesn't exist as property: type");
        }

        @Test
        void check() {
            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Employee.class);
            }).hasMessageContaining("doesn't exist as property: type");

            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Customer.class);
            }).hasMessageContaining("doesn't exist as property: type");
        }
    }

    // JsonTypeInfo.As.Property and a conflicting field name in the objects.
    @Nested
    class WithPropertyAndConflictingField {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type", visible = true)
        @JsonSubTypes({
                @JsonSubTypes.Type(name = "employee", value = Employee.class),
                @JsonSubTypes.Type(name = "customer", value = Customer.class),
        })
        private interface Person {
            @JsonProperty("type")
            String type();
        }

        record Employee(@JsonProperty("type") String type,
                        @JsonProperty("employee_number") String employeeNumber) implements Person {
        }

        record Customer(@JsonProperty("type") String type,
                        @JsonProperty("customer_number") String customerNumber) implements Person {
        }

        @Test
        void serialize() {
            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(new Employee("employee", "emp-123")), Person.class);
            }).hasMessageContaining("conflicts with existing property: type");

            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(new Customer("customer", "cus-123")), Person.class);
            }).hasMessageContaining("conflicts with existing property: type");
        }

        @Test
        void check() {
            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Employee.class);
            }).hasMessageContaining("conflicts with existing property: type");

            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Customer.class);
            }).hasMessageContaining("conflicts with existing property: type");
        }
    }

    // JsonTypeInfo.As.Property and missing JsonTypeName annotations with abstract classes.
    @Nested
    class WithPropertyAndMissingTypeNames {
        @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, include = JsonTypeInfo.As.PROPERTY, property = "type")
        @JsonSubTypes({
                @JsonSubTypes.Type(name = "employee", value = Employee.class),
                @JsonSubTypes.Type(name = "customer", value = Customer.class),
        })
        private interface Person {
        }

        @AutoValue
        static abstract class Employee implements Person {
            @JsonProperty("employee_number")
            public abstract String employeeNumber();

            @JsonCreator
            public static Employee create(@JsonProperty("employee_number") String employeeNumber) {
                return new AutoValue_JacksonModelValidatorTest_WithPropertyAndMissingTypeNames_Employee(employeeNumber);
            }
        }

        @AutoValue
        static abstract class Customer implements Person {
            @JsonProperty("customer_number")
            public abstract String customerNumber();

            @JsonCreator
            public static Customer create(@JsonProperty("customer_number") String customerNumber) {
                return new AutoValue_JacksonModelValidatorTest_WithPropertyAndMissingTypeNames_Customer(customerNumber);
            }
        }

        @Test
        void serialize() {
            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(Employee.create("emp-123")), Person.class);
            }).hasMessageContaining("must have a @JsonTypeName annotation");

            assertThatThrownBy(() -> {
                objectMapper.readValue(objectMapper.writeValueAsString(Customer.create("cus-123")), Person.class);
            }).hasMessageContaining("must have a @JsonTypeName annotation");
        }

        @Test
        void check() {
            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Employee.class);
            }).hasMessageContaining("must have a @JsonTypeName annotation");

            assertThatThrownBy(() -> {
                JacksonModelValidator.check("test", objectMapper, Customer.class);
            }).hasMessageContaining("must have a @JsonTypeName annotation");
        }
    }
}
