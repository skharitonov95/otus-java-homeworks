package ru.otus.atms.impl;

import ru.otus.atms.Atm;
import ru.otus.cells.Cell;
import ru.otus.domain.Banknote;
import ru.otus.exceptions.AtmModificationException;

import java.util.*;

public class BasicAtm implements Atm {

	private Map<Banknote, Cell> balance;
	private final Snapshot snapshot;

	public BasicAtm(final Map<Banknote, Cell> balance) {
		this.balance = copyBalance(balance);
		this.snapshot = new Snapshot(balance);
	}

	@Override
	public int calculateBalance() {
		return balance.values().stream()
				.mapToInt(Cell::calculateBalance)
				.sum();
	}

	@Override
	public List<Banknote> debitFromBalance(final int value) throws AtmModificationException {
		if (value <= 0) {
			throw new AtmModificationException("not valid value. Value must be more 0");
		}

		final int balance = calculateBalance();

		if (value > balance) {
			throw new AtmModificationException("not enough money");
		}

		final boolean isValidValue = value % Banknote.FIFTY.getValue() == 0;

		if (!isValidValue) {
			throw new AtmModificationException("value must be a multiple of 50");
		}

		final List<Banknote> debitedBanknotes = new ArrayList<>();
		int difference = value;

		for (Map.Entry<Banknote, Cell> entry : this.balance.entrySet()) {
			final int monetaryValue = entry.getKey().getValue();
			final Cell cell = entry.getValue();
			final boolean isValidDifference = difference >= monetaryValue;

			if (!isValidDifference) {
				continue;
			}

			while (Math.abs(difference / monetaryValue) > 0 && difference > 0 && cell.size() > 0) {
				final Banknote banknote = cell.getBanknote();

				debitedBanknotes.add(banknote);
				difference -= monetaryValue;
			}
		}

		return debitedBanknotes;
	}

	@Override
	public int putOnBalance(final Banknote banknote) throws AtmModificationException {

		if (Objects.isNull(banknote)) {
			throw new AtmModificationException("banknote is null");
		}

		final Cell cell = this.balance.get(banknote);

		if (Objects.isNull(cell)) {
			throw new AtmModificationException("cell not found");
		}

		try {
			cell.addBanknote(banknote);
		} catch (IllegalArgumentException e) {
			throw new AtmModificationException(e.getMessage());
		}

		return calculateBalance();
	}

	@Override
	public void restart() {
		final Map<Banknote, Cell> balance = snapshot.balance;

		balance.values().forEach(Cell::reset);

		this.balance = copyBalance(balance);
	}

	private static Map<Banknote, Cell> copyBalance(final Map<Banknote, Cell> balance) {
		final Map<Banknote, Cell> copiedBalance = new TreeMap<>(Comparator.reverseOrder());

		copiedBalance.putAll(balance);

		return copiedBalance;
	}

	private static class Snapshot {
		private final Map<Banknote, Cell> balance;

		private Snapshot(final Map<Banknote, Cell> balance) {
			this.balance = copyBalance(balance);
		}
	}
}
