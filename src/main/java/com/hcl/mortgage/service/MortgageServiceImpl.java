package com.hcl.mortgage.service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Calendar;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.hcl.mortgage.dto.MortgageDetailsDto;
import com.hcl.mortgage.dto.MortgageDto;
import com.hcl.mortgage.entity.Account;
import com.hcl.mortgage.entity.Customer;
import com.hcl.mortgage.entity.Mortgage;
import com.hcl.mortgage.entity.Transaction;
import com.hcl.mortgage.exception.MortgageException;
import com.hcl.mortgage.repository.AccountRepository;
import com.hcl.mortgage.repository.CustomerRepository;
import com.hcl.mortgage.repository.MortgageRepository;
import com.hcl.mortgage.repository.TransactionRepository;
import com.hcl.mortgage.util.MortgageConstants;

@Service
public class MortgageServiceImpl implements IMortgageService {

	private static final Logger LOGGER = LoggerFactory.getLogger(MortgageServiceImpl.class);

	@Autowired
	MortgageRepository mortgageRepository;

	@Autowired
	CustomerRepository customerRepository;

	@Autowired
	AccountRepository accountRepository;

	@Autowired
	TransactionRepository transactionRepository;

	public MortgageDetailsDto signup(MortgageDto mortgageDto) {
		LOGGER.debug("MortgageServiceImpl:createMortgage");

		Account transactionalAccount = null;
		Account mortgageAccount = null;
		Customer customer = null;
		Mortgage mortgage = null;
		Transaction transaction = null;
		Transaction mortgageTransaction = null;
		MortgageDetailsDto mortgageDetailsDto = null;

		LocalDate birthDay = mortgageDto.getDob();

		if (mortgageDto.getPropertyCost() >= 100000 && mortgageDto.getDeposit() > 0) {
			if (validPhoneNumber(mortgageDto.getPhoneNumber())) {
				if (emailValidation(mortgageDto.getEmail())) {
					if (validAge(birthDay)) {

						mortgage = new Mortgage();
						customer = new Customer();
						transaction = new Transaction();
						mortgageTransaction = new Transaction();
						mortgageDetailsDto = new MortgageDetailsDto();
						Random random = new Random();

						customer.setLoginId(mortgageDto.getFirstName() + random.nextInt(1000));
						customer.setPassword(mortgageDto.getFirstName() + "@" + random.nextInt(100));
						customer.setCustomerName(mortgageDto.getFirstName() + random.nextInt(1000));

						Customer customerRepo = customerRepository.save(customer);

						transactionalAccount = new Account();
						transactionalAccount.setBalance(mortgageDto.getPropertyCost() - mortgageDto.getDeposit());
						transactionalAccount.setAccountNumber(
								MortgageConstants.TRANSACTIONAL_ACCOUNT_SUFFIX + random.nextInt(1000));
						transactionalAccount.setAccountType(MortgageConstants.TRANSACTION_ACCOUNT);
						transactionalAccount.setCreatedDate(LocalDate.now());
						transactionalAccount.setCustomerId(customerRepo.getCustomerId());

						accountRepository.save(transactionalAccount);

						mortgageAccount = new Account();
						mortgageAccount.setBalance(-(mortgageDto.getPropertyCost() - mortgageDto.getDeposit()));
						mortgageAccount
								.setAccountNumber(MortgageConstants.MORTGAGE_ACCOUNT_SUFFIX + random.nextInt(1000));
						mortgageAccount.setAccountType(MortgageConstants.MORTGAGE_ACCOUNT);
						mortgageAccount.setCreatedDate(LocalDate.now());
						mortgageAccount.setCustomerId(customerRepo.getCustomerId());

						accountRepository.save(mortgageAccount);

						BeanUtils.copyProperties(mortgageDto, mortgage,"dateOfBirth");
						mortgage.setDob(birthDay);
						mortgage.setCustomerId(customerRepo.getCustomerId());
						mortgageRepository.save(mortgage);

						transaction.setAccountNumber(transactionalAccount.getAccountNumber());
						transaction.setTransactionType(MortgageConstants.DEBIT);
						transaction.setAmount(mortgageDto.getDeposit());
						transaction.setTransactionDate(LocalDateTime.now());
						transaction.setDescription("credited to "+mortgageAccount.getAccountNumber());

						transactionRepository.save(transaction);

						mortgageTransaction.setAccountNumber(mortgageAccount.getAccountNumber());
						mortgageTransaction.setTransactionType(MortgageConstants.CREDIT);
						mortgageTransaction.setAmount(mortgageDto.getDeposit());
						mortgageTransaction.setTransactionDate(LocalDateTime.now());
						mortgageTransaction.setDescription("debited from  "+transactionalAccount.getAccountNumber());

						transactionRepository.save(mortgageTransaction);

						mortgageDetailsDto.setStatusCode(201);
						mortgageDetailsDto.setMessage(MortgageConstants.MORTGAGE_APPROVED_MESSAGE);

						return mortgageDetailsDto;

					} else {
						throw new MortgageException(MortgageConstants.CUSTOMER_AGE_MINIMUM_MESSAGE);
					}

				} else {
					throw new MortgageException(MortgageConstants.EMAIL_VALIDATION_MESSAGE);
				}

			} else {
				throw new MortgageException(MortgageConstants.PHONE_VALIDATION_MESSAGE);
			}
		} else {
			throw new MortgageException(MortgageConstants.PROPERTY_COST_VALIDATION_MESSAGE);
		}
	}

	static boolean validPhoneNumber(Long number) {
		String num = number.toString();
		Pattern p = Pattern.compile("^[0-9]{10}$");
		Matcher m = p.matcher(num);
		return (m.find() && m.group().equals(num));
	}

	static boolean validAge(LocalDate date1) {
		boolean result = false;
		int birthYear = date1.getYear();
		int year = Calendar.getInstance().get(Calendar.YEAR);
		int age = year - birthYear;
		if (age > 18) {
			result = true;
		}
		return result;
	}

	static boolean emailValidation(String email) {
		String regex = "^[\\w-_\\.+]*[\\w-_\\.]\\@([\\w]+\\.)+[\\w]+[\\w]$";
		return email.matches(regex);
	}
}