package com.everis.fixedtermaccount.repository;

import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import com.everis.fixedtermaccount.model.fixedTermAccount;

@Repository
public interface fixedTermAccountRepository extends MongoRepository<fixedTermAccount, String> {
	boolean existsByAccountNumber(String number);

	boolean existsByIdCustomer(String id);

	fixedTermAccount findByAccountNumber(String number);
}
