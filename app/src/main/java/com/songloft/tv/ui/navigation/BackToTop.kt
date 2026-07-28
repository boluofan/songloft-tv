package com.songloft.tv.ui.navigation

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.focus.FocusRequester
import kotlinx.coroutines.launch

@Stable
class TabBarBridge {
    val tabFocusRequester = FocusRequester()
    var hasFocus by mutableStateOf(false)

    fun focusTabBar() {
        runCatching { tabFocusRequester.requestFocus() }
    }
}

val LocalTabBarBridge = compositionLocalOf<TabBarBridge?> { null }

/**
 * 三段式返回键：列表非顶部时回顶并聚焦 [topFocus]；已在顶部时聚焦底部 Tab 栏；
 * 焦点已在 Tab 栏时本 handler 禁用，返回键穿透到外层既有 BackHandler。
 */
@Composable
fun ListBackToTopHandler(
    listState: LazyListState,
    topFocus: FocusRequester? = null,
    topFocusInList: Boolean = false,
    enabled: Boolean = true
) {
    val bridge = LocalTabBarBridge.current
    val scope = rememberCoroutineScope()
    BackHandler(
        enabled = enabled && bridge?.hasFocus != true &&
            (listState.canScrollBackward || bridge != null)
    ) {
        if (listState.canScrollBackward) {
            scope.launch {
                if (topFocusInList) {
                    listState.scrollToItem(0)
                    withFrameNanos { }
                    if (topFocus == null || runCatching { topFocus.requestFocus() }.isFailure) {
                        bridge?.focusTabBar()
                    }
                } else {
                    // 锚点在列表外恒组合：先夺焦再滚动，避免焦点项被回收后焦点乱跳
                    topFocus?.let { runCatching { it.requestFocus() } }
                    listState.scrollToItem(0)
                }
            }
        } else {
            bridge?.focusTabBar()
        }
    }
}
