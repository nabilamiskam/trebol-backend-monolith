package org.trebol.product.application.usecase;

import org.trebol.product.application.command.BulkPatchProductCommand;
import org.trebol.product.application.result.BulkPatchProductResult;

public interface PatchProductsUseCase {
    BulkPatchProductResult execute(BulkPatchProductCommand command);
}