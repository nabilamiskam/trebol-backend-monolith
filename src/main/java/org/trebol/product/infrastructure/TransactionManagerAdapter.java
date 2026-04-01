package org.trebol.product.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class TransactionManagerAdapter {

    @Transactional
    public void runInTransaction(Runnable runnable) {
        runnable.run();
    }
}
