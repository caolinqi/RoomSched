# 🚀 RoomSched 项目开发指南：如何安全地新增一个页面或模块

为了避免由于只写了 HTML 而忘记配置 Controller 导致 500/404 错误，或者破坏了全局 UI 骨架，针对我们当前的 `RoomSched` 架构，特此梳理了**标准的“四步走”新增页面 SOP（标准作业程序）**。

在后续增加新功能时，务必严格按照以下顺序执行：

---

## 第一步：编写后端 Controller 路由（最关键）
在编写前端模板前，**必须**先在 Controller 中占位并注入页面所需的变量。Thymeleaf 在渲染时，如果强行读取了模型中不存在的变量，会导致底层抛出 `SpelEvaluationException` 从而产生 500 错误。

- **位置**：`src/main/java/org/example/roomsched/controller/`
- **规范**：
  - 前台页面建议写在 `FrontController.java`
  - 后台管理页面建议写在 `AdminController.java`

```java
@GetMapping("/your-new-page")
public String yourNewPage(Model model, @AuthenticationPrincipal CustomUserDetails userDetails) {
    // 1. 业务逻辑与数据查询
    String myData = "Hello World";
    
    // 2. 注入前端需要的变量（即使是 null 或默认值也一定要注入，防止前端找不到变量而报错）
    model.addAttribute("myData", myData);
    
    // 3. 返回视图的逻辑路径（不带 .html 后缀）
    return "front/your-new-page"; 
}
```

---

## 第二步：创建前端 Thymeleaf 模板视图
由于系统配置了非常优雅的 `LayoutInterceptor`（全局拦截器），它会自动将你的页面内容“塞进”导航栏和全局骨架中。因此，您**千万不要**在新页面中写 `<html>`、`<head>`、`<body>` 等基础标签。

- **位置**：`src/main/resources/templates/front/`（或 `admin/`）
- **规范**：直接从具体的布局容器（如 `<div>`）开始写起。

```html
<!-- 直接写内容即可，全局的 CSS/JS 以及导航栏都会由 layout.html 自动包裹 -->
<style>
    /* 在这里写该页面专属的 CSS */
    .my-custom-card { ... }
    
    /* 别忘了适配暗黑模式 */
    html[data-theme="dark"] .my-custom-card { ... }
</style>

<div class="my-custom-card">
    <h2 th:text="${myData}">默认文本</h2>
</div>

<script>
    /* 在这里写该页面专属的 JS */
</script>
```

---

## 第三步：检查 Spring Security 权限（按需）
如果您的新页面需要特定的权限控制（例如必须登录，或者只有管理员才能看），请确保安全配置拦截到位。

- **前端入口隐藏**：在导航栏中通过 `sec:authorize` 标签控制入口可见性。
  ```html
  <!-- 只有登录用户可见 -->
  <a sec:authorize="isAuthenticated()" th:href="@{/your-new-page}">新页面</a>
  
  <!-- 只有管理员可见 -->
  <a sec:authorize="hasRole('ADMIN')" th:href="@{/your-new-page}">新页面</a>
  ```
- **后端接口放行**：如果这是一个**公开页面**（无需登录），必须去 `SecurityConfig.java` 中将其加入白名单：
  ```java
  request.requestMatchers("/your-new-page").permitAll()
  ```

---

## 第四步：将入口注册到全局导航栏
最后一步，让用户能够点击进入你的新页面。

- **位置**：`src/main/resources/templates/common/layout.html` (前台) 或 `admin-layout.html` (后台)
- **操作**：找到 `<div class="rs-nav-menu">` 区域，追加你的 `<a>` 标签。

```html
<div class="rs-nav-menu">
    <a th:href="@{/}" id="nav-home">首页</a>
    ...
    <!-- 加入你的新页面链接 -->
    <a th:href="@{/your-new-page}" id="nav-your-new-page">我的新功能</a>
</div>
```

---

### 💡 避坑速查小结
1. **先写 Controller，再写 HTML**。
2. **HTML 里不要写 `<body>` 和 `<head>`**。
3. **Thymeleaf 变量如果有可能为空，后端最好提前做默认值处理，或者前端用安全写法**（如 `${myVar != null ? myVar : '默认值'}`）。
4. **修改完 Java 代码后，务必重启 Spring Boot 服务器**。修改 HTML 则只需刷新浏览器即可。
