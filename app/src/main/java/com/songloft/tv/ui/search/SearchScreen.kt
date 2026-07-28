package com.songloft.tv.ui.search

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.QrCode
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.songloft.tv.data.model.Song
import com.songloft.tv.ui.components.generateQrBitmap
import com.songloft.tv.ui.navigation.ListBackToTopHandler

@Composable
fun SearchScreen(
    viewModel: SearchViewModel = hiltViewModel(),
    onSongClick: (List<Song>, Int) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val remoteUrl by viewModel.remoteUrl.collectAsStateWithLifecycle()
    var showKeyboard by remember { mutableStateOf(false) }
    var showQrDialog by remember { mutableStateOf(false) }
    val searchBoxFocus = remember { FocusRequester() }
    var searchFocused by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) {
        runCatching { searchBoxFocus.requestFocus() }
    }

    LaunchedEffect(Unit) {
        viewModel.remoteSubmitEvents.collect {
            showQrDialog = false
            showKeyboard = false
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopRemoteInput() }
    }

    BackHandler(enabled = showKeyboard) {
        showKeyboard = false
        runCatching { searchBoxFocus.requestFocus() }
    }

    ListBackToTopHandler(
        listState,
        topFocus = searchBoxFocus,
        topFocusHasFocus = searchFocused,
        enabled = !showKeyboard
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 48.dp, vertical = 24.dp)
    ) {
        Text(
            text = "搜索音乐",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )

        Spacer(Modifier.height(16.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(
                        if (searchFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .then(
                        if (searchFocused) Modifier.border(
                            2.dp, MaterialTheme.colorScheme.primary, RoundedCornerShape(12.dp)
                        ) else Modifier
                    )
                    .focusRequester(searchBoxFocus)
                    .onFocusChanged { searchFocused = it.isFocused }
                    .clickable { showKeyboard = !showKeyboard }
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = if (uiState.query.isEmpty()) "点击搜索歌曲..." else uiState.query,
                    fontSize = 20.sp,
                    color = if (uiState.query.isEmpty()) MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                            else MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
            }
            if (uiState.query.isNotEmpty()) {
                var clearFocused by remember { mutableStateOf(false) }
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = "清空",
                    tint = if (clearFocused) MaterialTheme.colorScheme.onPrimary
                            else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    modifier = Modifier
                        .padding(start = 12.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(
                            if (clearFocused) MaterialTheme.colorScheme.primary
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                        )
                        .onFocusChanged { clearFocused = it.isFocused }
                        .clickable { viewModel.clearSearch() }
                        .padding(horizontal = 10.dp, vertical = 14.dp)
                )
            }
            var qrFocused by remember { mutableStateOf(false) }
            Icon(
                imageVector = Icons.Rounded.QrCode,
                contentDescription = "扫码搜索",
                tint = if (qrFocused) MaterialTheme.colorScheme.onPrimary
                        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier
                    .padding(start = 12.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        if (qrFocused) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                    )
                    .onFocusChanged { qrFocused = it.isFocused }
                    .clickable {
                        viewModel.startRemoteInput()
                        showQrDialog = true
                    }
                    .padding(horizontal = 10.dp, vertical = 14.dp)
            )
        }

        Spacer(Modifier.height(12.dp))

        if (showKeyboard) {
            TvKeyboard(
                onKeyPress = { key ->
                    when (key) {
                        "←退格" -> {
                            val current = uiState.query
                            if (current.isNotEmpty()) {
                                viewModel.onQueryChanged(current.substring(0, current.length - 1))
                            }
                        }
                        "清空" -> viewModel.clearSearch()
                        "确定" -> { showKeyboard = false; searchBoxFocus.requestFocus() }
                        "空格" -> viewModel.onQueryChanged("${uiState.query} ")
                        else -> viewModel.onQueryChanged("${uiState.query}$key")
                    }
                }
            )
            Spacer(Modifier.height(12.dp))
        }

        if (uiState.isSearching) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("搜索中...", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f))
            }
        } else if (uiState.hasSearched && uiState.results.isEmpty()) {
            Box(
                modifier = Modifier.fillMaxWidth().padding(24.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("未找到结果", fontSize = 16.sp, color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f))
            }
        } else if (uiState.results.isNotEmpty()) {
            Text(
                text = "共 ${uiState.results.size} 首",
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                modifier = Modifier.padding(bottom = 8.dp)
            )

            LazyColumn(
                state = listState,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                itemsIndexed(uiState.results) { index, song ->
                    SongResultItem(
                        song = song,
                        onClick = { onSongClick(uiState.results, index) }
                    )
                }
            }
        } else if (!uiState.hasSearched) {
            if (uiState.hotTags.isNotEmpty()) {
                Text(
                    text = "热门搜索",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                    modifier = Modifier.padding(top = 8.dp, bottom = 12.dp)
                )
                LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    items(uiState.hotTags) { tag ->
                        HotTagChip(tag) { viewModel.onQueryChanged(tag) }
                    }
                }
            } else {
                Box(
                    modifier = Modifier.fillMaxWidth().padding(48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "输入关键词搜索歌曲",
                        fontSize = 16.sp,
                        color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                    )
                }
            }
        }
    }

    if (showQrDialog) {
        SearchQrDialog(url = remoteUrl, onDismiss = { showQrDialog = false })
    }
}

@Composable
private fun SearchQrDialog(url: String?, onDismiss: () -> Unit) {
    Dialog(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .clip(RoundedCornerShape(16.dp))
                .background(MaterialTheme.colorScheme.surface)
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (url == null) {
                Text(
                    text = "未获取到局域网地址\n请检查电视网络连接",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
                )
            } else {
                val qrBitmap = remember(url) { generateQrBitmap(url) }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color.White)
                        .padding(12.dp)
                ) {
                    Image(
                        bitmap = qrBitmap.asImageBitmap(),
                        contentDescription = "扫码搜索",
                        modifier = Modifier.size(200.dp)
                    )
                }
                Spacer(Modifier.height(16.dp))
                Text(
                    text = "手机扫码搜索",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = "同一局域网内扫码，在手机上输入\n关键字远程搜索，可反复提交",
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    text = url,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun HotTagChip(tag: String, onClick: () -> Unit) {
    var isFocused by remember { mutableStateOf(false) }

    Text(
        text = tag,
        fontSize = 14.sp,
        fontWeight = if (isFocused) FontWeight.Bold else FontWeight.Normal,
        color = if (isFocused) MaterialTheme.colorScheme.onPrimary
        else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f),
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(horizontal = 16.dp, vertical = 8.dp)
    )
}

@Composable
private fun SongResultItem(
    song: Song,
    onClick: () -> Unit
) {
    var isFocused by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(8.dp))
            .background(
                if (isFocused) MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                else MaterialTheme.colorScheme.surface.copy(alpha = 0.3f)
            )
            .then(
                if (isFocused) Modifier.border(
                    1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.5f), RoundedCornerShape(8.dp)
                ) else Modifier
            )
            .onFocusChanged { isFocused = it.isFocused }
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title,
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(2.dp))
            Text(
                text = buildString {
                    song.artist?.let { append(it) }
                    song.album?.let {
                        if (isNotEmpty()) append(" · ")
                        append(it)
                    }
                },
                fontSize = 13.sp,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (song.isVideo) {
            Text(
                text = "MV",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier
                    .clip(RoundedCornerShape(4.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                    .padding(horizontal = 6.dp, vertical = 2.dp)
            )
        }
        Spacer(Modifier.width(8.dp))
        Icon(
            imageVector = Icons.Rounded.PlayArrow,
            contentDescription = "播放",
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
    }
}
