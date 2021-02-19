package com.javislaptop.binance.api;

import com.binance.api.client.domain.account.Account;
import com.binance.api.client.domain.account.Order;
import com.javislaptop.binance.orderbook.MonitorOrderBookTask;
import org.apache.commons.lang3.ArrayUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.util.CollectionUtils;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class AccountService {

    private static final Logger logger = LoggerFactory.getLogger(AccountService.class);

    private final Binance binance;
    private Account account;
    private List<Order> openOrders;

    public AccountService(Binance binance) {
        this.binance = binance;
    }

    @Scheduled(initialDelay = 0, fixedRate = 5000)
    public void refreshAccount() {
        logger.debug("Synchronizing account");
        this.account = binance.getAccount();
    }

    @Scheduled(initialDelay = 0, fixedRate = 3000)
    public void refreshOrders() {
        logger.debug("Refreshing orders");
        List<Order> openOrders = binance.getOpenOrders();
        while (CollectionUtils.isEmpty(openOrders)) {
            logger.warn("Received empty orders, refreshing");
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                logger.error("Error while sleeping", e);
            }
            openOrders = binance.getOpenOrders();
        }
        this.openOrders = openOrders;
    }

    public Account getAccount() {
        return account;
    }

    public List<Order> getOpenOrders(String symbol) {
        return openOrders.stream().filter(o -> o.getSymbol().equals(symbol)).collect(Collectors.toList());
    }
}
