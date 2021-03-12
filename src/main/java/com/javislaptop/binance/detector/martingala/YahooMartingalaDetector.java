package com.javislaptop.binance.detector.martingala;

import com.javislaptop.binance.api.domain.Candlestick;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.stereotype.Service;
import yahoofinance.Stock;
import yahoofinance.YahooFinance;
import yahoofinance.histquotes.Interval;

import javax.annotation.PostConstruct;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@EnableConfigurationProperties(MartingalaProperties.class)
public class YahooMartingalaDetector extends MartingalaDetector {

    YahooMartingalaDetector(MartingalaProperties martingalaProperties, WalletState wallet) {
        super(martingalaProperties, wallet);
    }

//    @PostConstruct
    public void init() {
        execute();
    }

    @Override
    Map<String, List<Candlestick>> populate() {
//        String[] symbols = new String[] {"AAPL"};
        String[] symbols = new String[] {"AAPL", "GOOG", "MSFT", "FB", "NVDA"};
        Calendar from = Calendar.getInstance();
        from.set(Calendar.SECOND, 0);
        from.set(Calendar.MINUTE, 0);
        from.set(Calendar.HOUR, 0);
        from.set(Calendar.MONTH, Calendar.AUGUST);
        from.set(Calendar.DAY_OF_MONTH, 1);
        from.set(Calendar.YEAR, 2020);
        Calendar to = Calendar.getInstance();
        to.set(Calendar.SECOND, 0);
        to.set(Calendar.MINUTE, 0);
        to.set(Calendar.HOUR, 0);
        to.set(Calendar.MONTH, Calendar.NOVEMBER);
        to.set(Calendar.DAY_OF_MONTH, 30);
        to.set(Calendar.YEAR, 2020);


        try {
            Map<String, Stock> data = YahooFinance.get(symbols);
            Map<String, List<Candlestick>> result = new HashMap<>();
            data.forEach((key, value) -> {
                try {
                    List<Candlestick> values = value.getHistory(from, to, Interval.DAILY).stream().map(t -> new Candlestick(t.getDate().toInstant(), t.getOpen(), t.getHigh(), t.getLow(), t.getClose(), BigDecimal.valueOf(t.getVolume()), t.getDate().toInstant(), null, null, null, null)).collect(Collectors.toList());
                    result.put(key, values);
                } catch (IOException aaa) {
                    aaa.printStackTrace();
                }
            });
            return result;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    @Override
    BigDecimal updateBuyThreshold(BigDecimal referencePrice, String symbol, int attempts) {
        return referencePrice.subtract(referencePrice.multiply(props.getDecrease())).setScale(2, RoundingMode.DOWN);
    }
}
