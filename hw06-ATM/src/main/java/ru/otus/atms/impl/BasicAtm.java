package ru.otus.atms.impl;

import ru.otus.atms.Atm;
import ru.otus.cells.Cell;
import ru.otus.domain.Banknote;
import ru.otus.exceptions.AtmModificationException;
import ru.otus.utils.CollectionUtils;

import java.util.*;
import java.util.function.Predicate;

public class BasicAtm implements Atm {

	private final Map<Banknote, Cell> balance;

	public BasicAtm(final Map<Banknote, Cell> balance) {
		this.balance = balance;
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

		final int value = banknote.getValue();
		final boolean isValidValue = value > 0;

		if (!isValidValue) {
			throw new AtmModificationException("banknote has not valid value. Value must be more 0");
		}

		final Predicate<Banknote> predicate = (monetaryValue) -> monetaryValue.getValue() == value;
		final Banknote foundBanknote = CollectionUtils.findValue(Arrays.asList(Banknote.values()), predicate);

		if (Objects.isNull(foundBanknote)) {
			throw new AtmModificationException("unknown banknote");
		}

		final Cell cell = this.balance.get(foundBanknote);

		cell.addBanknote(banknote);

		return calculateBalance();
	}
}
