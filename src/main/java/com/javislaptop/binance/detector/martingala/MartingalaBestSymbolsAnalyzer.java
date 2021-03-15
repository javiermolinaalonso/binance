package com.javislaptop.binance.detector.martingala;

import com.binance.api.client.domain.general.SymbolInfo;
import com.javislaptop.binance.api.Binance;
import com.javislaptop.binance.api.domain.Candlestick;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;

import javax.annotation.PostConstruct;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class MartingalaBestSymbolsAnalyzer {

    private static final Logger logger = LogManager.getLogger(MartingalaBestSymbolsAnalyzer.class);

    private final Binance binance;
    private final MartingalaLoader loader;
    private final MartingalaProperties props;

    public MartingalaBestSymbolsAnalyzer(Binance binance, MartingalaLoader loader, MartingalaProperties properties) {
        this.binance = binance;
        this.loader = loader;
        this.props = properties;
    }

//    @PostConstruct
    public void execute() {
        List<String> symbols = binance.getExchangeInfo().getSymbols().stream()
                .map(SymbolInfo::getSymbol)
                .filter(s -> s.contains(props.getBaseCurrency()))
                .collect(Collectors.toList());

        Map<String, List<Candlestick>> data = loader.load(props.getFrom(), props.getTo(), symbols, props.getInterval());

        Map<String, Double> values = new TreeMap<>();
        data.entrySet().stream()
                .filter(e -> e.getValue().size() > 180)
                .filter(e -> e.getValue().get(e.getValue().size() - 1).getClose().compareTo(new BigDecimal("0.0001")) > 0)
                .forEach(e -> values.put(e.getKey(), e.getValue().stream().mapToDouble(c -> c.getLowerShadow().setScale(4, RoundingMode.HALF_DOWN).doubleValue()).average().orElse(0d)));


        LinkedHashMap<String, Double> a = values.entrySet().stream().
                sorted(Map.Entry.comparingByValue()).
                collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue,
                        (e1, e2) -> e1, LinkedHashMap::new));

        a.forEach((k,v) -> logger.info("{}: {}%", k, v * 100d));
    }

}
