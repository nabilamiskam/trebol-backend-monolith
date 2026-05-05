package org.trebol.product.adapter.outbound.persistence;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.trebol.product.domain.aggregate.ProductAggregate;
import org.trebol.product.domain.port.ProductRepository;
import org.trebol.product.domain.vo.ProductCode;
import org.trebol.product.domain.vo.ProductId;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Objects;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class ProductRepositoryAdapter implements ProductRepository {
    private final ProductJpaRepository jpaRepository;
    private final ProductPersistenceMapper mapper;

    public ProductRepositoryAdapter(ProductJpaRepository jpaRepository, ProductPersistenceMapper mapper) {
        this.jpaRepository = jpaRepository;
        this.mapper = mapper;
    }

    @Override
    public ProductAggregate save(ProductAggregate aggregate) {
        ProductJpaEntity saved = jpaRepository.save(mapper.toEntity(aggregate));
        return mapper.toAggregate(saved);
    }

    @Override
    public Optional<ProductAggregate> findById(ProductId id) {
        return jpaRepository.findById(id.value()).map(mapper::toAggregate);
    }

    @Override
    public Optional<ProductAggregate> findByCode(ProductCode code) {
        return jpaRepository.findByCode(code.value()).map(mapper::toAggregate);
    }

    @Override
    public List<ProductAggregate> findAll(int pageIndex, int pageSize, Map<String, String> requestParams) {
        Pageable pageable = PageRequest.of(pageIndex, pageSize, resolveSort(requestParams));
        Specification<ProductJpaEntity> specification = buildSpecification(requestParams);
        return jpaRepository.findAll(specification, pageable)
            .stream()
            .map(mapper::toAggregate)
            .toList();
    }

    @Override
    public long countAll(Map<String, String> requestParams) {
        Specification<ProductJpaEntity> specification = buildSpecification(requestParams);
        return jpaRepository.count(specification);
    }

    @Override
    public void deleteById(ProductId id) {
        jpaRepository.deleteById(id.value());
    }

    private Specification<ProductJpaEntity> buildSpecification(Map<String, String> requestParams) {
        return (root, query, criteriaBuilder) -> {
            if (requestParams == null || requestParams.isEmpty()) {
                return criteriaBuilder.conjunction();
            }

            List<Predicate> predicates = new ArrayList<>();
            requestParams.forEach((paramName, paramValue) -> {
                switch (paramName) {
                    case "id":
                        try {
                            predicates.add(criteriaBuilder.equal(root.get("id"), Long.valueOf(paramValue)));
                        }
                        catch (NumberFormatException ignored) {
                        }
                        break;
                    case "barcode":
                    case "code":
                        predicates.add(criteriaBuilder.equal(root.get("code"), paramValue));
                        break;
                    case "name":
                        predicates.add(criteriaBuilder.equal(root.get("name"), paramValue));
                        break;
                    case "price":
                        try {
                            predicates.add(criteriaBuilder.equal(root.get("price"), Integer.valueOf(paramValue)));
                        }
                        catch (NumberFormatException ignored) {
                        }
                        break;
                    case "barcodeLike":
                        predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("code")),
                            "%" + paramValue.toLowerCase() + "%"
                        ));
                        break;
                    case "nameLike":
                        predicates.add(criteriaBuilder.like(
                            criteriaBuilder.lower(root.get("name")),
                            "%" + paramValue.toLowerCase() + "%"
                        ));
                        break;
                    default:
                        break;
                }
            });

            if (predicates.isEmpty()) {
                return criteriaBuilder.conjunction();
            }
            return criteriaBuilder.and(predicates.toArray(new Predicate[0]));
        };
    }

    private Sort resolveSort(Map<String, String> requestParams) {
        if (requestParams == null || !requestParams.containsKey("sortBy")) {
            return Sort.unsorted();
        }

        String property = switch (requestParams.get("sortBy")) {
            case "id" -> "id";
            case "name" -> "name";
            case "barcode" -> "code";
            case "price" -> "price";
            default -> null;
        };

        if (property == null) {
            return Sort.unsorted();
        }

        Sort.Direction direction = "desc".equalsIgnoreCase(requestParams.get("order"))
            ? Sort.Direction.DESC
            : Sort.Direction.ASC;

        return Sort.by(direction, property);
    }
}
