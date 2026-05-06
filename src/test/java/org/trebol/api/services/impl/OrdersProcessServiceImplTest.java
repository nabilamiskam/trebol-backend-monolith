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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.trebol.config.Constants.*;

import java.util.Collection;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.trebol.api.models.OrderDetailPojo;
import org.trebol.api.models.OrderPojo;
import org.trebol.api.models.ProductPojo;
import org.trebol.common.exceptions.BadInputException;
import org.trebol.jpa.entities.Order;
import org.trebol.jpa.entities.OrderDetail;
import org.trebol.jpa.repositories.OrderDetailsRepository;
import org.trebol.jpa.repositories.OrderStatusesRepository;
import org.trebol.jpa.repositories.OrdersRepository;
import org.trebol.jpa.services.conversion.OrdersConverterService;
import org.trebol.jpa.services.conversion.ProductsConverterService;
import org.trebol.jpa.services.crud.OrdersCrudService;
import org.trebol.order.application.AbortPaymentUseCase;
import org.trebol.order.application.CompleteOrderUseCase;
import org.trebol.order.application.ConfirmOrderUseCase;
import org.trebol.order.application.FailPaymentUseCase;
import org.trebol.order.application.MarkPaidUseCase;
import org.trebol.order.application.RejectOrderUseCase;
import org.trebol.order.application.StartPaymentUseCase;
import org.trebol.testing.ProductsTestHelper;

@ExtendWith(MockitoExtension.class)
class OrdersProcessServiceImplTest {
    @InjectMocks
    OrdersProcessServiceImpl instance;
    @Mock
    OrdersCrudService crudServiceMock;
    @Mock
    OrdersRepository ordersRepositoryMock;
    @Mock
    OrderStatusesRepository orderStatusesRepositoryMock;
    @Mock
    OrderDetailsRepository orderDetailsRepositoryMock;
    @Mock
    OrdersConverterService sellConverterServiceMock;
    @Mock
    ProductsConverterService productConverterServiceMock;
    @Mock
    ConfirmOrderUseCase confirmOrderUseCaseMock;
    @Mock
    RejectOrderUseCase rejectOrderUseCaseMock;
    @Mock
    CompleteOrderUseCase completeOrderUseCaseMock;
    @Mock
    AbortPaymentUseCase abortPaymentUseCaseMock;
    @Mock
    FailPaymentUseCase failPaymentUseCaseMock;
    @Mock
    StartPaymentUseCase startPaymentUseCaseMock;
    @Mock
    MarkPaidUseCase markPaidUseCaseMock;
    final ProductsTestHelper productsHelper = new ProductsTestHelper();

    @BeforeEach
    void beforeEach() {
        productsHelper.resetProducts();
    }

    @Nested
class MarkAsStarted {

    @Test
    void markAsStarted_SellStatus_IsNotPending_BadInputException() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        // New behavior: service delegates, so stub use case to throw
        when(startPaymentUseCaseMock.startPayment(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsStarted(orderPojoMock));
    }

    @Test
    void markAsStarted_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(startPaymentUseCaseMock.startPayment(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: 2"));

        assertThrows(IllegalStateException.class, () -> instance.markAsStarted(orderPojoMock));
    }

    @Test
    void markAsStarted_ShouldReturn_SellPojo_WithStatusStarted() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order updatedOrderMock = new Order();
        updatedOrderMock.setId(1L);

        when(startPaymentUseCaseMock.startPayment(any(OrderPojo.class))).thenReturn(updatedOrderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_PAYMENT_STARTED, instance.markAsStarted(orderPojoMock).getStatus());
    }
}

    @Nested
class MarkAsAborted {

    @Test
    void markAsAborted_SellStatus_IsNotStarted_BadInputException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(abortPaymentUseCaseMock.abortPayment(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsAborted(orderPojoMock));
    }

    @Test
    void markAsAborted_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(abortPaymentUseCaseMock.abortPayment(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: -1"));

        assertThrows(IllegalStateException.class, () -> instance.markAsAborted(orderPojoMock));
    }

    @Test
    void markAsAborted_ShouldReturn_SellPojo_WithStatusCancelled() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        when(abortPaymentUseCaseMock.abortPayment(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_PAYMENT_CANCELLED, instance.markAsAborted(orderPojoMock).getStatus());
    }

    @Test
    void markAsAborted_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        OrderDetail orderDetailMock = new OrderDetail();
        orderDetailMock.setId(1L);
        orderDetailMock.setUnits(11);
        orderDetailMock.setUnitValue(111);
        List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

        ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

        when(abortPaymentUseCaseMock.abortPayment(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);
        when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
        when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

        Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsAborted(orderPojoMock).getDetails();
        OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

        assertEquals(1, actualSellDetailsPojo.size());
        assertEquals(11, actualOrderDetailPojo.getUnits());
        assertEquals(111, actualOrderDetailPojo.getUnitValue());
        assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
    }
}

    @Nested
class MarkAsFailed {

    @Test
    void markAsFailed_SellStatus_IsNotStarted_BadInputException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(failPaymentUseCaseMock.failPayment(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsFailed(orderPojoMock));
    }

    @Test
    void markAsFailed_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(failPaymentUseCaseMock.failPayment(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: -2"));

        assertThrows(IllegalStateException.class, () -> instance.markAsFailed(orderPojoMock));
    }

    @Test
    void markAsFailed_ShouldReturn_SellPojo_WithStatusFailed() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        when(failPaymentUseCaseMock.failPayment(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_PAYMENT_FAILED, instance.markAsFailed(orderPojoMock).getStatus());
    }

    @Test
    void markAsFailed_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        OrderDetail orderDetailMock = new OrderDetail();
        orderDetailMock.setId(1L);
        orderDetailMock.setUnits(11);
        orderDetailMock.setUnitValue(111);
        List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

        ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

        when(failPaymentUseCaseMock.failPayment(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);
        when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
        when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

        Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsFailed(orderPojoMock).getDetails();
        OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

        assertEquals(1, actualSellDetailsPojo.size());
        assertEquals(11, actualOrderDetailPojo.getUnits());
        assertEquals(111, actualOrderDetailPojo.getUnitValue());
        assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
    }
}

    @Nested
class MarkAsPaid {

    @Test
    void markAsPaid__SellStatus_IsNotStarted_BadInputException() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(markPaidUseCaseMock.markPaid(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsPaid(orderPojoMock));
    }

    @Test
    void markAsPaid_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(markPaidUseCaseMock.markPaid(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: 3"));

        assertThrows(IllegalStateException.class, () -> instance.markAsPaid(orderPojoMock));
    }

    @Test
    void markAsPaid_ShouldReturn_SellPojo_WithStatusUnconfirmed() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order updatedOrderMock = new Order();
        updatedOrderMock.setId(1L);

        when(markPaidUseCaseMock.markPaid(any(OrderPojo.class))).thenReturn(updatedOrderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_PAID_UNCONFIRMED, instance.markAsPaid(orderPojoMock).getStatus());
    }

    @Test
    void markAsPaid_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
        // Setup
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order updatedOrderMock = new Order();
        updatedOrderMock.setId(1L);

        OrderDetail orderDetailMock = new OrderDetail();
        orderDetailMock.setId(1L);
        orderDetailMock.setUnits(11);
        orderDetailMock.setUnitValue(111);
        List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

        ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

        // New behavior: stub use case return
        when(markPaidUseCaseMock.markPaid(any(OrderPojo.class))).thenReturn(updatedOrderMock);

        // Converter called with returned Order
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        // Details still built in service
        when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
        when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

        Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsPaid(orderPojoMock).getDetails();
        OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

        assertEquals(1, actualSellDetailsPojo.size());
        assertEquals(11, actualOrderDetailPojo.getUnits());
        assertEquals(111, actualOrderDetailPojo.getUnitValue());
        assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
    }
}

    @Nested
    class MarkAsConfirmed {

    @Test
void markAsConfirmed__SellStatus_IsNotStarted_BadInputException() throws BadInputException {
    OrderPojo orderPojoMock = OrderPojo.builder().build();

    when(confirmOrderUseCaseMock.confirm(any(OrderPojo.class)))
        .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

    assertThrows(BadInputException.class, () -> instance.markAsConfirmed(orderPojoMock));
}

        @Test
void markAsConfirmed_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
    OrderPojo orderPojoMock = OrderPojo.builder().build();

    when(confirmOrderUseCaseMock.confirm(any(OrderPojo.class)))
        .thenThrow(new IllegalStateException("No status matches code: 4"));

    assertThrows(IllegalStateException.class, () -> instance.markAsConfirmed(orderPojoMock));
}

    @Test
void markAsConfirmed_ShouldReturn_SellPojo_WithStatusConfirmed() throws BadInputException {
    OrderPojo orderPojoMock = OrderPojo.builder().build();

    Order orderMock = new Order();
    orderMock.setId(1L);

    when(confirmOrderUseCaseMock.confirm(any(OrderPojo.class))).thenReturn(orderMock);
    when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

    assertEquals(ORDER_STATUS_PAID_CONFIRMED, instance.markAsConfirmed(orderPojoMock).getStatus());
}

        @Test
void markAsConfirmed_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
    OrderPojo orderPojoMock = OrderPojo.builder().build();

    Order orderMock = new Order();
    orderMock.setId(1L);

    OrderDetail orderDetailMock = new OrderDetail();
    orderDetailMock.setId(1L);
    orderDetailMock.setUnits(11);
    orderDetailMock.setUnitValue(111);
    List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

    ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

    when(confirmOrderUseCaseMock.confirm(any(OrderPojo.class))).thenReturn(orderMock);
    when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);
    when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
    when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

    Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsConfirmed(orderPojoMock).getDetails();
    OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

    assertEquals(1, actualSellDetailsPojo.size());
    assertEquals(11, actualOrderDetailPojo.getUnits());
    assertEquals(111, actualOrderDetailPojo.getUnitValue());
    assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
}
    }

    @Nested
class MarkAsRejected {

    @Test
    void markAsRejected__SellStatus_IsNotStarted_BadInputException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(rejectOrderUseCaseMock.reject(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsRejected(orderPojoMock));
    }

    @Test
    void markAsRejected_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(rejectOrderUseCaseMock.reject(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: -3"));

        assertThrows(IllegalStateException.class, () -> instance.markAsRejected(orderPojoMock));
    }

    @Test
    void markAsRejected_ShouldReturn_SellPojo_WithStatusRejected() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        when(rejectOrderUseCaseMock.reject(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_REJECTED, instance.markAsRejected(orderPojoMock).getStatus());
    }

    @Test
    void markAsRejected_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        OrderDetail orderDetailMock = new OrderDetail();
        orderDetailMock.setId(1L);
        orderDetailMock.setUnits(11);
        orderDetailMock.setUnitValue(111);
        List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

        ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

        when(rejectOrderUseCaseMock.reject(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);
        when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
        when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

        Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsRejected(orderPojoMock).getDetails();
        OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

        assertEquals(1, actualSellDetailsPojo.size());
        assertEquals(11, actualOrderDetailPojo.getUnits());
        assertEquals(111, actualOrderDetailPojo.getUnitValue());
        assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
    }
}

    @Nested
class MarkAsCompleted {

    @Test
    void markAsCompleted__SellStatus_IsNotStarted_BadInputException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(completeOrderUseCaseMock.complete(any(OrderPojo.class)))
            .thenThrow(new BadInputException("The transaction is not in a valid state for this api"));

        assertThrows(BadInputException.class, () -> instance.markAsCompleted(orderPojoMock));
    }

    @Test
    void markAsCompleted_SellStatus_IsNotInRepo_IllegalStateException() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        when(completeOrderUseCaseMock.complete(any(OrderPojo.class)))
            .thenThrow(new IllegalStateException("No status matches code: 6"));

        assertThrows(IllegalStateException.class, () -> instance.markAsCompleted(orderPojoMock));
    }

    @Test
    void markAsCompleted_ShouldReturn_SellPojo_WithStatusCompleted() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        when(completeOrderUseCaseMock.complete(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);

        assertEquals(ORDER_STATUS_COMPLETED, instance.markAsCompleted(orderPojoMock).getStatus());
    }

    @Test
    void markAsCompleted_ShouldReturn_SellPojo_WithCorrectDetails() throws BadInputException {
        OrderPojo orderPojoMock = OrderPojo.builder().build();

        Order orderMock = new Order();
        orderMock.setId(1L);

        OrderDetail orderDetailMock = new OrderDetail();
        orderDetailMock.setId(1L);
        orderDetailMock.setUnits(11);
        orderDetailMock.setUnitValue(111);
        List<OrderDetail> orderDetailsMock = List.of(orderDetailMock);

        ProductPojo productPojoMock = productsHelper.productPojoAfterCreationWithoutCategory();

        when(completeOrderUseCaseMock.complete(any(OrderPojo.class))).thenReturn(orderMock);
        when(sellConverterServiceMock.convertToPojo(any())).thenReturn(orderPojoMock);
        when(orderDetailsRepositoryMock.findBySellId(any())).thenReturn(orderDetailsMock);
        when(productConverterServiceMock.convertToPojo(any())).thenReturn(productPojoMock);

        Collection<OrderDetailPojo> actualSellDetailsPojo = instance.markAsCompleted(orderPojoMock).getDetails();
        OrderDetailPojo actualOrderDetailPojo = actualSellDetailsPojo.iterator().next();

        assertEquals(1, actualSellDetailsPojo.size());
        assertEquals(11, actualOrderDetailPojo.getUnits());
        assertEquals(111, actualOrderDetailPojo.getUnitValue());
        assertEquals(productPojoMock, actualOrderDetailPojo.getProduct());
    }
}
}
