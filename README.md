## Aegis Gate — 分布式限流与熔断中间件

Aegis Gate 是一个基于 Spring Boot 的分布式限流与熔断中间件，旨在帮助 Java 微服务项目轻松实现请求限流、熔断保护，支持 Redis + Lua 的分布式计数器，适用于高并发场景。

## 依赖导入
### Maven 项目

**将 Jar 包放入项目的 libs/ 目录（可自定义路径）**

在 pom.xml 中添加如下依赖配置：
```
<dependency>
    <groupId>com.lihuazou</groupId>
    <artifactId>aegis-gate</artifactId>
    <version>0.1.0</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/aegis-gate-0.1.0.jar</systemPath>
</dependency>

<dependency>
    <groupId>org.aspectj</groupId>
    <artifactId>aspectjweaver</artifactId>
    <version>1.9.22</version>
</dependency>
```

${project.basedir} 表示项目根目录。

system 范围表示使用本地 Jar，不会从远程仓库下载。

依赖引入后需要在 SpringBoot 项目的启动类中添加注解``` @ComponentScan ```扫描中间包

``` @ComponentScan({"org.example", "com.lihuazou.aegisgate"}) ```


Jar 包引入后，你就可以在项目中直接使用中间件提供的注解和功能，例如：
```
package org.example.controller;

import com.lihuazou.aegisgate.annotation.CircuitBreaker;
import org.example.service.UserService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import com.lihuazou.aegisgate.annotation.RateLimit;

import java.math.BigDecimal;
import java.util.Random;

@RestController
@RequestMapping("/test")
public class TestController {

    @GetMapping("/test01")
    @RateLimit(
            key = "test01",
            maxRequests = 2,
            window = 60
    )
    public void test01(@RequestParam String userId, @RequestParam BigDecimal price) {
        System.out.println("test01触发");
    }

    @GetMapping("/test02")
    @CircuitBreaker(
            key = "test02",
            window = 60,
            resetTimeout = 30,
            fallbackMethod = "test03"
    )
    public String test02(@RequestParam String monthlyId, @RequestParam BigDecimal price) {
        Random random = new Random();
        if (random.nextInt(10) + 1 > 5){
            int a = 5/0; // 模拟异常
        }
        return "success";
    }

    // 熔断回退方法
    public String test03(String monthlyId, BigDecimal price){
        System.out.println("熔断回退方法触发");
        return "fallback";
    }
}

```
注意: 
回退方法的参数参数数量和类型必须与原方法匹配,因为 AOP 通过反射调用回退方法时，会把原方法的参数直接传给它，如果类型或数量不一致，就会抛 NoSuchMethodException

可以少用参数吗?
一般不行，除非你的回退方法是无参方法，而你在 AOP 中做了额外适配逻辑，但中间件默认实现是直接反射调用，所以必须匹配

返回值要求: 
返回值类型必须兼容
最好完全一样
或者是父类/接口类型兼容也行，比如原方法返回 ArrayList，回退方法返回 List 类型也能兼容
如果返回值类型不兼容，AOP 调用会报 ClassCastException

方法访问修饰符必须 public，否则 AOP 拦截器无法反射调用

## 功能概述
1. 分布式限流：支持基于 Redis 的全局限流，保证多实例环境下的一致性。
2. 熔断保护：支持方法调用失败率统计，超过阈值自动触发熔断。
3. Spring Boot 注解式配置：只需在方法上添加注解即可实现限流和熔断，无需侵入业务逻辑。
4. 高性能：Lua 脚本保证限流操作原子性，并减少网络请求次数。

## 使用方式
1. 限流注解 @RateLimit
```
@RateLimit(
    key = "user_login",
    maxRequests = 5,
    window = 60,
    message = "请求过于频繁，请稍后再试"
)
public void login(String username, String password) { ... }
```

### 参数说明
| 参数 | 类型 | 必选 | 含义|
|----------|----------|----------|---------|
| key | String | 否 | 限流的唯一标识 |
| maxRequests | long | 是 | 时间窗口内最大请求数 |
| window | long | 是 | 时间窗口(秒) |
| message | String | 否 | 限流提示信息 默认:"请求过于频繁"|

示例：每 60 秒允许用户登录接口最多 5 次，超出后抛出异常。

2. 熔断注解 @CircuitBreaker
```
@CircuitBreaker(
    key = "remote_service_call",
    failureThreshold = 0.5,
    minimumRequest = 10,
    window = 60,
    resetTimeout = 30,
    fallbackMethod = "fallbackHandler"
)
public String callRemoteService() { ... }
```

参数说明
| 参数 | 类型 | 必选 | 含义|
|----------|----------|----------|----------|
|key|	String|	可选|	熔断唯一标识，如果不填，默认使用方法签名|
|failureThreshold|	double|	必填|	失败率阈值（0~1），超过该比例则触发熔断|
|minimumRequest|	int|	必填|	最少请求数，低于该请求数不进行熔断判断|
|window|	int|	必填|	统计失败率的时间窗口（秒）|
|resetTimeout|	int|	必填|	熔断触发后，经过该时间（秒）自动尝试半开状态|
|fallbackMethod|	String|	可选|	当熔断触发时调用的回退方法名称|

示例：当 60 秒内某服务请求失败率超过 50%，且请求数超过 10 个时，触发熔断，30 秒后尝试半开状态，如果仍失败则继续熔断。

## 核心设计

1. 分布式计数器：通过 Redis + Lua 原子操作实现请求计数，保证多实例环境下限流一致性。
2. 注解 + AOP 拦截：通过 Spring AOP 在方法执行前进行限流和熔断判断，业务代码无需修改。
3. 熔断逻辑：记录方法调用成功/失败次数，根据阈值触发熔断，并可指定回退方法。
4. Lua 脚本：保证限流计数的原子性，同时支持可配置的时间窗口。

示例项目
```
@RestController
public class UserController {

    @RateLimit(key = "login", maxRequests = 5, window = 60)
    @CircuitBreaker(failureThreshold = 0.5, minimumRequest = 10, window = 60, resetTimeout = 30)
    @GetMapping("/login")
    public String login(String username, String password) {
        // 模拟业务逻辑
        return "登录成功";
    }

    public String fallbackHandler() {
        return "服务暂不可用，请稍后再试";
    }
}
```
## 使用建议

限流适用于高并发接口、用户敏感操作等场景。

熔断适用于调用外部服务或不可靠模块，防止级联故障。

部署：确保 Redis 集群可用，否则限流功能不可用。

扩展：可自定义 Lua 脚本或熔断策略，满足业务复杂需求。
