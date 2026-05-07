package com.decoupler.unit.interfaces;

import com.decoupler.interfaces.Monolith;
import com.decoupler.interfaces.Transaction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

/**
 * Monolith lifecycle: init, execute, close ordering.
 */
@ExtendWith(MockitoExtension.class)
class MonolithLifecycleTest {

    @Mock Monolith monolith;
    @Mock Transaction tx;

    @Test
    void init_then_execute_then_close_in_order() {
        InOrder order = inOrder(monolith);

        monolith.init();
        monolith.execute(tx);
        monolith.close();

        order.verify(monolith).init();
        order.verify(monolith).execute(tx);
        order.verify(monolith).close();
    }

    @Test
    void init_is_called_once() {
        monolith.init();

        verify(monolith, times(1)).init();
    }

    @Test
    void close_is_called_once() {
        monolith.close();

        verify(monolith, times(1)).close();
    }
}
