package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.Schema;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Schema.FieldDescriptor: record contract — construction, accessors, equality.
 */
class SchemaFieldDescriptorTest {

    @Test
    void field_descriptor_exposes_name_and_type_hint() {
        var fd = new Schema.FieldDescriptor("customer_id", "integer");

        assertThat(fd.name()).isEqualTo("customer_id");
        assertThat(fd.typeHint()).isEqualTo("integer");
    }

    @Test
    void field_descriptor_with_null_type_hint_is_valid() {
        var fd = new Schema.FieldDescriptor("notes", null);

        assertThat(fd.name()).isEqualTo("notes");
        assertThat(fd.typeHint()).isNull();
    }

    @Test
    void equal_descriptors_with_same_values() {
        var a = new Schema.FieldDescriptor("id", "integer");
        var b = new Schema.FieldDescriptor("id", "integer");

        assertThat(a).isEqualTo(b);
        assertThat(a.hashCode()).isEqualTo(b.hashCode());
    }

    @Test
    void descriptors_with_different_names_are_not_equal() {
        var a = new Schema.FieldDescriptor("id", "integer");
        var b = new Schema.FieldDescriptor("total", "integer");

        assertThat(a).isNotEqualTo(b);
    }

    @Test
    void descriptors_with_different_type_hints_are_not_equal() {
        var a = new Schema.FieldDescriptor("amount", "integer");
        var b = new Schema.FieldDescriptor("amount", "decimal");

        assertThat(a).isNotEqualTo(b);
    }
}
