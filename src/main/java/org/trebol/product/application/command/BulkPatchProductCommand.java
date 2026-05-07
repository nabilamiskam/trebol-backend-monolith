package org.trebol.product.application.command;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

public record BulkPatchProductCommand(
    Map<String, String> requestParams,
    Map<String, Object> changes
) {
    private static final Set<String> TARGET_FILTER_KEYS = Set.of(
        "id",
        "code",
        "barcode",
        "name",
        "price",
        "barcodeLike",
        "nameLike"
    );

    private static final Set<String> PATCHABLE_FIELDS = Set.of("name", "price", "isActive");

    public BulkPatchProductCommand {
        requestParams = requestParams == null ? Map.of() : Map.copyOf(requestParams);
        changes = changes == null ? Map.of() : new LinkedHashMap<>(changes);

        if (!hasTargetFilter(requestParams)) {
            throw new IllegalArgumentException("At least one query filter is required to select target products");
        }
        if (changes.isEmpty()) {
            throw new IllegalArgumentException("At least one field must be provided in the patch body");
        }

        for (String fieldName : changes.keySet()) {
            if ("code".equals(fieldName)) {
                throw new IllegalArgumentException("Field 'code' cannot be patched");
            }
            if (!PATCHABLE_FIELDS.contains(fieldName)) {
                throw new IllegalArgumentException("Unsupported patch field: " + fieldName);
            }
        }
    }

    private static boolean hasTargetFilter(Map<String, String> requestParams) {
        return requestParams.keySet().stream().anyMatch(TARGET_FILTER_KEYS::contains);
    }
}