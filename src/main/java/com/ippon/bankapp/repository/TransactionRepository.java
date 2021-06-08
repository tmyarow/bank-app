package com.ippon.bankapp.repository;

import com.ippon.bankapp.domain.Account;
import com.ippon.bankapp.domain.Transaction;
import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;

@Repository
public interface TransactionRepository extends CrudRepository<Transaction, String> {

    ArrayList<Transaction> findAllByAccount(Account account);

}