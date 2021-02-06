package com.javislaptop.binance.pumper;

import com.binance.api.client.BinanceApiRestClient;
import com.binance.api.client.domain.market.Candlestick;
import com.binance.api.client.domain.market.CandlestickInterval;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.OptionalDouble;

@Service
public class UnusualVolumeDetector {

    private final BinanceApiRestClient binance;

    public UnusualVolumeDetector(BinanceApiRestClient binance) {
        this.binance = binance;
    }

    //Volumen estandar -> x0.5 - x2 volumen medio
    //Gran volumen -> x2 - x5 volumen medio
    //Volumen exagerado -> >x5 volumen medio
    public void detect() {
        //Predictors
        // Gran volumen y gran movimiento. Venimos de una subida pronunciada? Si venimos de mucha subida vale la pena ir contrarian?
        //     Si detectamos este patron por segunda o tercera vez? Cuando salimos del trade?
        // Gran volumen sin apenas movimiento -> Marcando techo o suelo? Este gran volumen es unico o hay mas antes?
        // Volumen exagerado?? (>5 veces la media)
        // Calculo de derivadas! puntos cercanos al 0
        // Influyen el tamaño de los picos de la vela?
        List<Candlestick> candlesticks = binance.getCandlestickBars("BNBBTC", CandlestickInterval.ONE_MINUTE);
        OptionalDouble average = candlesticks.stream().mapToDouble(c -> new BigDecimal(c.getVolume()).doubleValue()).average();
        candlesticks.stream().sorted(Comparator.comparing(c -> new BigDecimal(c.getVolume()))).forEach(System.out::println);
    }
}
