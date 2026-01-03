package io.homeey.matrix.rpc.core.filter;

import io.homeey.matrix.rpc.core.Filter;

import java.util.List;

public final class FilterLoader {

    private FilterLoader() {
    }

    public static List<Filter> loadFilters(FilterScope scope) {
//        List<Filter> actives = ExtensionLoader.getExtensionLoader(Filter.class)
//                .getActivateExtensions(scope.name());
//        actives.sort(new FilterComparator());
//        return actives;
        return null;
    }
}
