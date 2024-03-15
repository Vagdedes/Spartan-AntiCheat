package com.vagdedes.spartan.abstraction.pattern.implementation;

import com.vagdedes.spartan.abstraction.pattern.PatternFamily;
import com.vagdedes.spartan.abstraction.pattern.implementation.base.PatternStorage;
import com.vagdedes.spartan.utils.gameplay.GroundUtils;

public class MovementPatterns extends PatternFamily {

    public PatternStorage horizontal, vertical, all, fall;

    public MovementPatterns(long storeFrequency) {
        super(storeFrequency, 4);
        this.addPatterns(
                new PatternStorage[]{
                        this.horizontal = new PatternStorage(
                                "movement/distance/horizontal",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.vertical = new PatternStorage(
                                "movement/distance/vertical",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.all = new PatternStorage(
                                "movement/distance/all",
                                new int[]{GroundUtils.maxHeightLength}
                        ),
                        this.fall = new PatternStorage(
                                "movement/distance/fall",
                                new int[]{}
                        )
                }
        );
    }
}
