package org.trebol.product.application.port;

import java.util.function.Supplier;

public interface TransactionManagerPort {

    <T> T runInTransaction(Supplier<T> supplier);

    void runInTransaction(Runnable runnable);
}
