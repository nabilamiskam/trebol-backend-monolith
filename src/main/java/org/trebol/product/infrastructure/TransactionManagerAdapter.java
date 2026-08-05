package org.trebol.product.infrastructure;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.trebol.product.application.port.TransactionManagerPort;

import java.util.function.Supplier;

@Component
public class TransactionManagerAdapter implements TransactionManagerPort {

    private final TransactionTemplate transactionTemplate;

    public TransactionManagerAdapter(PlatformTransactionManager transactionManager) {
        this.transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @Override
    public <T> T runInTransaction(Supplier<T> supplier) {
        return transactionTemplate.execute(status -> supplier.get());
    }

    @Override
    public void runInTransaction(Runnable runnable) {
        runInTransaction(() -> {
            runnable.run();
            return null;
        });
    }
}
