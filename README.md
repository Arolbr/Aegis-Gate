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
    <version>1.0-SNAPSHOT</version>
    <scope>system</scope>
    <systemPath>${project.basedir}/libs/aegis-gate-1.0-SNAPSHOT.jar</systemPath>
</dependency>
```

${project.basedir} 表示项目根目录。

system 范围表示使用本地 Jar，不会从远程仓库下载。

### Gradle 项目

将 Jar 包放入 libs/ 目录(同样的也可以自定义路径)

在 build.gradle 中添加依赖：
```
dependencies {
    implementation files('libs/aegis-gate-1.0-SNAPSHOT.jar')
}
```

Jar 包引入后，你就可以在项目中直接使用中间件提供的注解和功能，例如：
```
import com.lihuazou.aegisgate.annotation.RateLimit;

@RestController
public class DemoController {

    @RateLimit(maxRequests = 5, window = 60, message = "请求过于频繁")
    @GetMapping("/test")
    public String test() {
        return "成功访问";
    }
}
```

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
