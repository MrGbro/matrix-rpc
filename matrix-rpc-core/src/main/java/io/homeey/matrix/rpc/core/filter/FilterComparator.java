package io.homeey.matrix.rpc.core.filter;

import io.homeey.matrix.rpc.spi.Activate;

import java.util.Comparator;

public class FilterComparator implements Comparator<Filter> {

    @Override
    public int compare(Filter f1, Filter f2) {
        Activate a1 = f1.getClass().getAnnotation(Activate.class);
        Activate a2 = f2.getClass().getAnnotation(Activate.class);

        int o1 = a1 != null ? a1.order() : 0;
        int o2 = a2 != null ? a2.order() : 0;

        return Integer.compare(o1, o2);
    }
}
