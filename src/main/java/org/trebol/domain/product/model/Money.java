package org.trebol.domain.product.model;

public class Money {
	private final int amount;

	public Money(int amount) {
		if (amount < 0) {
			throw new IllegalArgumentException("amount cannot be negative");
		}
		this.amount = amount;
	}

	public int amount() {
		return amount;
	}
}
