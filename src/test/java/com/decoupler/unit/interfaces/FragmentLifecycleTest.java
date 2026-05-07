package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.Command;
import com.decoupler.interfaces.Fragment;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Fragment lifecycle: init, execute, close ordering.
 */
@ExtendWith(MockitoExtension.class)
class FragmentLifecycleTest {

    @Mock Fragment fragment;
    @Mock Command command;

    @Test
    void init_then_execute_then_close_in_order() {
        InOrder order = inOrder(fragment);

        fragment.init();
        fragment.execute(command);
        fragment.close();

        order.verify(fragment).init();
        order.verify(fragment).execute(command);
        order.verify(fragment).close();
    }

    @Test
    void init_is_called_once() {
        fragment.init();

        verify(fragment, times(1)).init();
    }

    @Test
    void close_is_called_once() {
        fragment.close();

        verify(fragment, times(1)).close();
    }
}
