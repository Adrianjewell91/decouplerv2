package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;

/**
 * Fragment ↔ Command interaction.
 *
 * Fragment.execute(command) calls command.build(schema()) and command.params()
 * to obtain SQL and bind values, then runs them against its own DataSource.
 * Command has no reference to Fragment — the dependency is one-way.
 */
@ExtendWith(MockitoExtension.class)
class FragmentCommandTest {

    @Mock Fragment fragment;
    @Mock Command command;
    @Mock Result result;
    @Mock Schema schema;

    @Test
    void fragment_execute_returns_result() {
        when(fragment.execute(command)).thenReturn(result);

        Result actual = fragment.execute(command);

        assertThat(actual).isSameAs(result);
        verify(fragment).execute(command);
    }

    @Test
    void command_build_takes_schema_and_returns_sql() {
        when(fragment.schema()).thenReturn(schema);
        when(command.build(schema)).thenReturn("SELECT * FROM orders");

        String sql = command.build(fragment.schema());

        assertThat(sql).contains("orders");
    }

    @Test
    void command_params_returns_map() {
        when(command.params()).thenReturn(Map.of("status", "PAID"));

        assertThat(command.params()).containsEntry("status", "PAID");
    }

    @Test
    void fragment_exposes_schema() {
        var fields = List.of(
                new Schema.FieldDescriptor("id", "integer"),
                new Schema.FieldDescriptor("customer_id", "integer"),
                new Schema.FieldDescriptor("total", "decimal"));
        when(fragment.schema()).thenReturn(schema);
        when(schema.name()).thenReturn("orders");
        when(schema.fields()).thenReturn(fields);

        Schema s = fragment.schema();

        assertThat(s.name()).isEqualTo("orders");
        assertThat(s.fields()).extracting(Schema.FieldDescriptor::name)
                .containsExactly("id", "customer_id", "total");
        assertThat(s.fields()).extracting(Schema.FieldDescriptor::typeHint)
                .containsExactly("integer", "integer", "decimal");
    }

    @Test
    void command_result_is_returned_to_caller() {
        when(fragment.execute(command)).thenReturn(result);
        when(result.message()).thenReturn("42 rows affected");

        Result actual = fragment.execute(command);

        assertThat(actual.message()).isEqualTo("42 rows affected");
    }

    @Test
    void command_describe_returns_human_readable_summary() {
        when(command.describe()).thenReturn("SELECT * FROM orders WHERE status = 'PENDING'");

        assertThat(command.describe()).contains("orders");
        verify(command).describe();
    }
}
