package io.homeey.matrix.rpc.proxy.bytebuddy;

import io.homeey.matrix.rpc.core.Invocation;
import io.homeey.matrix.rpc.core.Invoker;
import io.homeey.matrix.rpc.core.Result;
import io.homeey.matrix.rpc.core.URL;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

/**
 * ByteBuddy 代理功能测试
 * 
 * @author Matrix RPC Team
 */
public class ByteBuddyProxyFactoryTest {
    
    private ByteBuddyProxyFactory factory;
    
    @BeforeEach
    public void setup() {
        factory = new ByteBuddyProxyFactory();
    }
    
    /**
     * 测试基本代理功能
     */
    @Test
    public void testBasicProxy() {
        // 1. 创建 Mock Invoker
        Invoker<TestService> invoker = createMockInvoker("echo: ");
        
        // 2. 创建代理
        TestService proxy = factory.getProxy(invoker);
        
        // 3. 验证调用
        assertNotNull(proxy);
        String result = proxy.echo("hello");
        assertEquals("echo: hello", result);
    }
    
    /**
     * 测试多方法调用
     */
    @Test
    public void testMultipleMethods() {
        Invoker<TestService> invoker = createMockInvoker("result: ");
        TestService proxy = factory.getProxy(invoker);
        
        // 测试不同方法
        assertEquals("result: test1", proxy.echo("test1"));
        assertEquals("result: test2", proxy.echo("test2"));
        assertEquals(42, proxy.add(20, 22));
    }
    
    /**
     * 测试缓存效率
     */
    @Test
    public void testCacheEfficiency() {
        Invoker<TestService> invoker = createMockInvoker("test");
        
        // 第一次创建（触发字节码生成）
        long start1 = System.nanoTime();
        TestService proxy1 = factory.getProxy(invoker);
        long time1 = System.nanoTime() - start1;
        
        // 第二次创建（缓存命中）
        long start2 = System.nanoTime();
        TestService proxy2 = factory.getProxy(invoker);
        long time2 = System.nanoTime() - start2;
        
        // 验证代理可用
        assertNotNull(proxy1);
        assertNotNull(proxy2);
        
        // 缓存命中后应该快很多（至少10倍）
        assertTrue(time1 / time2 > 10, 
            "Cache should be faster, time1=" + time1 + ", time2=" + time2);
        
        System.out.println("First create: " + time1 / 1000 + " μs");
        System.out.println("Cache hit: " + time2 / 1000 + " μs");
        System.out.println("Speed up: " + (time1 / time2) + "x");
    }
    
    /**
     * 测试多接口代理
     */
    @Test
    public void testMultipleInterfaces() {
        Invoker<TestService> invoker = createMockInvoker("multi: ");
        
        // 创建实现多个接口的代理
        Object proxy = factory.getProxy(invoker, new Class<?>[]{
            TestService.class,
            java.io.Serializable.class
        });
        
        // 验证代理实现了所有接口
        assertTrue(proxy instanceof TestService);
        assertTrue(proxy instanceof java.io.Serializable);
        
        // 验证功能
        assertEquals("multi: test", ((TestService) proxy).echo("test"));
    }
    
    /**
     * 测试 Object 方法
     */
    @Test
    public void testObjectMethods() {
        Invoker<TestService> invoker = createMockInvoker("test");
        TestService proxy = factory.getProxy(invoker);
        
        // toString
        assertNotNull(proxy.toString());
        assertTrue(proxy.toString().contains("TestInvoker"));
        
        // hashCode
        assertTrue(proxy.hashCode() != 0);
        
        // equals
        assertTrue(proxy.equals(proxy));
        assertFalse(proxy.equals(new Object()));
    }
    
    /**
     * 测试异常处理
     */
    @Test
    public void testExceptionHandling() {
        Invoker<TestService> invoker = new Invoker<TestService>() {
            @Override
            public Class<TestService> getInterface() {
                return TestService.class;
            }
            
            @Override
            public Result invoke(Invocation invocation) {
                return new Result(new RuntimeException("Test exception"));
            }
        };
        
        TestService proxy = factory.getProxy(invoker);
        
        try {
            proxy.echo("test");
            fail("Should throw exception");
        } catch (RuntimeException e) {
            assertEquals("Test exception", e.getMessage());
        }
    }
    
    /**
     * 创建模拟的 Invoker
     */
    private Invoker<TestService> createMockInvoker(String prefix) {
        return new Invoker<TestService>() {
            @Override
            public Class<TestService> getInterface() {
                return TestService.class;
            }
            
            @Override
            public Result invoke(Invocation invocation) {
                Object result;
                String methodName = invocation.methodName();
                Object[] args = invocation.arguments();
                
                if ("echo".equals(methodName)) {
                    result = prefix + args[0];
                } else if ("add".equals(methodName)) {
                    result = (Integer) args[0] + (Integer) args[1];
                } else {
                    result = "unknown";
                }
                
                return new Result(result);
            }
            
            @Override
            public String toString() {
                return "TestInvoker[" + getInterface().getName() + "]";
            }
        };
    }
    
    /**
     * 测试服务接口
     */
    public interface TestService {
        String echo(String msg);
        int add(int a, int b);
    }
}
