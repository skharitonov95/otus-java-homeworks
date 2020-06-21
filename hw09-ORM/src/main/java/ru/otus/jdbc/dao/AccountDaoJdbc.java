package ru.otus.jdbc.dao;

import ru.otus.core.dao.AccountDao;
import ru.otus.core.model.Account;
import ru.otus.core.sessionmanager.SessionManager;
import ru.otus.jdbc.DbExecutor;
import ru.otus.jdbc.DbExecutorImpl;
import ru.otus.jdbc.mapper.JdbcMapper;
import ru.otus.jdbc.mapper.impl.BasicJdbcMapper;
import ru.otus.jdbc.sessionmanager.SessionManagerJdbc;

import java.sql.Connection;
import java.util.Optional;

public class AccountDaoJdbc implements AccountDao {

	private final SessionManagerJdbc sessionManager;
	private final DbExecutor<Account> executor;

	public AccountDaoJdbc(SessionManagerJdbc sessionManager, DbExecutorImpl<Account> dbExecutor) {
		this.sessionManager = sessionManager;
		this.executor = dbExecutor;
	}

	@Override
	public Optional<Account> findById(long id) {
		final var mapper = getJdbcMapper();

		return Optional.ofNullable(mapper.findById(id, Account.class));
	}

	@Override
	public long insertAccount(Account account) {
		final var mapper = getJdbcMapper();

		mapper.insert(account);

		return account.getNo();
	}

	@Override
	public void updateAccount(final Account account) {
		final var mapper = getJdbcMapper();

		mapper.update(account);
	}

	@Override
	public void insertOrUpdate(final Account account) {
		final var mapper = getJdbcMapper();

		mapper.insertOrUpdate(account);
	}

	@Override
	public SessionManager getSessionManager() {
		return sessionManager;
	}

	private Connection getConnection() {
		return sessionManager.getCurrentSession().getConnection();
	}

	private JdbcMapper<Account> getJdbcMapper() {
		return new BasicJdbcMapper<>(Account.class, getConnection(), executor);
	}
}