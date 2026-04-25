package com.algo.risk.repository;

import com.algo.risk.repository.entity.TradeHistory;
import org.springframework.data.repository.reactive.ReactiveCrudRepository;

public interface TradeHistoryRepository extends ReactiveCrudRepository<TradeHistory, Long> {
}
