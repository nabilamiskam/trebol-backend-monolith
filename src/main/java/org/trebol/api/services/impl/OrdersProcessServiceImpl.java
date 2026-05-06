/*
 * Copyright (c) 2020-2024 The Trebol eCommerce Project
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software
 * and associated documentation files (the "Software"), to deal in the Software without restriction,
 * including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense,
 * and/or sell copies of the Software, and to permit persons to whom the Software is furnished
 * to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial
 * portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED,
 * INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A
 * PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION
 * OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE
 * SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 */

package org.trebol.api.services.impl;

import static org.trebol.config.Constants.*;

import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.trebol.api.models.OrderDetailPojo;
import org.trebol.api.models.OrderPojo;
import org.trebol.api.models.ProductPojo;
import org.trebol.api.services.OrdersProcessService;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderDetail;
import org.trebol.jpa.repositories.OrderDetailsRepository;
import org.trebol.jpa.services.conversion.OrdersConverterService;
import org.trebol.jpa.services.conversion.ProductsConverterService;
import org.trebol.order.application.AbortPaymentUseCase;
import org.trebol.order.application.CompleteOrderUseCase;
import org.trebol.order.application.ConfirmOrderUseCase;
import org.trebol.order.application.FailPaymentUseCase;
import org.trebol.order.application.MarkPaidUseCase;
import org.trebol.order.application.RejectOrderUseCase;
import org.trebol.order.application.StartPaymentUseCase;

import jakarta.persistence.EntityNotFoundException;

@Transactional
@Service
public class OrdersProcessServiceImpl
    implements OrdersProcessService {
    private final OrderDetailsRepository orderDetailsRepository;
    private final OrdersConverterService converterService;
    private final ProductsConverterService productConverterService;
    private final ConfirmOrderUseCase confirmOrderUseCase;
    private final RejectOrderUseCase rejectOrderUseCase;
    private final CompleteOrderUseCase completeOrderUseCase;
    private final AbortPaymentUseCase abortPaymentUseCase;
    private final FailPaymentUseCase failPaymentUseCase;
    private final StartPaymentUseCase startPaymentUseCase;
    private final MarkPaidUseCase markPaidUseCase;

    public OrdersProcessServiceImpl(
        OrderDetailsRepository orderDetailsRepository,
        OrdersConverterService converterService,
        ProductsConverterService productConverterService,
        ConfirmOrderUseCase confirmOrderUseCase,
        RejectOrderUseCase rejectOrderUseCase,
        CompleteOrderUseCase completeOrderUseCase,
        AbortPaymentUseCase abortPaymentUseCase,
        FailPaymentUseCase failPaymentUseCase,
        StartPaymentUseCase startPaymentUseCase,
        MarkPaidUseCase markPaidUseCase

    ) {
        this.orderDetailsRepository = orderDetailsRepository;
        this.converterService = converterService;
        this.productConverterService = productConverterService;
        this.confirmOrderUseCase = confirmOrderUseCase;
        this.rejectOrderUseCase = rejectOrderUseCase;
        this.completeOrderUseCase = completeOrderUseCase;
        this.abortPaymentUseCase = abortPaymentUseCase;
        this.failPaymentUseCase = failPaymentUseCase;
        this.startPaymentUseCase = startPaymentUseCase;
        this.markPaidUseCase = markPaidUseCase;
    }
    @Override
public OrderPojo markAsStarted(OrderPojo sell) throws BadInputException, EntityNotFoundException {
    Order updated = startPaymentUseCase.startPayment(sell);

    OrderPojo target = this.convertOrThrowException(updated);
    target.setStatus(ORDER_STATUS_PAYMENT_STARTED);
    return target;
}

    @Override
public OrderPojo markAsAborted(OrderPojo sell)
    throws BadInputException, EntityNotFoundException {

    Order existingOrder = abortPaymentUseCase.abortPayment(sell);

    OrderPojo target = this.convertOrThrowException(existingOrder);

    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }
    target.setDetails(pojoDetails);
    target.setStatus(ORDER_STATUS_PAYMENT_CANCELLED);

    return target;
}

@Override
public OrderPojo markAsFailed(OrderPojo sell)
    throws BadInputException, EntityNotFoundException {

    Order existingOrder = failPaymentUseCase.failPayment(sell);

    OrderPojo target = this.convertOrThrowException(existingOrder);

    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }
    target.setDetails(pojoDetails);
    target.setStatus(ORDER_STATUS_PAYMENT_FAILED);

    return target;
}

    @Override
public OrderPojo markAsPaid(OrderPojo sell) throws BadInputException, EntityNotFoundException {
    // Delegate the transition (2 -> 3) to the use case
    Order updatedOrder = markPaidUseCase.markPaid(sell);

    // Convert base order fields
    OrderPojo target = this.convertOrThrowException(updatedOrder);

    // Keep your existing detail aggregation logic
    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(updatedOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }

    target.setStatus(ORDER_STATUS_PAID_UNCONFIRMED);
    target.setDetails(pojoDetails);

    return target;
}

    @Override
public OrderPojo markAsConfirmed(OrderPojo sell)
    throws BadInputException, EntityNotFoundException {

    // NEW: delegate state transition + persistence update to application layer
    Order existingOrder = confirmOrderUseCase.confirm(sell);

    // keep the rest the same (conversion + details)
    OrderPojo target = this.convertOrThrowException(existingOrder);

    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }
    target.setDetails(pojoDetails);

    // response status string stays the same (API compatibility)
    target.setStatus(ORDER_STATUS_PAID_CONFIRMED);

    return target;
}

    @Override
public OrderPojo markAsRejected(OrderPojo sell)
    throws BadInputException, EntityNotFoundException {

    Order existingOrder = rejectOrderUseCase.reject(sell);

    OrderPojo target = this.convertOrThrowException(existingOrder);

    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }
    target.setDetails(pojoDetails);
    target.setStatus(ORDER_STATUS_REJECTED);

    return target;
}

    @Override
public OrderPojo markAsCompleted(OrderPojo sell)
    throws BadInputException, EntityNotFoundException {

    Order existingOrder = completeOrderUseCase.complete(sell);

    OrderPojo target = this.convertOrThrowException(existingOrder);

    List<OrderDetailPojo> pojoDetails = new ArrayList<>();
    for (OrderDetail detail : orderDetailsRepository.findBySellId(existingOrder.getId())) {
        ProductPojo productPojo = productConverterService.convertToPojo(detail.getProduct());
        OrderDetailPojo orderDetailPojo = OrderDetailPojo.builder()
            .units(detail.getUnits())
            .unitValue(detail.getUnitValue())
            .product(productPojo)
            .build();
        pojoDetails.add(orderDetailPojo);
    }
    target.setDetails(pojoDetails);
    target.setStatus(ORDER_STATUS_COMPLETED);

    return target;
}
    private OrderPojo convertOrThrowException(Order existingOrder) {
    OrderPojo target = converterService.convertToPojo(existingOrder);
    if (target == null) {
        throw new IllegalStateException("Converter could not turn Sell into its Pojo equivalent");
    }
    return target;
    }
}
