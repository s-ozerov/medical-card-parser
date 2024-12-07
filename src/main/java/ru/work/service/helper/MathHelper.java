package ru.work.service.helper;

import java.math.BigDecimal;
import java.math.MathContext;
import java.math.RoundingMode;

public class MathHelper {

    public static final RoundingMode ROUND_MODE = RoundingMode.HALF_UP;
    public static final MathContext MATCH_CONTEXT_18 = new MathContext(18, ROUND_MODE);

    public static BigDecimal round2(BigDecimal bigDecimal) {
        return bigDecimal.setScale(2, ROUND_MODE);
    }

}
