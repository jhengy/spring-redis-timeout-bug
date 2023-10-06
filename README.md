# Getting Started
## Summary
- `spring.redis.timeout` seems to broken when
  1. `ReactiveStringRedisTemplate` is used 
  2. `ClientOptions` is set under `LettuceClientConfigurationBuilderCustomizer` 
- the bug is **NOT** resolved by the following
  - upgrading spring boot from 2.7.14 to the latest version (3.1.4 at the time of writing)
  - upgrading redis from 5.0 to the latest version (7.2.1 at the time of writing)
  - set redis command timeout programmatically via `LettuceClientConfigurationBuilderCustomizer`
- the bug can be resolved by
  1. using `StringRedisTemplate` instead of `ReactiveStringRedisTemplate` OR
  2. avoid setting `ClientOptions` under `LettuceClientConfigurationBuilderCustomizer`

## Steps

### 1. Set up
- run redis in docker
    - `docker-compose up -d`

### 2. Reproduce the bug with `ReactiveStringRedisTemplate` and `customize-client-options` set to true in `application.yml`
- run Spring Boot Application
- call /hget-reactive once to allow redis client to connect to redis server at least once (important step to trigger the bug)
    - `curl localhost:8080/hget-reactive`
- run DEBUG SLEEP in redis-cli
    ```shell
    docker exec -it redis-timeout-bug-test-redis-1 sh
    /data # redis-cl
    127.0.0.1:6379> DEBUG SLEEP 20
    ```
- call /hget-reactive again
  - `curl localhost:8080/hget-reactive`
- redis client should time out after the configured timeout, however in reality, the application waits until server responds in ~20s
```shell
curl localhost:8080/hget-reactive
redisCommandTimeout=3s, timeElapsed=18325ms, hash={}
```

### 3. No bug when `ReactiveStringRedisTemplate` is used without setting `ClientOptions` under `LettuceClientConfigurationBuilderCustomizer`
- set `customize-client-options` in `application.yml` to false
- run Spring Boot Application
- call /hget-reactive once to allow redis client to connect to redis server at least once (important step to trigger the bug)
  - `curl localhost:8080/hget-reactive`
- run DEBUG SLEEP in redis-cli
    ```shell
    docker exec -it redis-timeout-bug-test-redis-1 sh
    /data # redis-cl
    127.0.0.1:6379> DEBUG SLEEP 20
    ```
- call /hget-reactive again
  - `curl localhost:8080/hget-reactive`
- application successfully times out after the configured duration
```txt
2023-10-06T23:04:58.154+08:00 ERROR 67319 --- [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: io.lettuce.core.RedisCommandTimeoutException: Command timed out after 3 second(s)] with root cause

io.lettuce.core.RedisCommandTimeoutException: Command timed out after 3 second(s)
	at io.lettuce.core.internal.ExceptionFactory.createTimeoutException(ExceptionFactory.java:59) ~[lettuce-core-6.2.6.RELEASE.jar:6.2.6.RELEASE]
	at io.lettuce.core.protocol.CommandExpiryWriter.lambda$potentiallyExpire$0(CommandExpiryWriter.java:176) ~[lettuce-core-6.2.6.RELEASE.jar:6.2.6.RELEASE]
	at io.netty.util.concurrent.PromiseTask.runTask(PromiseTask.java:98) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.concurrent.ScheduledFutureTask.run(ScheduledFutureTask.java:153) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.concurrent.AbstractEventExecutor.runTask(AbstractEventExecutor.java:174) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.concurrent.DefaultEventExecutor.run(DefaultEventExecutor.java:66) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.concurrent.SingleThreadEventExecutor$4.run(SingleThreadEventExecutor.java:997) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.internal.ThreadExecutorMap$2.run(ThreadExecutorMap.java:74) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at io.netty.util.concurrent.FastThreadLocalRunnable.run(FastThreadLocalRunnable.java:30) ~[netty-common-4.1.97.Final.jar:4.1.97.Final]
	at java.base/java.lang.Thread.run(Thread.java:833) ~[na:na]
	Suppressed: java.lang.Exception: #block terminated with an error
		at reactor.core.publisher.BlockingSingleSubscriber.blockingGet(BlockingSingleSubscriber.java:103) ~[reactor-core-3.5.10.jar:3.5.10]
		at reactor.core.publisher.Mono.block(Mono.java:1712) ~[reactor-core-3.5.10.jar:3.5.10]
		at com.example.demo.redistimeoutbug.ReactiveRedisController.hget(ReactiveRedisController.java:35) ~[main/:na]
		at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
		at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
		at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
		at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
		at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-6.0.12.jar:6.0.12]
		at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:884) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:797) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1081) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:974) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1011) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:903) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:564) ~[tomcat-embed-core-10.1.13.jar:6.0]
		at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.0.12.jar:6.0.12]
		at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658) ~[tomcat-embed-core-10.1.13.jar:6.0]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:205) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.0.12.jar:6.0.12]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-6.0.12.jar:6.0.12]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-6.0.12.jar:6.0.12]
		at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
		at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:482) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:115) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:341) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:391) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:894) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1740) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
		... 1 common frames omitted
```

### 4. No bug when `StringRedisTemplate` is used and `ClientOptions` is set under `LettuceClientConfigurationBuilderCustomizer`
- set `customize-client-options` in `application.yml` to true
- run Spring Boot Application
- call /hget-non-reactive once to allow redis client to connect to redis server at least once (important step to trigger the bug)
  - `curl localhost:8080/hget-nonce-reactive`
- run DEBUG SLEEP in redis-cli
    ```shell
    docker exec -it redis-timeout-bug-test-redis-1 sh
    /data # redis-cl
    127.0.0.1:6379> DEBUG SLEEP 20
    ```
- call /hget-nonce-reactive again
  - `curl localhost:8080/hget-non-reactive`
- application successfully times out after the configured duration
```shell
2023-10-06T22:54:32.641+08:00 ERROR 56904 --- [nio-8080-exec-2] o.a.c.c.C.[.[.[/].[dispatcherServlet]    : Servlet.service() for servlet [dispatcherServlet] in context with path [] threw exception [Request processing failed: org.springframework.dao.QueryTimeoutException: Redis command timed out] with root cause

io.lettuce.core.RedisCommandTimeoutException: Command timed out after 3 second(s)
	at io.lettuce.core.internal.ExceptionFactory.createTimeoutException(ExceptionFactory.java:59) ~[lettuce-core-6.2.6.RELEASE.jar:6.2.6.RELEASE]
	at io.lettuce.core.internal.Futures.awaitOrCancel(Futures.java:246) ~[lettuce-core-6.2.6.RELEASE.jar:6.2.6.RELEASE]
	at io.lettuce.core.LettuceFutures.awaitOrCancel(LettuceFutures.java:74) ~[lettuce-core-6.2.6.RELEASE.jar:6.2.6.RELEASE]
	at org.springframework.data.redis.connection.lettuce.LettuceConnection.await(LettuceConnection.java:967) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.lettuce.LettuceConnection.lambda$doInvoke$4(LettuceConnection.java:826) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.lettuce.LettuceInvoker$Synchronizer.invoke(LettuceInvoker.java:665) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.lettuce.LettuceInvoker.just(LettuceInvoker.java:94) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.lettuce.LettuceHashCommands.hGetAll(LettuceHashCommands.java:103) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.DefaultedRedisConnection.hGetAll(DefaultedRedisConnection.java:1381) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.connection.DefaultStringRedisConnection.hGetAll(DefaultStringRedisConnection.java:466) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.core.DefaultHashOperations.lambda$entries$18(DefaultHashOperations.java:235) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.core.RedisTemplate.execute(RedisTemplate.java:406) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.core.RedisTemplate.execute(RedisTemplate.java:373) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.core.AbstractOperations.execute(AbstractOperations.java:97) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at org.springframework.data.redis.core.DefaultHashOperations.entries(DefaultHashOperations.java:235) ~[spring-data-redis-3.1.4.jar:3.1.4]
	at com.example.demo.redistimeoutbug.NonReactiveRedisController.hget(NonReactiveRedisController.java:32) ~[main/:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke0(Native Method) ~[na:na]
	at java.base/jdk.internal.reflect.NativeMethodAccessorImpl.invoke(NativeMethodAccessorImpl.java:77) ~[na:na]
	at java.base/jdk.internal.reflect.DelegatingMethodAccessorImpl.invoke(DelegatingMethodAccessorImpl.java:43) ~[na:na]
	at java.base/java.lang.reflect.Method.invoke(Method.java:568) ~[na:na]
	at org.springframework.web.method.support.InvocableHandlerMethod.doInvoke(InvocableHandlerMethod.java:205) ~[spring-web-6.0.12.jar:6.0.12]
	at org.springframework.web.method.support.InvocableHandlerMethod.invokeForRequest(InvocableHandlerMethod.java:150) ~[spring-web-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.mvc.method.annotation.ServletInvocableHandlerMethod.invokeAndHandle(ServletInvocableHandlerMethod.java:118) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.invokeHandlerMethod(RequestMappingHandlerAdapter.java:884) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerAdapter.handleInternal(RequestMappingHandlerAdapter.java:797) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.mvc.method.AbstractHandlerMethodAdapter.handle(AbstractHandlerMethodAdapter.java:87) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.DispatcherServlet.doDispatch(DispatcherServlet.java:1081) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.DispatcherServlet.doService(DispatcherServlet.java:974) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.FrameworkServlet.processRequest(FrameworkServlet.java:1011) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at org.springframework.web.servlet.FrameworkServlet.doGet(FrameworkServlet.java:903) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:564) ~[tomcat-embed-core-10.1.13.jar:6.0]
	at org.springframework.web.servlet.FrameworkServlet.service(FrameworkServlet.java:885) ~[spring-webmvc-6.0.12.jar:6.0.12]
	at jakarta.servlet.http.HttpServlet.service(HttpServlet.java:658) ~[tomcat-embed-core-10.1.13.jar:6.0]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:205) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.websocket.server.WsFilter.doFilter(WsFilter.java:51) ~[tomcat-embed-websocket-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.springframework.web.filter.RequestContextFilter.doFilterInternal(RequestContextFilter.java:100) ~[spring-web-6.0.12.jar:6.0.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.springframework.web.filter.FormContentFilter.doFilterInternal(FormContentFilter.java:93) ~[spring-web-6.0.12.jar:6.0.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.springframework.web.filter.CharacterEncodingFilter.doFilterInternal(CharacterEncodingFilter.java:201) ~[spring-web-6.0.12.jar:6.0.12]
	at org.springframework.web.filter.OncePerRequestFilter.doFilter(OncePerRequestFilter.java:116) ~[spring-web-6.0.12.jar:6.0.12]
	at org.apache.catalina.core.ApplicationFilterChain.internalDoFilter(ApplicationFilterChain.java:174) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.ApplicationFilterChain.doFilter(ApplicationFilterChain.java:149) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.StandardWrapperValve.invoke(StandardWrapperValve.java:167) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.StandardContextValve.invoke(StandardContextValve.java:90) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.authenticator.AuthenticatorBase.invoke(AuthenticatorBase.java:482) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.StandardHostValve.invoke(StandardHostValve.java:115) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.valves.ErrorReportValve.invoke(ErrorReportValve.java:93) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.core.StandardEngineValve.invoke(StandardEngineValve.java:74) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.catalina.connector.CoyoteAdapter.service(CoyoteAdapter.java:341) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.coyote.http11.Http11Processor.service(Http11Processor.java:391) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.coyote.AbstractProcessorLight.process(AbstractProcessorLight.java:63) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.coyote.AbstractProtocol$ConnectionHandler.process(AbstractProtocol.java:894) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.util.net.NioEndpoint$SocketProcessor.doRun(NioEndpoint.java:1740) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.util.net.SocketProcessorBase.run(SocketProcessorBase.java:52) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor.runWorker(ThreadPoolExecutor.java:1191) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.util.threads.ThreadPoolExecutor$Worker.run(ThreadPoolExecutor.java:659) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at org.apache.tomcat.util.threads.TaskThread$WrappingRunnable.run(TaskThread.java:61) ~[tomcat-embed-core-10.1.13.jar:10.1.13]
	at java.base/java.lang.Thread.run(Thread.java:833) ~[na:na]
```