package com.vagdedes.spartan.abstraction.profiling;

import com.vagdedes.spartan.functionality.server.SpartanBukkit;
import com.vagdedes.spartan.utils.math.AlgebraUtils;
import com.vagdedes.spartan.utils.minecraft.world.GroundUtils;

public class PlayerViolation {

    public final long time;
    public final int hash;
    public final double increase;

    public PlayerViolation(long time,
                           double increase,
                           String information) {
        this.time = time;
        this.increase = increase;

        if (information.length() > 0) {
            int hash = 0;
            StringBuilder number = new StringBuilder();
            boolean decimal = false, exponent = false, negative = false;
            char barChar = '-';

            for (char c : information.toCharArray()) {
                if (c == barChar) { // Exponent or Negative
                    if (exponent) { // Continuation of Exponent
                        number.append(c);
                    } else if (number.length() > 0) { // Invalid Number
                        if (negative) { // Repetition of Negative
                            hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                        } else {
                            negative = true;
                        }
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + number.toString().hashCode();
                        number = new StringBuilder();
                        decimal = false;
                    } else if (negative) { // Repetition of Negative
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                    } else {
                        negative = true;
                    }
                } else if (c == '0'
                        || c == '1'
                        || c == '2'
                        || c == '3'
                        || c == '4'
                        || c == '5'
                        || c == '6'
                        || c == '7'
                        || c == '8'
                        || c == '9') { // Number
                    if (negative) { // Negative Number
                        number.append(barChar);
                        negative = false;
                    }
                    number.append(c);
                } else if (c == '.') { // Potential Decimal
                    if (number.length() > 0) { // Prepared Number
                        if (decimal) { // Invalid Decimal
                            if (negative) { // Invalid Exponent or Negative
                                hash = (hash * SpartanBukkit.hashCodeMultiplier) + barChar;
                                negative = false;
                            }
                            hash = (hash * SpartanBukkit.hashCodeMultiplier) + number.toString().hashCode();
                            number = new StringBuilder();
                            decimal = false;
                            exponent = false;
                        } else { // Valid Decimal
                            number.append(c);
                            decimal = true;
                        }
                    } else { // Not Decimal
                        if (negative) { // Invalid Exponent or Negative
                            hash = (hash * SpartanBukkit.hashCodeMultiplier) + barChar;
                            negative = false;
                        }
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                    }
                } else if (c == 'E') { // Potential Exponent
                    if (number.length() > 0) { // Prepared Number
                        if (exponent) { // Invalid Exponent
                            if (negative) { // Invalid Exponent or Negative
                                hash = (hash * SpartanBukkit.hashCodeMultiplier) + barChar;
                                negative = false;
                            }
                            hash = (hash * SpartanBukkit.hashCodeMultiplier) + number.toString().hashCode();
                            number = new StringBuilder();
                            decimal = false;
                            exponent = false;
                        } else { // Valid Exponent
                            number.append(c);
                            exponent = true;
                        }
                    } else { // Not Exponent
                        if (negative) { // Invalid Exponent or Negative
                            hash = (hash * SpartanBukkit.hashCodeMultiplier) + barChar;
                            negative = false;
                        }
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                    }
                } else if (number.length() > 0) { // End of Number
                    if (decimal) { // End of Decimal Number
                        double d = AlgebraUtils.cut(
                                Double.parseDouble(number.toString()),
                                GroundUtils.maxHeightLength
                        );
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + Double.hashCode(d);
                    } else {  // End of Integer Number
                        int i = Integer.parseInt(number.toString());
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + i;
                    }
                    number = new StringBuilder();
                    decimal = false;
                    exponent = false;
                    negative = false;
                } else { // String
                    if (negative) { // Invalid Exponent or Negative
                        hash = (hash * SpartanBukkit.hashCodeMultiplier) + barChar;
                        negative = false;
                    }
                    hash = (hash * SpartanBukkit.hashCodeMultiplier) + c;
                }
            }
            this.hash = hash;
        } else {
            this.hash = 0;
        }
    }

}
