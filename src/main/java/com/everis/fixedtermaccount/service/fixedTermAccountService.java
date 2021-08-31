package com.everis.fixedtermaccount.service;

import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.everis.fixedtermaccount.consumer.webclient;
import com.everis.fixedtermaccount.dto.customer;
import com.everis.fixedtermaccount.dto.message;
import com.everis.fixedtermaccount.model.fixedTermAccount;
import com.everis.fixedtermaccount.model.movements;
import com.everis.fixedtermaccount.repository.fixedTermAccountRepository;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

@Service
@Transactional
public class fixedTermAccountService {
	@Autowired
	fixedTermAccountRepository reposirtory;

	private final List<String> operations = Arrays.asList("Retiro", "Deposito", "Trasnferencia", "Comision");

	private Boolean verifyCustomer(String id) {
		return webclient.customer.get().uri("/verifyId/{id}", id).retrieve().bodyToMono(Boolean.class).block();
	}

	private customer customerFind(String id) {
		return webclient.customer.get().uri("/{id}", id).retrieve().bodyToMono(customer.class).block();
	}

	private Boolean verifyNumberCC(String number) {
		return webclient.currentAccount.get().uri("/verifyByNumberAccount/" + number).retrieve()
				.bodyToMono(Boolean.class).block();
	}

	private Boolean verifyNumberSC(String number) {
		return webclient.savingAccount.get().uri("/verifyByNumberAccount/" + number).retrieve()
				.bodyToMono(Boolean.class).block();
	}

	private Boolean verifyNumberFC(String number) {
		return webclient.fixedAccount.get().uri("/verifyByNumberAccount/" + number).retrieve().bodyToMono(Boolean.class)
				.block();
	}

	private Boolean verifyCE(String number) {
		if (verifyNumberCC(number) || verifyNumberSC(number) || verifyNumberFC(number))
			return true;
		return false;
	}

	private Boolean verifyCR(String number) {
		if (verifyNumberCC(number) || verifyNumberSC(number) || verifyNumberFC(number))
			return true;
		return false;
	}

	private double getAmountByNumber(String number) {
		return reposirtory.findByAccountNumber(number).getAmount();
	}

	private String addMovements(movements movement) {
		double val = getAmountByNumber(movement.getAccountEmisor());
		fixedTermAccount model = reposirtory.findByAccountNumber(movement.getAccountEmisor());

		if (movement.getType().equals("Deposito")) {
			model.setAmount(movement.getAmount() + val);
			model.getMovements().add(movement);
		} else {
			if (movement.getAmount() > val)
				return "Cantidad insuficiente.";
			else {
				if (movement.getType().equals("Trasnferencia") && movement.getAccountRecep() != null) {
					if (verifyCR(movement.getAccountRecep())) {
						if (verifyNumberCC(movement.getAccountRecep())) {
							webclient.currentAccount.post().uri("/addTransfer")
									.body(Mono.just(movement), movements.class).retrieve().bodyToMono(Object.class)
									.subscribe();
						}
						if (verifyNumberSC(movement.getAccountRecep())) {
							webclient.savingAccount.post().uri("/addTransfer")
									.body(Mono.just(movement), movements.class).retrieve().bodyToMono(Object.class)
									.subscribe();
						}
						if (verifyNumberFC(movement.getAccountRecep())) {
							webclient.fixedAccount.post().uri("/addTransfer").body(Mono.just(movement), movements.class)
									.retrieve().bodyToMono(Object.class).subscribe();
						}
					} else
						return "Cuenta receptora no exciste.";
				}

				model.setAmount(val - movement.getAmount());
				model.getMovements().add(movement);
			}
		}

		reposirtory.save(model);
		return "Movimiento realizado";
	}

	public Mono<Object> save(fixedTermAccount model) {
		String msg = "Cuenta creada.";

		if (verifyCustomer(model.getIdCustomer())) {
			String typeCustomer = customerFind(model.getIdCustomer()).getType();

			if (typeCustomer.equals("personal")) {
				if (!reposirtory.existsByIdCustomer(model.getIdCustomer()))
					reposirtory.save(model);
				else
					msg = "Usted ya no puede tener mas cuentas fijas.";
			} else
				msg = "Las cuentas empresariales no deben tener cuentas a plazo fijo.";
		} else
			msg = "Cliente no registrado";

		return Mono.just(new message(msg));
	}

	public Mono<Object> saveTransfer(movements model) {
		fixedTermAccount obj = reposirtory.findByAccountNumber(model.getAccountRecep());
		double amount = obj.getAmount();

		obj.setAmount(amount + model.getAmount());
		obj.getMovements().add(model);

		reposirtory.save(obj);
		return Mono.just(new message(""));
	}

	public Mono<Object> saveMovements(movements model) {
		String msg = "Movimiento realizado";
		Date date = new Date();

		if (date.getDate() == 15) {
			if (reposirtory.existsByAccountNumber(model.getAccountEmisor())) {
				if (!operations.stream().filter(c -> c.equals(model.getType())).collect(Collectors.toList()).isEmpty())
					msg = addMovements(model);
				else
					msg = "Selecione una operacion correcta.";
			} else
				msg = "Numero de cuenta incorrecto.";
		} else
			msg = "No puede hacer movimientos fuera de la fecha establecida (15/**/****).";

		return Mono.just(new message(msg));
	}

	public Flux<Object> getAll() {
		return Flux.fromIterable(reposirtory.findAll());
	}

	public Mono<Object> getOne(String id) {
		return Mono.just(reposirtory.findByAccountNumber(id));
	}

	public Mono<Boolean> _verifyByNumberAccount(String number) {
		return Mono.just(reposirtory.existsByAccountNumber(number));
	}

	public Flux<Object> getByCustomer(String id) {
		return Flux.fromIterable(
				reposirtory.findAll().stream().filter(c -> c.getIdCustomer().equals(id)).collect(Collectors.toList()));
	}
}
