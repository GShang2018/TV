# 主题色（Theme Color）新增指南

## 概述

应用目前支持 **7 种预设主题色**，用户可在「设置 → 自定义 → 主题色」中选择。所有颜色基于深色 Material3 主题，切换主题色后应用会自动重启以使颜色生效。

---

## 新增主题色步骤（共 6 步）

假设要新增一个 **「黄色」** 主题色，关键值为 `yellow`。

### 第 1 步：在 `colors.xml` 添加颜色值

**文件**：[`app/src/mobile/res/values/colors.xml`](app/src/mobile/res/values/colors.xml)

```xml
<!-- 在 <!-- ====== 主题色预设 ====== --> 区域末尾添加 -->
<color name="theme_yellow">#FFEB3B</color>

<!-- 在 <!-- 各主题色文本高亮色（25% alpha） --> 区域末尾添加 -->
<color name="text_highlight_yellow">#40FFEB3B</color>
```

> **规则**：颜色名必须使用 `theme_xxx` 格式，高亮色使用 `text_highlight_xxx` 格式，alpha 通道为 `40`（25%）。

---

### 第 2 步：在 `styles.xml` 添加主题样式

**文件**：[`app/src/mobile/res/values/styles.xml`](app/src/mobile/res/values/styles.xml)

在最后一个 `AppTheme.Pink` 样式之后添加：

```xml
<style name="AppTheme.Yellow" parent="AppTheme">
    <item name="colorPrimary">@color/theme_yellow</item>
    <item name="colorPrimaryContainer">@color/theme_yellow</item>
    <item name="colorPrimaryDark">@color/theme_yellow</item>
    <item name="colorAccent">@color/theme_yellow</item>
    <item name="android:textColorHighlight">@color/text_highlight_yellow</item>
</style>
```

> **规则**：样式名必须使用 `AppTheme.xxx` 格式，parent 固定为 `AppTheme`。<br>
> 必须覆盖以下 5 个属性：`colorPrimary`、`colorPrimaryContainer`、`colorPrimaryDark`、`colorAccent`、`android:textColorHighlight`。

---

### 第 3 步：在 `strings.xml` 添加显示名

**文件**：[`app/src/mobile/res/values/strings.xml`](app/src/mobile/res/values/strings.xml)

在 `select_theme_color` 数组中添加一项：

```xml
<string-array name="select_theme_color">
    <item>绿色</item>
    <item>蓝色</item>
    <item>红色</item>
    <item>紫色</item>
    <item>橙色</item>
    <item>青色</item>
    <item>粉色</item>
    <item>黄色</item>   <!-- ← 新增 -->
</string-array>
```

如果已有中文翻译文件 `values-zh-rCN/strings.xml`，同样添加。没有则需要按需添加翻译。

---

### 第 4 步：在 `SettingCustomFragment.java` 注册新颜色

**文件**：[`app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingCustomFragment.java`](app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingCustomFragment.java)

需要修改 **3 处**：

#### 4a. `THEME_COLOR_VALUES` 数组 — 添加键名

```java
private final String[] THEME_COLOR_VALUES = {"green", "blue", "red", "purple", "orange", "teal", "pink", "yellow"};
```

#### 4b. `THEME_COLOR_HEX` 数组 — 添加对应色值

```java
private final String[] THEME_COLOR_HEX = {"#1DB954", "#2196F3", "#F44336", "#9C27B0", "#FF9800", "#009688", "#E91E63", "#FFEB3B"};
```

> 这两个数组的**顺序必须与 `select_theme_color` 数组完全一致**，确保颜色名、色值和显示名一一对应。

#### 4c. `getThemeColorIndex()` 方法 — 添加检索

```java
private int getThemeColorIndex() {
    String[] all = {"green", "blue", "red", "purple", "orange", "teal", "pink", "yellow"};
    // ...
}
```

---

### 第 5 步：在 `BaseActivity.java` 添加样式映射

**文件**：[`app/src/mobile/java/com/fongmi/android/tv/ui/base/BaseActivity.java`](app/src/mobile/java/com/fongmi/android/tv/ui/base/BaseActivity.java)

在 `getThemeStyleResId()` switch 中添加 case：

```java
private int getThemeStyleResId(String color) {
    switch (color) {
        case "blue":    return R.style.AppTheme_Blue;
        case "red":     return R.style.AppTheme_Red;
        case "purple":  return R.style.AppTheme_Purple;
        case "orange":  return R.style.AppTheme_Orange;
        case "teal":    return R.style.AppTheme_Teal;
        case "pink":    return R.style.AppTheme_Pink;
        case "yellow":  return R.style.AppTheme_Yellow;  // ← 新增
        default:        return R.style.AppTheme_Green;
    }
}
```

---

### 第 6 步：构建验证

```bash
./gradlew assembleMobileJavaArmeabi_v7aDebug
```

---

## 快速对照表

| 预设色 | 键名 | 样式名 | 颜色值 |
|--------|------|--------|--------|
| 绿色（默认） | `green` | `AppTheme.Green` | `#1DB954` |
| 蓝色 | `blue` | `AppTheme.Blue` | `#2196F3` |
| 红色 | `red` | `AppTheme.Red` | `#F44336` |
| 紫色 | `purple` | `AppTheme.Purple` | `#9C27B0` |
| 橙色 | `orange` | `AppTheme.Orange` | `#FF9800` |
| 青色 | `teal` | `AppTheme.Teal` | `#009688` |
| 粉色 | `pink` | `AppTheme.Pink` | `#E91E63` |

## 关键文件清单

| 文件 | 用途 |
|------|------|
| [`app/src/mobile/res/values/colors.xml`](app/src/mobile/res/values/colors.xml) | 颜色定义 |
| [`app/src/mobile/res/values/styles.xml`](app/src/mobile/res/values/styles.xml) | 主题样式定义 |
| [`app/src/mobile/res/values/strings.xml`](app/src/mobile/res/values/strings.xml) | 颜色显示名 |
| [`app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingCustomFragment.java`](app/src/mobile/java/com/fongmi/android/tv/ui/fragment/SettingCustomFragment.java) | 选择弹窗逻辑 |
| [`app/src/mobile/java/com/fongmi/android/tv/ui/base/BaseActivity.java`](app/src/mobile/java/com/fongmi/android/tv/ui/base/BaseActivity.java) | 主题映射 |
