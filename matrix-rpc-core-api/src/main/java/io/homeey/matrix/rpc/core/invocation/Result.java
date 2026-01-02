package io.homeey.matrix.rpc.core.invocation;

public interface Result {

    /**
     * 获取调用结果的值
     *
     * @return 调用返回的值，如果没有异常则返回实际结果，否则可能返回null
     */
    Object value();

    /**
     * 获取调用过程中发生的异常
     *
     * @return 如果调用过程中发生异常则返回该异常，否则返回null
     */
    Throwable exception();

    /**
     * 检查调用结果是否包含异常
     *
     * @return 如果存在异常返回true，否则返回false
     */
    default boolean hasException() {
        return exception() != null;
    }
}
